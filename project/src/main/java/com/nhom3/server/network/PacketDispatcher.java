package com.nhom3.server.network;

import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.nhom3.shared.network.packet.Packet;
import com.nhom3.shared.network.packet.PacketType;
import com.nhom3.server.network.liveUpdate.Announcer;

import com.nhom3.server.service.AuthService;
import com.nhom3.server.service.AuctionService;
import com.nhom3.server.service.ItemService;
import com.nhom3.server.dao.AuctionDAO;
import com.nhom3.server.dao.AuctionDAOImpl;
import com.nhom3.server.dao.ItemDAO;
import com.nhom3.server.dao.ItemDAOImpl;
import com.nhom3.server.dao.UserDAO;
import com.nhom3.server.dao.UserDAOImpl;

import com.nhom3.shared.model.user.*;
import com.nhom3.shared.model.item.*;
import com.nhom3.shared.model.auction.*;
import com.nhom3.shared.network.payload.*;

public class PacketDispatcher {
    private static final Logger logger = LoggerFactory.getLogger(PacketDispatcher.class);
    private final Map<PacketType, PacketHandler> handlers = new HashMap<>();
    private final AuthService authService;
    private final AuctionService auctionService;
    private final AuctionHandler auctionHandler;
    private final ClientHandler currentClient;
    private final Announcer announcer = Announcer.getInstance();

    public PacketDispatcher(AuthService authService, AuctionService auctionService, ClientHandler currentClient) {
        auctionHandler = AuctionHandler.getInstance();
        this.authService = authService;
        this.auctionService = auctionService;
        this.currentClient = currentClient;
        registerHandlers();
    }

    private void registerHandlers() {
        handlers.put(PacketType.LOGIN, this::handleLogin);
        handlers.put(PacketType.REGISTER, this::handleRegister);
        handlers.put(PacketType.PLACE_BID, this::handlePlaceBid);
        handlers.put(PacketType.LOAD_BID_HISTORY, this::handleLoadBidHistory);
        handlers.put(PacketType.LOAD_SELLER_ITEMS, this::handleLoadSellerItems);
        handlers.put(PacketType.LOAD_ACTIVE_AUCTIONS, this::handleLoadActiveAuctions);
        handlers.put(PacketType.LOAD_ALL_AUCTIONS, this::handleLoadAllAuctions);
        handlers.put(PacketType.LOAD_AUCTION_BY_ITEM, this::handleLoadAuctionByItem);
        handlers.put(PacketType.LOAD_ITEM_IMAGE, this::handleLoadItemImage);
        handlers.put(PacketType.SAVE_ITEM, this::handleSaveItem);
        handlers.put(PacketType.UPDATE_ITEM, this::handleUpdateItem);
        handlers.put(PacketType.DELETE_ITEM, this::handleDeleteItem);
        handlers.put(PacketType.CONFIRM_PAYMENT, this::handleConfirmPayment);
        handlers.put(PacketType.CANCEL_AUCTION, this::handleCancelAuction);
        handlers.put(PacketType.UPDATE_PROFILE, this::handleUpdateProfile);
        handlers.put(PacketType.CHANGE_PASSWORD, this::handleChangePassword);
        handlers.put(PacketType.LOAD_USERS, this::handleLoadUsers);
        handlers.put(PacketType.PUBLISH_AUCTION, this::handlePublishAuction);
        handlers.put(PacketType.LOAD_PURCHASE_HISTORY, this::handleLoadPurchaseHistory);
        handlers.put(PacketType.PLACE_AUTO_BID, this::handlePlaceAutoBid);
        handlers.put(PacketType.CHECK_AUTO_BID, this::handleCheckAutoBid);
        handlers.put(PacketType.CANCEL_AUTO_BID, this::handleCancelAutoBid);
        handlers.put(PacketType.LOAD_DASHBOARD, this::handleLoadDashboard);
        handlers.put(PacketType.AUCTION_SUBSCRIBE, this::handleAuctionSubscribe);
        handlers.put(PacketType.DELETE_USER, this::handleDeleteUser);
        handlers.put(PacketType.SUBSCRIBE_SYSTEM_LOGS, this::handleSubscribeSystemLogs);
    }

