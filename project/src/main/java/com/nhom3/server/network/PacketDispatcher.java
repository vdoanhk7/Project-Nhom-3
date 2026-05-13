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
import com.nhom3.server.dao.AuctionDAO;
import com.nhom3.server.dao.AuctionDAOImpl;
import com.nhom3.server.dao.ItemDAO;
import com.nhom3.server.dao.ItemDAOImpl;

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
    private final Announcer announcer = Announcer.getInstance();

    public PacketDispatcher(AuthService authService, AuctionService auctionService) {
        auctionHandler = AuctionHandler.getInstance();
        this.authService = authService;
        this.auctionService = auctionService;
        registerHandlers();
    }

    private void registerHandlers() {
        handlers.put(PacketType.LOGIN, this::handleLogin);
        handlers.put(PacketType.REGISTER, this::handleRegister);
        handlers.put(PacketType.PLACE_BID, this::handlePlaceBid);
        handlers.put(PacketType.LOAD_BID_HISTORY, this::handleLoadBidHistory);
        handlers.put(PacketType.LOAD_SELLER_ITEMS, this::handleLoadSellerItems);
        handlers.put(PacketType.PUBLISH_AUCTION, this::handlePublishAuction);
        handlers.put(PacketType.LOAD_PURCHASE_HISTORY, this::handleLoadPurchaseHistory);
        handlers.put(PacketType.PLACE_AUTO_BID, this::handlePlaceAutoBid);
        handlers.put(PacketType.CHECK_AUTO_BID, this::handleCheckAutoBid);
        handlers.put(PacketType.CANCEL_AUTO_BID, this::handleCancelAutoBid);
        handlers.put(PacketType.LOAD_DASHBOARD, this::handleLoadDashboard);
        handlers.put(PacketType.AUCTION_SUBSCRIBE, this::handleAuctionSubscribe);
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
            resultPayload = new ResultPayload(true, "Đăng nhập thành công", user.getId(), user.getUserInfo().getUserName(), user.getUserInfo().getName(), user.getRole().name(), uEmail, uPhone);
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
        
        boolean isRegSuccess = authService.register(newUser);
        ResultPayload regResult = new ResultPayload(isRegSuccess, isRegSuccess ? "Đăng ký thành công" : "Tên đăng nhập đã tồn tại hoặc lỗi hệ thống", -1, "", "", "", "", "");
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
            simpleList.add(new BidHistoryResponsePayload.SimpleBid(b.getAmount(), b.getBidTime().toString(), b.getNote(), bName));
        }
        return new Packet(PacketType.LOAD_BID_HISTORY, new BidHistoryResponsePayload(reqData.getAuctionId(), simpleList));
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
            dtoList.add(new SellerItemsResponsePayload.SellerItemDTO(itm.getId(), itm.getName(), itm.getType().name(), itm.getStartPrice(), itm.getCurHighest(), stt));
        }
        return new Packet(PacketType.LOAD_SELLER_ITEMS, new SellerItemsResponsePayload(dtoList));
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
                a.getId(), a.getItem().getId(), a.getItem().getName(), amount, timeStr, a.getStatus().name(), topBidderId,
                a.getItem().getType().name(), a.getItem().getStartPrice(), a.getItem().getCurHighest(),
                a.getStartTime().toString(), a.getEndTime().toString()
            ));
        }
        return new Packet(PacketType.LOAD_PURCHASE_HISTORY, new PurchaseHistoryResponsePayload(purDtoList));
    }

    private Packet handlePlaceAutoBid(Packet request, Gson gson) {
        AutoBidPayload autoData = gson.fromJson(request.getPayload(), AutoBidPayload.class);
        AuctionDAO adao = new AuctionDAOImpl();
        boolean autoSuccess = adao.saveAutoBidConfig(autoData);
        if (autoSuccess) {
            auctionService.triggerAutoBids(autoData.getAuctionId());
        }
        return new Packet(PacketType.PLACE_AUTO_BID, new ResultPayload(autoSuccess, autoSuccess ? "Hệ thống đã ghi nhận thiết lập Auto-Bid của bạn!" : "Lỗi Database khi cài đặt Auto-Bid!", -1, "", "", "", "", ""));
    }

    private Packet handleCheckAutoBid(Packet request, Gson gson) {
        AutoBidPayload checkReq = gson.fromJson(request.getPayload(), AutoBidPayload.class);
        AutoBidPayload existingConfig = new AuctionDAOImpl().getUserAutoBid(checkReq.getAuctionId(), checkReq.getUserId());
        return new Packet(PacketType.CHECK_AUTO_BID, existingConfig);
    }

    private Packet handleCancelAutoBid(Packet request, Gson gson) {
        AutoBidPayload cancelData = gson.fromJson(request.getPayload(), AutoBidPayload.class);
        boolean cancelSuccess = new AuctionDAOImpl().cancelAutoBid(cancelData.getAuctionId(), cancelData.getUserId());
        return new Packet(PacketType.CANCEL_AUTO_BID, new ResultPayload(cancelSuccess, cancelSuccess ? "Đã tắt hệ thống Đấu giá tự động!" : "Lỗi hệ thống khi tắt Auto-Bid!", -1, "", "", "", "", ""));
    }

    private Packet handleLoadDashboard(Packet request, Gson gson) {
        AuctionDAO dashDao = new AuctionDAOImpl();
        DashboardResponsePayload dashRes = dashDao.getDashboardStats();
        return new Packet(PacketType.LOAD_DASHBOARD, dashRes);
    }

    private Packet handleAuctionSubscribe(Packet request, Gson gson) {
        AuctionSubscribePayload subscribePayload = gson.fromJson(request.getPayload(), AuctionSubscribePayload.class);
        ClientHandler currentClient = (ClientHandler) Thread.currentThread();
        Packet k = new Packet(PacketType.AUCTION_SUBSCRIBE, new ResultPayload(true, "", -1, "", "", "", "", ""));
        if (subscribePayload.isSub()){
            announcer.addObserver(subscribePayload.getAuctionId(), currentClient);
            return k;
        }
        else {
            announcer.removeObserver(subscribePayload.getAuctionId(), currentClient);
            return k;
        }
    }
}
