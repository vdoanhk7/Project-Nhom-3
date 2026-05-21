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
            "Xin lỗi, mình chưa hiểu ý bạn. Bạn có thể hỏi về hướng dẫn sử dụng, điều hướng, hoặc chức năng phù hợp với vai trò hiện tại.";

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
        String input = removeAccents(rawInput);
        List<String> inputTokens = tokenize(input);
        String currentUserRole = getCurrentUserRole();
        for (IntentRule rule : intentRules) {
            if (!isIntentAllowedForRole(rule, currentUserRole)) {
                continue;
            }
            if (matchesExactPhrase(input, inputTokens, rule)) {
                return buildResult(rule);
            }
        }

        for (IntentRule rule : intentRules) {
            if (!isIntentAllowedForRole(rule, currentUserRole)) {
                continue;
            }
            if (matchesSingleWordFuzzy(inputTokens, rule)) {
                return buildResult(rule);
            }
        }

        return new ChatResult(FALLBACK_RESPONSE, null);
    }

    public ChatResult getContextualGreeting(String contextType, Object contextData) {
        String role = getCurrentUserRole();
        return switch (contextType == null ? "DEFAULT" : contextType) {
            case "VIEW_ITEM" -> buildViewItemGreeting(contextData, role);
            case "DASHBOARD" -> new ChatResult(
                    "Chào mừng đến với Tổng quan AuctionPro. Mình có thể đưa bạn đến các khu vực phù hợp với vai trò hiện tại.",
                    null,
                    dashboardSuggestionsForRole(role));
            case "MARKET" -> new ChatResult(
                    isSeller(role)
                            ? "Bạn đang xem Chợ đấu giá ở góc nhìn Người bán. Mình có thể hỗ trợ cách đăng bán hoặc quản lý sản phẩm."
                            : "Bạn đang ở Chợ đấu giá. Chúc bạn săn được món hời nhé! Mình có thể hỗ trợ cách đặt giá hoặc Auto-Bid.",
                    null,
                    suggestionsForRole(role));
            case "MANAGE_ITEM" -> new ChatResult(
                    "Bạn đang ở trang Quản lý sản phẩm. Mình có thể hướng dẫn đăng bán, tạo phiên đấu giá hoặc xác nhận tiền.",
                    null,
                    sellerSuggestions());
            case "PURCHASE_HISTORY" -> new ChatResult(
                    "Bạn đang ở Lịch sử đấu giá. Mình có thể giải thích thanh toán, điểm uy tín hoặc cách đánh giá người bán.",
                    null,
                    bidderSuggestions());
            case "PROFILE" -> new ChatResult(
                    "Bạn đang ở Hồ sơ cá nhân. Mình có thể hỗ trợ đổi mật khẩu, cập nhật thông tin hoặc quay về các trang chính.",
                    null,
                    profileSuggestionsForRole(role));
            case "ADMIN_PANEL" -> new ChatResult(
                    "Bạn đang ở trang Quản trị. Mình có thể hỗ trợ điều hướng hoặc giải thích quy trình chung.",
                    null,
                    Arrays.asList("Hướng dẫn sử dụng", "Tổng quan", "Mở hồ sơ"));
            default -> getResponse("chao");
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
        return Arrays.asList("Hướng dẫn sử dụng", "Tổng quan", "Mở hồ sơ");
    }

    private List<String> dashboardSuggestionsForRole(String role) {
        if (isSeller(role)) {
            return Arrays.asList("Quản lý sản phẩm", "Cách đăng bán", "Mở hồ sơ");
        }
        if (isBidder(role)) {
            return Arrays.asList("Mở chợ đấu giá", "Lịch sử đấu giá", "Mở hồ sơ");
        }
        return Arrays.asList("Trang quản trị", "Tổng quan", "Mở hồ sơ");
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
        return Arrays.asList("Cách đặt giá", "Cài Auto-Bid", "Thanh toán", "Điểm uy tín");
    }

    private List<String> sellerSuggestions() {
        return Arrays.asList("Cách đăng bán", "Quản lý sản phẩm", "Xác nhận tiền", "Tạo phiên đấu giá");
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
        return new ChatResult(injectUserData(rule.response), rule.action, rule.suggestions);
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