    public Packet dispatch(Packet request, Gson gson) {
        PacketHandler handler = handlers.get(request.getType());
        if (handler != null) {
            try {
                return handler.handle(request, gson);
            } catch (Exception e) {
                logger.error("Lỗi khi xử lý gói tin: " + request.getType(), e);
                return new Packet(request.getType(), new ResultPayload(false, "Lỗi máy chủ", -1, "", "", "", "", ""));
            }
        }
        logger.warn("Không tìm thấy Handler cho loại gói tin: " + request.getType());
        return null;
    }

    private Packet handleLogin(Packet request, Gson gson) {
        LoginPayload login = gson.fromJson(request.getPayload(), LoginPayload.class);
        logger.info("Yêu cầu đăng nhập từ: " + login.getUsername());
        User user = authService.login(login.getUsername(), login.getPassword());

        ResultPayload resultPayload;
        if (user != null) {
            String uEmail = user.getUserContact() != null ? user.getUserContact().getEmail() : "";
            String uPhone = user.getUserContact() != null ? user.getUserContact().getPhoneNumber() : "";
            String profileImage = user.getUserInfo() != null ? user.getUserInfo().getProfileImageBase64() : null;
            resultPayload = new ResultPayload(true, "Đăng nhập thành công", user.getId(),
                    user.getUserInfo().getUserName(), user.getUserInfo().getName(), user.getRole().name(), uEmail,
                    uPhone, profileImage);
        } else {
            resultPayload = new ResultPayload(false, "Sai tài khoản hoặc mật khẩu", -1, "", "", "", "", "");
        }
        return new Packet(PacketType.LOGIN, resultPayload);
    }

    private Packet handleRegister(Packet request, Gson gson) {
        RegisterPayload regData = gson.fromJson(request.getPayload(), RegisterPayload.class);
        logger.info("Yêu cầu ĐĂNG KÝ từ user: " + regData.getUsername());
        UserInfo info = new UserInfo(regData.getUsername(), regData.getPassword(), regData.getFullName());
        UserContact contact = new UserContact(regData.getEmail(), regData.getPhone());
        User newUser = "SELLER".equals(regData.getRole()) ? new Seller(0, info, contact) : new Bidder(0, info, contact);

        boolean isRegSuccess;
        String message;
        try {
            isRegSuccess = authService.register(newUser);
            message = isRegSuccess ? "Đăng ký thành công" : "Tên đăng nhập đã tồn tại hoặc lỗi hệ thống";
        } catch (IllegalArgumentException e) {
            isRegSuccess = false;
            message = e.getMessage();
        }

        ResultPayload regResult = new ResultPayload(isRegSuccess, message, -1, "", "", "", "", "");
        return new Packet(PacketType.REGISTER, regResult);
    }

    private Packet handlePlaceBid(Packet request, Gson gson) {
        BidPayload bidData = gson.fromJson(request.getPayload(), BidPayload.class);
        logger.info("Nhận yêu cầu Đặt giá: " + bidData.getAmount() + " từ User ID: " + bidData.getUserId());
        ResultPayload bidResultPayload = auctionHandler.handleBid(bidData);
        return new Packet(PacketType.PLACE_BID, bidResultPayload);
    }

    private Packet handleLoadBidHistory(Packet request, Gson gson) {
        AuctionIdPayload reqData = gson.fromJson(request.getPayload(), AuctionIdPayload.class);
        AuctionDAO dao = new AuctionDAOImpl();
        List<BidTransaction> dbHistory = dao.getBidHistory(reqData.getAuctionId());

        List<BidHistoryResponsePayload.SimpleBid> simpleList = new ArrayList<>();
        for (BidTransaction b : dbHistory) {
            String bName = b.getBidder() != null ? b.getBidder().getUserInfo().getName() : "Ẩn danh";
            simpleList.add(new BidHistoryResponsePayload.SimpleBid(b.getAmount(), b.getBidTime().toString(),
                    b.getNote(), bName));
        }
        return new Packet(PacketType.LOAD_BID_HISTORY,
                new BidHistoryResponsePayload(reqData.getAuctionId(), simpleList));
    }

    private Packet handleLoadSellerItems(Packet request, Gson gson) {
        SellerIdPayload sellerReq = gson.fromJson(request.getPayload(), SellerIdPayload.class);
        ItemDAO itemDAO = new ItemDAOImpl();
        AuctionDAO auctionDAOForSeller = new AuctionDAOImpl();

        List<Item> itemsFromDb = itemDAO.getItemsBySellerId(sellerReq.getSellerId());
        Map<Integer, String> statusMap = auctionDAOForSeller.getAuctionStatusBySeller(sellerReq.getSellerId());

        List<SellerItemsResponsePayload.SellerItemDTO> dtoList = new ArrayList<>();
        for (Item itm : itemsFromDb) {
            String stt = statusMap.getOrDefault(itm.getId(), "");
            dtoList.add(new SellerItemsResponsePayload.SellerItemDTO(itm.getId(), itm.getName(), itm.getType().name(),
                    itm.getStartPrice(), itm.getCurHighest(), stt));
        }
        return new Packet(PacketType.LOAD_SELLER_ITEMS, new SellerItemsResponsePayload(dtoList));
    }

