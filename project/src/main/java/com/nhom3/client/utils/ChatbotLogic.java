package com.nhom3.client.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nhom3.shared.model.auction.Auction;
import com.nhom3.shared.model.item.Item;
import com.nhom3.shared.model.user.User;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public class ChatbotLogic {
    private static final String RULES_RESOURCE_PATH = "/com/nhom3/client/data/chatbot_rules.json";
    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{M}+");
    private static final Pattern TOKEN_SPLIT_PATTERN = Pattern.compile("[^a-z0-9-]+");
    private static final int MAX_LEVENSHTEIN_DISTANCE = 1;
    private static final String FALLBACK_RESPONSE =
            "Xin lỗi, mình chưa hiểu ý bạn. Vui lòng bấm vào các gợi ý bên dưới hoặc hỏi lại rõ hơn.";

    private final List<IntentRule> intentRules;

    public ChatbotLogic() {
        this.intentRules = loadRules();
    }

    public static final class ChatResult {
        private final String message;
        private final String actionCode;
        private final List<String> suggestions;

        public ChatResult(String message, String actionCode) {
            this(message, actionCode, Collections.emptyList());
        }

        public ChatResult(String message, String actionCode, List<String> suggestions) {
            this.message = message;
            this.actionCode = actionCode;
            this.suggestions = suggestions != null ? List.copyOf(suggestions) : Collections.emptyList();
        }

        public String getMessage() {
            return message;
        }

        public String getActionCode() {
            return actionCode;
        }

        public List<String> getSuggestions() {
            return suggestions;
        }
    }

    public String removeAccents(String text) {
        if (text == null) {
            return "";
        }

        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        String withoutDiacritics = DIACRITICS_PATTERN.matcher(normalized).replaceAll("");
        return withoutDiacritics
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    public ChatResult getResponse(String rawInput) {
        return getResponse(rawInput, null);
    }

    public ChatResult getResponse(String rawInput, String contextType) {
        String input = removeAccents(rawInput);
        List<String> inputTokens = tokenize(input);
        String currentUserRole = getCurrentUserRole();
        for (IntentRule rule : intentRules) {
            if (!isIntentAllowedForRole(rule, currentUserRole)) {
                continue;
            }
            if (matchesExactPhrase(input, inputTokens, rule)) {
                return buildResult(rule, contextType);
            }
        }

        for (IntentRule rule : intentRules) {
            if (!isIntentAllowedForRole(rule, currentUserRole)) {
                continue;
            }
            if (matchesSingleWordFuzzy(inputTokens, rule)) {
                return buildResult(rule, contextType);
            }
        }

        return new ChatResult(
                FALLBACK_RESPONSE,
                null,
                smartSuggestionsForContext(suggestionsForRole(currentUserRole), currentUserRole, contextType));
    }

    public ChatResult getContextualGreeting(String contextType, Object contextData) {
        String role = getCurrentUserRole();
        return switch (contextType == null ? "DEFAULT" : contextType) {
            case "VIEW_ITEM" -> applyContext(buildViewItemGreeting(contextData, role), contextType);
            case "DASHBOARD" -> applyContext(new ChatResult(
                    "Chào mừng đến với Tổng quan AuctionPro. Mình có thể đưa bạn đến các khu vực phù hợp với vai trò hiện tại.",
                    null,
                    dashboardSuggestionsForRole(role)), contextType);
            case "MARKET" -> applyContext(new ChatResult(
                    isSeller(role)
                            ? "Bạn đang xem Chợ đấu giá ở góc nhìn Người bán. Mình có thể hỗ trợ cách đăng bán hoặc quản lý sản phẩm."
                            : "Bạn đang ở Chợ đấu giá. Chúc bạn săn được món hời nhé! Mình có thể hỗ trợ cách đặt giá hoặc Auto-Bid.",
                    null,
                    suggestionsForRole(role)), contextType);
            case "MANAGE_ITEM" -> applyContext(new ChatResult(
                    "Bạn đang ở trang Quản lý sản phẩm. Mình có thể hướng dẫn đăng bán, tạo phiên đấu giá hoặc xác nhận tiền.",
                    null,
                    sellerSuggestions()), contextType);
            case "PURCHASE_HISTORY" -> applyContext(new ChatResult(
                    "Bạn đang ở Lịch sử đấu giá. Mình có thể giải thích thanh toán, điểm uy tín hoặc cách đánh giá người bán.",
                    null,
                    bidderSuggestions()), contextType);
            case "PROFILE" -> applyContext(new ChatResult(
                    "Bạn đang ở Hồ sơ cá nhân. Mình có thể hỗ trợ đổi mật khẩu, cập nhật thông tin hoặc quay về các trang chính.",
                    null,
                    profileSuggestionsForRole(role)), contextType);
            case "ADMIN_PANEL" -> applyContext(new ChatResult(
                    "Bạn đang ở trang Quản trị. Mình có thể hỗ trợ điều hướng hoặc giải thích quy trình chung.",
                    null,
                    Arrays.asList("Hướng dẫn sử dụng", "Tổng quan", "Mở hồ sơ")), contextType);
            default -> getResponse("chao", contextType);
        };
    }

    private ChatResult buildViewItemGreeting(Object contextData, String role) {
        Item item = extractContextItem(contextData);
        String itemName = item != null && item.getName() != null && !item.getName().isBlank()
                ? item.getName()
                : "sản phẩm này";
        String currentPrice = item != null
                ? String.format("%,.0f VNĐ", item.getCurHighest())
                : "chưa có thông tin";
        String message = "Mình thấy bạn vừa xem sản phẩm " + itemName
                + " (Giá hiện tại: " + currentPrice
                + "). Bạn có cần hỗ trợ gì về sản phẩm này không?";
        return new ChatResult(message, null, isSeller(role) ? sellerSuggestions() : bidderSuggestions());
    }

    private String contextHelpPrompt(String role) {
        if (isSeller(role)) {
            return "Bạn muốn hỏi về cách đăng bán, quản lý sản phẩm hay xác nhận tiền?";
        }
        if (isBidder(role)) {
            return "Bạn muốn hỏi về cách đấu giá, Auto-Bid, thanh toán hay điểm uy tín?";
        }
        return "Bạn muốn hỏi về hướng dẫn sử dụng hay điều hướng?";
    }

    private List<String> suggestionsForRole(String role) {
        if (isSeller(role)) {
            return sellerSuggestions();
        }
        if (isBidder(role)) {
            return bidderSuggestions();
        }
        return Arrays.asList("Trang quản trị", "Tổng quan");
    }

    private List<String> dashboardSuggestionsForRole(String role) {
        if (isSeller(role)) {
            return Arrays.asList("Quản lý sản phẩm", "Cách đăng bán", "Mở hồ sơ");
        }
        if (isBidder(role)) {
            return Arrays.asList("Mở chợ đấu giá", "Lịch sử đấu giá", "Mở hồ sơ");
        }
        return Arrays.asList("Trang quản trị", "Tổng quan");
    }

    private List<String> profileSuggestionsForRole(String role) {
        if (isSeller(role)) {
            return Arrays.asList("Đổi mật khẩu", "Cập nhật thông tin", "Quản lý sản phẩm");
        }
        if (isBidder(role)) {
            return Arrays.asList("Đổi mật khẩu", "Cập nhật thông tin", "Lịch sử đấu giá");
        }
        return Arrays.asList("Đổi mật khẩu", "Cập nhật thông tin", "Trang quản trị");
    }

    private List<String> bidderSuggestions() {
        return Arrays.asList("Cách đấu giá", "Điểm uy tín", "Thanh toán");
    }

    private List<String> sellerSuggestions() {
        return Arrays.asList("Cách đăng bán", "Quản lý sản phẩm", "Xác nhận tiền");
    }

    private boolean isBidder(String role) {
        return "BIDDER".equals(role);
    }

    private boolean isSeller(String role) {
        return "SELLER".equals(role);
    }

    private Item extractContextItem(Object contextData) {
        if (contextData instanceof Auction auction) {
            return auction.getItem();
        }
        if (contextData instanceof Item item) {
            return item;
        }
        return null;
    }

    private String getCurrentUserRole() {
        User user = UserSession.getInstance().getLoggedInUser();
        if (user == null || user.getRole() == null) {
            return "";
        }
        return user.getRole().name();
    }

    private boolean isIntentAllowedForRole(IntentRule rule, String currentUserRole) {
        if (rule == null || rule.targetRoles == null || rule.targetRoles.isEmpty()) {
            return true;
        }
        if (currentUserRole == null || currentUserRole.isBlank()) {
            return false;
        }

        for (String targetRole : rule.targetRoles) {
            if (currentUserRole.equalsIgnoreCase(targetRole)) {
                return true;
            }
        }
        return false;
    }

    private ChatResult buildResult(IntentRule rule) {
        return buildResult(rule, null);
    }

    private ChatResult buildResult(IntentRule rule, String contextType) {
        String role = getCurrentUserRole();
        String action = isActionTargetCurrentContext(rule.action, contextType) ? null : rule.action;
        String response = overrideResponseForContext(rule, contextType, action == null && rule.action != null);
        List<String> suggestions = suggestionsForResult(
                rule,
                role,
                contextType,
                action == null && rule.action != null);
        return new ChatResult(injectUserData(response), action, suggestions);
    }

    private ChatResult applyContext(ChatResult result, String contextType) {
        if (result == null) {
            return new ChatResult("", null);
        }
        String action = isActionTargetCurrentContext(result.getActionCode(), contextType)
                ? null
                : result.getActionCode();
        return new ChatResult(
                result.getMessage(),
                action,
                smartSuggestionsForContext(result.getSuggestions(), getCurrentUserRole(), contextType));
    }

    private String overrideResponseForContext(IntentRule rule, String contextType, boolean actionSuppressed) {
        String context = normalizeContext(contextType);
        if (rule == null) {
            return "";
        }

        if ("PROFILE".equals(context) && "common_profile_help".equals(rule.id)) {
            return "Bạn đang ở Hồ sơ cá nhân rồi. Để đổi mật khẩu, bấm nút 'Đổi Mật Khẩu', "
                    + "nhập mật khẩu hiện tại, mật khẩu mới và xác nhận mật khẩu mới. "
                    + "Để cập nhật hồ sơ, bấm chỉnh sửa thông tin, thay đổi họ tên, email, số điện thoại hoặc ảnh đại diện rồi lưu lại.";
        }

        if (actionSuppressed) {
            return switch (context) {
                case "DASHBOARD" -> "Bạn đang ở trang Tổng quan rồi.";
                case "MARKET" -> "Bạn đang ở Chợ đấu giá rồi.";
                case "PURCHASE_HISTORY" -> "Bạn đang ở Lịch sử mua rồi.";
                case "MANAGE_ITEM" -> "Bạn đang ở trang Quản lý sản phẩm rồi.";
                case "PROFILE" -> "Bạn đang ở trang Hồ sơ cá nhân rồi.";
                case "ADMIN_PANEL" -> "Bạn đang ở trang Quản trị rồi.";
                default -> rule.response;
            };
        }

        return rule.response;
    }

    private List<String> suggestionsForResult(
            IntentRule rule,
            String role,
            String contextType,
            boolean actionSuppressed) {
        List<String> contextualSuggestions = contextualSuggestionsForRule(rule, role, contextType, actionSuppressed);
        if (!contextualSuggestions.isEmpty()) {
            return smartSuggestionsForContext(contextualSuggestions, role, contextType);
        }

        return smartSuggestionsForContext(rule != null ? rule.suggestions : Collections.emptyList(), role, contextType);
    }

    private List<String> contextualSuggestionsForRule(
            IntentRule rule,
            String role,
            String contextType,
            boolean actionSuppressed) {
        String context = normalizeContext(contextType);
        if (rule == null || context.isBlank()) {
            return Collections.emptyList();
        }

        if ("PROFILE".equals(context) && "common_profile_help".equals(rule.id)) {
            if (isBidder(role)) {
                return Arrays.asList("Cập nhật thông tin", "Lịch sử mua", "Tổng quan");
            }
            if (isSeller(role)) {
                return Arrays.asList("Cập nhật thông tin", "Quản lý sản phẩm", "Tổng quan");
            }
            return Arrays.asList("Cập nhật thông tin", "Trang quản trị", "Tổng quan");
        }

        if (actionSuppressed) {
            return contextSuggestionsForRole(context, role);
        }

        return Collections.emptyList();
    }

    private List<String> smartSuggestionsForContext(List<String> suggestions, String role, String contextType) {
        List<String> filtered = filterSuggestionsForContext(suggestions, contextType);
        if (!filtered.isEmpty()) {
            return filtered;
        }

        String context = normalizeContext(contextType);
        if (context.isBlank()) {
            return suggestionsForRole(role);
        }

        return filterSuggestionsForContext(contextSuggestionsForRole(context, role), context);
    }

    private List<String> contextSuggestionsForRole(String context, String role) {
        return switch (normalizeContext(context)) {
            case "DASHBOARD" -> dashboardContextSuggestions(role);
            case "MARKET" -> marketContextSuggestions(role);
            case "MANAGE_ITEM" -> manageItemContextSuggestions(role);
            case "PURCHASE_HISTORY" -> purchaseHistoryContextSuggestions(role);
            case "PROFILE" -> profileContextSuggestions(role);
            case "ADMIN_PANEL" -> adminPanelContextSuggestions(role);
            case "VIEW_ITEM" -> viewItemContextSuggestions(role);
            default -> suggestionsForRole(role);
        };
    }

    private List<String> dashboardContextSuggestions(String role) {
        if (isBidder(role)) {
            return Arrays.asList("Cách đấu giá", "Điểm uy tín", "Mở chợ đấu giá");
        }
        if (isSeller(role)) {
            return Arrays.asList("Cách đăng bán", "Quản lý sản phẩm", "Mở quản lý SP");
        }
        return Arrays.asList("Trang quản trị", "Mở hồ sơ");
    }

    private List<String> marketContextSuggestions(String role) {
        if (isSeller(role)) {
            return Arrays.asList("Cách đăng bán", "Quản lý sản phẩm", "Tổng quan");
        }
        return Arrays.asList("Cách đặt giá", "Cài Auto-Bid", "Điểm uy tín");
    }

    private List<String> manageItemContextSuggestions(String role) {
        if (isSeller(role)) {
            return Arrays.asList("Cách đăng bán", "Xác nhận tiền", "Tổng quan");
        }
        return suggestionsForRole(role);
    }

    private List<String> purchaseHistoryContextSuggestions(String role) {
        if (isBidder(role)) {
            return Arrays.asList("Hướng dẫn thanh toán", "Đánh giá Seller", "Điểm uy tín");
        }
        return suggestionsForRole(role);
    }

    private List<String> profileContextSuggestions(String role) {
        if (isBidder(role)) {
            return Arrays.asList("Cập nhật thông tin", "Lịch sử mua", "Tổng quan");
        }
        if (isSeller(role)) {
            return Arrays.asList("Cập nhật thông tin", "Quản lý sản phẩm", "Tổng quan");
        }
        return Arrays.asList("Cập nhật thông tin", "Trang quản trị", "Tổng quan");
    }

    private List<String> adminPanelContextSuggestions(String role) {
        if ("ADMIN".equals(role)) {
            return Arrays.asList("Trang quản trị", "Tổng quan", "Mở hồ sơ");
        }
        return suggestionsForRole(role);
    }

    private List<String> viewItemContextSuggestions(String role) {
        if (isSeller(role)) {
            return Arrays.asList("Quản lý sản phẩm", "Xác nhận tiền", "Tổng quan");
        }
        return Arrays.asList("Cách đặt giá", "Cài Auto-Bid", "Thanh toán");
    }

    private List<String> filterSuggestionsForContext(List<String> suggestions, String contextType) {
        if (suggestions == null || suggestions.isEmpty()) {
            return Collections.emptyList();
        }

        String context = normalizeContext(contextType);
        if (context.isBlank()) {
            return suggestions;
        }

        List<String> filtered = new ArrayList<>();
        for (String suggestion : suggestions) {
            if (suggestion == null || isSuggestionTargetCurrentContext(suggestion, context)) {
                continue;
            }
            filtered.add(suggestion);
        }
        return filtered;
    }

    private boolean isSuggestionTargetCurrentContext(String suggestion, String context) {
        String targetContext = targetContextForSuggestion(suggestion);
        return !targetContext.isBlank() && targetContext.equals(context);
    }

    private boolean isActionTargetCurrentContext(String actionCode, String contextType) {
        String targetContext = targetContextForAction(actionCode);
        return !targetContext.isBlank() && targetContext.equals(normalizeContext(contextType));
    }

    private String targetContextForAction(String actionCode) {
        if (actionCode == null || actionCode.isBlank()) {
            return "";
        }

        return switch (actionCode) {
            case "NAV_MARKET" -> "MARKET";
            case "NAV_DASHBOARD" -> "DASHBOARD";
            case "NAV_PROFILE" -> "PROFILE";
            case "NAV_PURCHASE_HISTORY" -> "PURCHASE_HISTORY";
            case "NAV_MANAGE_ITEM" -> "MANAGE_ITEM";
            default -> "";
        };
    }

    private String targetContextForSuggestion(String suggestion) {
        String normalized = removeAccents(suggestion);
        return switch (normalized) {
            case "mo ho so", "di den ho so", "vao ho so", "mo trang ho so" -> "PROFILE";
            case "mo cho dau gia", "vao cho dau gia", "xem san pham dau gia", "vao market" -> "MARKET";
            case "mo lich su mua", "mo lich su dau gia", "xem lich su mua", "di den lich su mua" ->
                    "PURCHASE_HISTORY";
            case "mo quan ly sp", "mo quan ly san pham", "di den quan ly san pham", "vao quan ly san pham" ->
                    "MANAGE_ITEM";
            case "tong quan", "dashboard", "ve tong quan", "trang chu" -> "DASHBOARD";
            default -> "";
        };
    }

    private String normalizeContext(String contextType) {
        return contextType == null ? "" : contextType.trim().toUpperCase(Locale.ROOT);
    }

    private String injectUserData(String response) {
        if (response == null) {
            return "";
        }

        User user = UserSession.getInstance().getLoggedInUser();
        String name = "bạn";
        String role = "GUEST";
        String reputation = "0";

        if (user != null) {
            if (user.getUserInfo() != null && user.getUserInfo().getName() != null
                    && !user.getUserInfo().getName().isBlank()) {
                name = user.getUserInfo().getName();
            }
            if (user.getRole() != null) {
                role = user.getRole().name();
            }
            reputation = String.valueOf(user.getReputationScore());
        }

        return response
                .replace("{name}", name)
                .replace("{role}", role)
                .replace("{reputation}", reputation);
    }

    private List<IntentRule> loadRules() {
        try (InputStream stream = ChatbotLogic.class.getResourceAsStream(RULES_RESOURCE_PATH)) {
            if (stream == null) {
                return Collections.emptyList();
            }

            Type listType = new TypeToken<List<IntentRule>>() {
            }.getType();
            List<IntentRule> rules = new Gson().fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8), listType);
            return rules != null ? rules : Collections.emptyList();
        } catch (RuntimeException | java.io.IOException e) {
            return Collections.emptyList();
        }
    }

    private boolean matchesExactPhrase(String normalizedInput, List<String> inputTokens, IntentRule rule) {
        if (rule == null || rule.keywords == null || rule.response == null) {
            return false;
        }

        for (String keyword : rule.keywords) {
            String normalizedKeyword = removeAccents(keyword);
            if (normalizedKeyword.isBlank()) {
                continue;
            }
            if ((normalizedKeyword.contains(" ") || normalizedKeyword.contains("-"))
                    && normalizedInput.contains(normalizedKeyword)) {
                return true;
            }
            if (!normalizedKeyword.contains(" ") && !normalizedKeyword.contains("-")
                    && inputTokens.contains(normalizedKeyword)) {
                return true;
            }
        }

        return false;
    }

    private boolean matchesSingleWordFuzzy(List<String> inputTokens, IntentRule rule) {
        if (rule == null || rule.keywords == null || rule.response == null || inputTokens.isEmpty()) {
            return false;
        }

        for (String keyword : rule.keywords) {
            String normalizedKeyword = removeAccents(keyword);
            if (normalizedKeyword.isBlank() || normalizedKeyword.contains(" ")) {
                continue;
            }

            for (String inputToken : inputTokens) {
                if (levenshteinDistance(inputToken, normalizedKeyword) <= MAX_LEVENSHTEIN_DISTANCE) {
                    return true;
                }
            }
        }

        return false;
    }

    private List<String> tokenize(String normalizedText) {
        if (normalizedText == null || normalizedText.isBlank()) {
            return Collections.emptyList();
        }

        String[] rawTokens = TOKEN_SPLIT_PATTERN.split(normalizedText);
        List<String> tokens = new ArrayList<>();
        for (String token : rawTokens) {
            if (!token.isBlank()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    public int levenshteinDistance(String first, String second) {
        if (first == null) {
            first = "";
        }
        if (second == null) {
            second = "";
        }

        int[] previous = new int[second.length() + 1];
        int[] current = new int[second.length() + 1];

        for (int j = 0; j <= second.length(); j++) {
            previous[j] = j;
        }

        for (int i = 1; i <= first.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= second.length(); j++) {
                int substitutionCost = first.charAt(i - 1) == second.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(
                        Math.min(current[j - 1] + 1, previous[j] + 1),
                        previous[j - 1] + substitutionCost);
            }

            int[] temp = previous;
            previous = current;
            current = temp;
        }

        return previous[second.length()];
    }

    private static final class IntentRule {
        private String id;
        private List<String> keywords;
        private String response;
        private String action;
        private List<String> suggestions;
        private List<String> targetRoles;
    }
}