    private Packet handleLoadActiveAuctions(Packet request, Gson gson) {
        AuctionDAO dao = new AuctionDAOImpl();
        List<Auction> auctions = dao.getActiveAuctions();
        List<AuctionListResponsePayload.AuctionDTO> dtoList = new ArrayList<>();
        for (Auction auction : auctions) {
            dtoList.add(toAuctionDto(auction));
        }
        return new Packet(PacketType.LOAD_ACTIVE_AUCTIONS, new AuctionListResponsePayload(dtoList));
    }

    private Packet handleLoadAllAuctions(Packet request, Gson gson) {
        AuctionDAO dao = new AuctionDAOImpl();
        List<Auction> auctions = dao.getAllAuctions();
        List<AuctionListResponsePayload.AuctionDTO> dtoList = new ArrayList<>();
        for (Auction auction : auctions) {
            dtoList.add(toAuctionDto(auction));
        }
        return new Packet(PacketType.LOAD_ALL_AUCTIONS, new AuctionListResponsePayload(dtoList));
    }

    private Packet handleLoadAuctionByItem(Packet request, Gson gson) {
        ItemActionPayload payload = gson.fromJson(request.getPayload(), ItemActionPayload.class);
        Auction auction = new AuctionDAOImpl().getAuctionByItemId(payload.getItemId());
        List<AuctionListResponsePayload.AuctionDTO> dtoList = new ArrayList<>();
        if (auction != null) {
            dtoList.add(toAuctionDto(auction));
        }
        return new Packet(PacketType.LOAD_AUCTION_BY_ITEM, new AuctionListResponsePayload(dtoList));
    }

    private Packet handleLoadItemImage(Packet request, Gson gson) {
        ItemActionPayload payload = gson.fromJson(request.getPayload(), ItemActionPayload.class);
        String imageBase64 = new ItemDAOImpl().getItemImageBase64(payload.getItemId());
        return new Packet(PacketType.LOAD_ITEM_IMAGE,
                new ItemImagePayload(payload.getItemId(), imageBase64));
    }

    private Packet handleSaveItem(Packet request, Gson gson) {
        ItemPayload payload = gson.fromJson(request.getPayload(), ItemPayload.class);
        try {
            Item item = buildItem(payload);
            Seller seller = new Seller(payload.getSellerId(), null, null);
            boolean success = new ItemService().createItem(seller, item);
            return new Packet(PacketType.SAVE_ITEM, new ResultPayload(success,
                    success ? "Đã thêm sản phẩm mới vào kho!" : "Không thể lưu sản phẩm!",
                    -1, "", "", "", "", ""));
        } catch (Exception e) {
            return new Packet(PacketType.SAVE_ITEM, new ResultPayload(false,
                    e.getMessage(), -1, "", "", "", "", ""));
        }
    }

    private Packet handleUpdateItem(Packet request, Gson gson) {
        ItemPayload payload = gson.fromJson(request.getPayload(), ItemPayload.class);
        try {
            Item item = buildItem(payload);
            boolean success = new ItemDAOImpl().updateItem(item);
            return new Packet(PacketType.UPDATE_ITEM, new ResultPayload(success,
                    success ? "Đã cập nhật thông tin sản phẩm!" : "Không thể cập nhật sản phẩm!",
                    -1, "", "", "", "", ""));
        } catch (Exception e) {
            return new Packet(PacketType.UPDATE_ITEM, new ResultPayload(false,
                    e.getMessage(), -1, "", "", "", "", ""));
        }
    }

    private Packet handleDeleteItem(Packet request, Gson gson) {
        ItemActionPayload payload = gson.fromJson(request.getPayload(), ItemActionPayload.class);
        try {
            boolean success = new ItemService().removeItem(payload.getSellerId(), payload.getItemId());
            return new Packet(PacketType.DELETE_ITEM, new ResultPayload(success,
                    success ? "Đã xóa sản phẩm!" : "Không thể xóa sản phẩm!",
                    -1, "", "", "", "", ""));
        } catch (Exception e) {
            return new Packet(PacketType.DELETE_ITEM, new ResultPayload(false,
                    e.getMessage(), -1, "", "", "", "", ""));
        }
    }

    private Packet handleConfirmPayment(Packet request, Gson gson) {
        ItemActionPayload payload = gson.fromJson(request.getPayload(), ItemActionPayload.class);
        AuctionDAO dao = new AuctionDAOImpl();
        Auction auction = dao.getAuctionByItemId(payload.getItemId());
        boolean success = auction != null && dao.confirmPayment(auction.getId());
        return new Packet(PacketType.CONFIRM_PAYMENT, new ResultPayload(success,
                success ? "Đã xác nhận thanh toán!" : "Không thể xác nhận thanh toán!",
                -1, "", "", "", "", ""));
    }

    private Packet handleCancelAuction(Packet request, Gson gson) {
        AuctionIdPayload payload = gson.fromJson(request.getPayload(), AuctionIdPayload.class);
        try {
            boolean success = auctionService.cancelAuction(payload.getAuctionId());
            return new Packet(PacketType.CANCEL_AUCTION, new ResultPayload(success,
                    success ? "Đã hủy phiên đấu giá!" : "Không thể hủy phiên đấu giá!",
                    -1, "", "", "", "", ""));
        } catch (Exception e) {
            return new Packet(PacketType.CANCEL_AUCTION, new ResultPayload(false,
                    e.getMessage(), -1, "", "", "", "", ""));
        }
    }

    private Packet handleUpdateProfile(Packet request, Gson gson) {
        UserProfilePayload payload = gson.fromJson(request.getPayload(), UserProfilePayload.class);
        User user = buildUser(payload);
        boolean success;
        String message;
        try {
            success = authService.updateUser(user);
            message = success ? "Cập nhật thông tin cá nhân thành công!" : "Không thể cập nhật thông tin!";
        } catch (IllegalArgumentException e) {
            success = false;
            message = e.getMessage();
        }
        return new Packet(PacketType.UPDATE_PROFILE, new ResultPayload(success,
                message,
                payload.getUserId(), payload.getUsername(), payload.getFullName(),
                payload.getRole(), payload.getEmail(), payload.getPhone(), payload.getProfileImageBase64()));
    }

    private Packet handleChangePassword(Packet request, Gson gson) {
        ChangePasswordPayload payload = gson.fromJson(request.getPayload(), ChangePasswordPayload.class);
        boolean success = new UserDAOImpl().changePassword(
                payload.getUserId(), payload.getOldPassword(), payload.getNewPassword());
        return new Packet(PacketType.CHANGE_PASSWORD, new ResultPayload(success,
                success ? "Mật khẩu đã được thay đổi thành công!"
                        : "Mật khẩu hiện tại không chính xác hoặc không thể cập nhật!",
                -1, "", "", "", "", ""));
    }

    private Packet handleLoadUsers(Packet request, Gson gson) {
        UserDAO userDAO = new UserDAOImpl();
        List<UserListResponsePayload.UserDTO> dtoList = new ArrayList<>();
        for (User user : userDAO.getAllUsers()) {
            dtoList.add(toUserDto(user));
        }
        return new Packet(PacketType.LOAD_USERS, new UserListResponsePayload(dtoList));
    }

    private Packet handlePublishAuction(Packet request, Gson gson) {
        PublishAuctionPayload pubData = gson.fromJson(request.getPayload(), PublishAuctionPayload.class);
        boolean isPubSuccess = false;
        String pubMsg = "Lỗi không xác định";
        try {
            LocalDateTime startTime = LocalDateTime.parse(pubData.getStartTime());
            LocalDateTime endTime = LocalDateTime.parse(pubData.getEndTime());
            Item dummyItem = new Art(pubData.getItemId(), "", 0);
            Auction newAuction = new Auction(0, dummyItem, startTime, endTime);
            newAuction.setBidStep(pubData.getBidStep());

            isPubSuccess = auctionService.createAuction(newAuction);
            pubMsg = isPubSuccess ? "Đăng bán thành công!" : "Lỗi lưu Database!";
        } catch (IllegalStateException | IllegalArgumentException e) {
            pubMsg = e.getMessage();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Packet(PacketType.PUBLISH_AUCTION, new ResultPayload(isPubSuccess, pubMsg, -1, "", "", "", "", ""));
    }

    private Packet handleLoadPurchaseHistory(Packet request, Gson gson) {
        BidderIdPayload bidderReq = gson.fromJson(request.getPayload(), BidderIdPayload.class);
        AuctionDAO auctionDAOForHist = new AuctionDAOImpl();
        List<Auction> dbHistoryList = auctionDAOForHist.getMyBidHistory(bidderReq.getBidderId());

        List<PurchaseHistoryResponsePayload.HistoryDTO> purDtoList = new ArrayList<>();
        for (Auction a : dbHistoryList) {
            double amount = a.getBidHistory().isEmpty() ? 0 : a.getBidHistory().get(0).getAmount();
            String timeStr = a.getBidHistory().isEmpty() ? "" : a.getBidHistory().get(0).getBidTime().toString();
            int topBidderId = a.getHighestBidder() != null ? a.getHighestBidder().getId() : -1;
            purDtoList.add(new PurchaseHistoryResponsePayload.HistoryDTO(
                    a.getId(), a.getItem().getId(), a.getItem().getName(), amount, timeStr, a.getStatus().name(),
                    topBidderId,
                    a.getItem().getType().name(), a.getItem().getStartPrice(), a.getItem().getCurHighest(),
                    a.getBidStep(),
                    a.getStartTime().toString(), a.getEndTime().toString(), a.getItem().getImageBase64()));
        }
        return new Packet(PacketType.LOAD_PURCHASE_HISTORY, new PurchaseHistoryResponsePayload(purDtoList));
    }

    private Packet handlePlaceAutoBid(Packet request, Gson gson) {
        AutoBidPayload autoData = gson.fromJson(request.getPayload(), AutoBidPayload.class);
        AuctionDAO adao = new AuctionDAOImpl();

        Auction auction = adao.getAuctionById(autoData.getAuctionId());
        if (auction == null) {
            return new Packet(PacketType.PLACE_AUTO_BID,
                    new ResultPayload(false, "Không tìm thấy phiên đấu giá này!", -1, "", "", "", "", ""));
        }
        if (autoData.getIncrement() < auction.getBidStep()) {
            return new Packet(PacketType.PLACE_AUTO_BID,
                    new ResultPayload(false,
                            "Bước giá Auto-Bid phải lớn hơn hoặc bằng "
                                    + String.format("%,.0f VNĐ", auction.getBidStep()) + "!",
                            -1, "", "", "", "", ""));
        }
        if (autoData.getMaxAmount() <= auction.getItem().getCurHighest()) {
            return new Packet(PacketType.PLACE_AUTO_BID,
                    new ResultPayload(false, "Giá tối đa phải lớn hơn giá hiện tại!", -1, "", "", "", "", ""));
        }

        boolean autoSuccess = adao.saveAutoBidConfig(autoData);
        if (autoSuccess) {
            auctionHandler.handleAutoBid(autoData.getAuctionId());
        }
        String message = autoSuccess ? "Thiết lập Auto-Bid thành công!" : "Lỗi hệ thống khi thiết lập Auto-Bid!";
        return new Packet(PacketType.PLACE_AUTO_BID, new ResultPayload(autoSuccess, message, -1, "", "", "", "", ""));
    }

    private Packet handleCheckAutoBid(Packet request, Gson gson) {
        AutoBidPayload checkReq = gson.fromJson(request.getPayload(), AutoBidPayload.class);
        AuctionDAOImpl dao = new AuctionDAOImpl();
        AutoBidPayload existingConfig = dao.getUserAutoBid(checkReq.getAuctionId(), checkReq.getUserId());
        Auction auction = dao.getAuctionById(checkReq.getAuctionId());
        if (existingConfig != null && auction != null && existingConfig.getIncrement() < auction.getBidStep()) {
            existingConfig = null;
        }
        return new Packet(PacketType.CHECK_AUTO_BID, existingConfig);
    }

    private Packet handleCancelAutoBid(Packet request, Gson gson) {
        AutoBidPayload cancelData = gson.fromJson(request.getPayload(), AutoBidPayload.class);
        boolean cancelSuccess = new AuctionDAOImpl().cancelAutoBid(cancelData.getAuctionId(), cancelData.getUserId());
        return new Packet(PacketType.CANCEL_AUTO_BID,
                new ResultPayload(cancelSuccess,
                        cancelSuccess ? "Đã tắt hệ thống Đấu giá tự động!" : "Lỗi hệ thống khi tắt Auto-Bid!", -1, "",
                        "", "", "", ""));
    }

    private Packet handleLoadDashboard(Packet request, Gson gson) {
        AuctionDAO dashDao = new AuctionDAOImpl();
        DashboardResponsePayload dashRes = dashDao.getDashboardStats();
        return new Packet(PacketType.LOAD_DASHBOARD, dashRes);
    }

    private Packet handleAuctionSubscribe(Packet request, Gson gson) {
        AuctionSubscribePayload subscribePayload = gson.fromJson(request.getPayload(), AuctionSubscribePayload.class);
        Packet k = new Packet(PacketType.AUCTION_SUBSCRIBE, new ResultPayload(true, "", -1, "", "", "", "", ""));
        if (subscribePayload.isSub()) {
            announcer.addObserver(subscribePayload.getAuctionId(), currentClient);
        } else {
            announcer.removeObserver(subscribePayload.getAuctionId(), currentClient);
        }
        return k;
    }

    private Item buildItem(ItemPayload payload) {
        ItemType type = ItemType.valueOf(payload.getType());
        Item item = type.createItem(payload.getItemId(), payload.getName(), payload.getStartPrice());
        item.setCurHighest(payload.getStartPrice());
        item.setImageBase64(payload.getImageBase64());
        return item;
    }

    private User buildUser(UserProfilePayload payload) {
        UserInfo info = new UserInfo(payload.getUsername(), "", payload.getFullName());
        info.setProfileImageBase64(payload.getProfileImageBase64());
        UserContact contact = new UserContact(payload.getEmail(), payload.getPhone());
        if ("SELLER".equals(payload.getRole())) {
            return new Seller(payload.getUserId(), info, contact);
        } else if ("ADMIN".equals(payload.getRole())) {
            return new Admin(payload.getUserId(), info, contact);
        }
        return new Bidder(payload.getUserId(), info, contact);
    }

    private AuctionListResponsePayload.AuctionDTO toAuctionDto(Auction auction) {
        Item item = auction.getItem();
        return new AuctionListResponsePayload.AuctionDTO(
                auction.getId(),
                item != null ? item.getId() : -1,
                item != null ? item.getName() : "",
                item != null ? item.getType().name() : "",
                item != null ? item.getStartPrice() : 0,
                item != null ? item.getCurHighest() : 0,
                auction.getBidStep(),
                auction.getStartTime() != null ? auction.getStartTime().toString() : "",
                auction.getEndTime() != null ? auction.getEndTime().toString() : "",
                auction.getStatus() != null ? auction.getStatus().name() : "",
                auction.getHighestBidderId(),
                item != null ? item.getImageBase64() : null,
                item != null && item.getSellerName() != null ? item.getSellerName() : "");
    }

    private UserListResponsePayload.UserDTO toUserDto(User user) {
        return new UserListResponsePayload.UserDTO(
                user.getId(),
                user.getUserInfo() != null ? user.getUserInfo().getUserName() : "",
                user.getUserInfo() != null ? user.getUserInfo().getName() : "",
                user.getRole() != null ? user.getRole().name() : "",
                user.getUserContact() != null ? user.getUserContact().getEmail() : "",
                user.getUserContact() != null ? user.getUserContact().getPhoneNumber() : "");
    }

    private Packet handleDeleteUser(Packet request, Gson gson) {
        UserIdPayload payload = gson.fromJson(request.getPayload(), UserIdPayload.class);
        UserDAO userDAO = new UserDAOImpl();
        boolean success = userDAO.deleteUser(payload.getUserId());
        
        return new Packet(PacketType.DELETE_USER, new ResultPayload(success,
                success ? "Đã xóa tài khoản thành công!" : "Lỗi: Không thể xóa tài khoản!",
                -1, "", "", "", "", ""));
    }

    private Packet handleSubscribeSystemLogs(Packet request, Gson gson) {
        SystemLogSubscribePayload payload = gson.fromJson(request.getPayload(), SystemLogSubscribePayload.class);
        if (payload != null && payload.isSub()) {
            com.nhom3.server.network.liveUpdate.SystemLogAnnouncer.getInstance().addObserver(currentClient);
        } else {
            com.nhom3.server.network.liveUpdate.SystemLogAnnouncer.getInstance().removeObserver(currentClient);
        }
        return new Packet(PacketType.SUBSCRIBE_SYSTEM_LOGS, new ResultPayload(true, "", -1, "", "", "", "", ""));
    }
}
