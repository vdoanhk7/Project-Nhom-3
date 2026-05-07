package com.nhom3.server.network;

import java.io.*;
import java.net.Socket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.nhom3.shared.network.packet.Packet;
import com.nhom3.shared.network.packet.PacketType;
import com.nhom3.shared.network.payload.LoginPayload;
import com.nhom3.shared.network.payload.ResultPayload;
import com.nhom3.server.service.AuthService;
import com.nhom3.shared.model.user.User;
import com.nhom3.shared.model.user.UserInfo;
import com.nhom3.shared.model.user.UserContact;
import com.nhom3.shared.network.payload.RegisterPayload;
import com.nhom3.shared.network.payload.AuctionIdPayload;
import com.nhom3.shared.network.payload.BidHistoryResponsePayload;
import com.nhom3.shared.model.auction.BidTransaction;
import com.nhom3.server.dao.AuctionDAO;
import com.nhom3.server.dao.AuctionDAOImpl;
import com.nhom3.shared.model.user.Bidder;
import java.time.LocalDateTime;
import com.nhom3.shared.network.payload.BidPayload;
import com.nhom3.shared.network.payload.SellerIdPayload;
import com.nhom3.shared.model.item.Item;
import com.nhom3.shared.network.payload.SellerItemsResponsePayload;
import com.nhom3.server.dao.ItemDAO;
import com.nhom3.server.dao.ItemDAOImpl;
import java.util.List;
import java.util.Map;
import com.nhom3.shared.model.user.Seller;
import java.util.ArrayList;
import java.time.temporal.ChronoUnit;
import com.nhom3.shared.model.auction.Auction;
import com.nhom3.server.service.AuctionService;
import com.nhom3.shared.network.payload.PublishAuctionPayload;
import com.nhom3.shared.model.item.Art;
import com.nhom3.shared.network.payload.BidderIdPayload;
import com.nhom3.shared.network.payload.PurchaseHistoryResponsePayload;
import com.nhom3.shared.network.payload.AutoBidPayload;
import com.nhom3.shared.network.payload.DashboardResponsePayload;
public class ClientHandler extends Thread {
    private final Socket clientSocket;
    private final AuthService authService; // Khởi tạo Service xử lý DB
    private final AuctionService auctionService;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        this.authService = new AuthService(); 
        this.auctionService = new AuctionService();
    }

    @Override
    public void run() {
        Logger logger = LoggerFactory.getLogger(ClientHandler.class);
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
            String line;
            Gson gson = new Gson();

            while ((line = in.readLine()) != null) {
                Packet request = gson.fromJson(line, Packet.class);
                System.out.println("[SERVER - RAW RECEIVE] Vừa nhận được gói tin loại: " + request.getType());
                switch (request.getType()) {
                    case LOGIN:
                        LoginPayload login = gson.fromJson(request.getPayload(), LoginPayload.class);

                        logger.info("Yêu cầu đăng nhập từ: " + login.getUsername());
                        
                        // 1. GỌI DATABASE THẬT SỰ Ở ĐÂY
                        User user = authService.login(login.getUsername(), login.getPassword());
                        
                        // 2. ĐÓNG GÓI KẾT QUẢ (Trong case LOGIN)
                        ResultPayload resultPayload;
                        if (user != null) {
                            // Lấy email và phone (kiểm tra null để tránh lỗi DB cũ)
                            String uEmail = user.getUserContact() != null ? user.getUserContact().getEmail() : "";
                            String uPhone = user.getUserContact() != null ? user.getUserContact().getPhoneNumber() : "";

                            resultPayload = new ResultPayload(
                                true, "Đăng nhập thành công", 
                                user.getId(), 
                                user.getUserInfo().getUserName(), 
                                user.getUserInfo().getName(), 
                                user.getRole().name(),
                                uEmail, 
                                uPhone 
                            );
                        } else {
                            resultPayload = new ResultPayload(false, "Sai tài khoản hoặc mật khẩu", -1, "", "", "", "", "");
                        }
                        
                        // 3. GỬI LẠI CLIENT (Lưu ý: set type là LOGIN để Client Handler dễ phân loại)
                        Packet response = new Packet(PacketType.LOGIN, resultPayload);
                        out.write(gson.toJson(response));
                        out.newLine();
                        out.flush();
                        break;
                    
                    case REGISTER:
                        // 1. Giải nén payload
                        RegisterPayload regData = gson.fromJson(request.getPayload(), RegisterPayload.class);
                        logger.info("Yêu cầu ĐĂNG KÝ từ user: " + regData.getUsername() + " (Role: " + regData.getRole() + ")");
                        // 2. Khởi tạo đối tượng User để lưu Database
                        UserInfo info = new UserInfo(regData.getUsername(), regData.getPassword(), regData.getFullName());
                        UserContact contact = new UserContact(regData.getEmail(), regData.getPhone());              
                        User newUser;
                        if ("SELLER".equals(regData.getRole())) {
                            newUser = new Seller(0, info, contact);
                        } else {
                            newUser = new Bidder(0, info, contact);
                        }
                        // 3. Gọi Database (thông qua AuthService)
                        boolean isRegSuccess = authService.register(newUser);
                        // 4. Đóng gói kết quả (Tái sử dụng ResultPayload)
                        // Với đăng ký, ta chỉ quan tâm boolean và message, các tham số khác (id, tên) cứ truyền rỗng.
                        ResultPayload regResult;
                        if (isRegSuccess) {
                            regResult = new ResultPayload(true, "Đăng ký thành công", -1, "", "", "","", "");
                        } else {
                            regResult = new ResultPayload(false, "Tên đăng nhập đã tồn tại hoặc lỗi hệ thống", -1, "", "", "","", "");
                        }
                        // 5. Gửi về cho Client
                        Packet regResponse = new Packet(PacketType.REGISTER, regResult);
                        out.write(gson.toJson(regResponse));
                        out.newLine();
                        out.flush();
                        break;    
                    
                    case PLACE_BID:
                        // 1. Lấy dữ liệu (Đã áp dụng tối ưu Gson ở Phần 1)
                        BidPayload bidData = gson.fromJson(request.getPayload(), BidPayload.class);                 
                        logger.info("Nhận yêu cầu Đặt giá: " + bidData.getAmount() + " từ User ID: " + bidData.getUserId());
                        ResultPayload bidResultPayload;
                        try {
                            // 2. Lấy thông tin Phiên đấu giá từ Database
                            com.nhom3.server.dao.AuctionDAO dao = new com.nhom3.server.dao.AuctionDAOImpl();
                            com.nhom3.shared.model.auction.Auction currentAuction = dao.getAuctionById(bidData.getAuctionId());

                            if (currentAuction == null) {
                                throw new IllegalStateException("Không tìm thấy phiên đấu giá này!");
                            }
                            // 3. Chuẩn bị Object BidTransaction để truyền cho Service
                            com.nhom3.shared.model.user.Bidder bidder = new com.nhom3.shared.model.user.Bidder(bidData.getUserId(), null, null);
                            com.nhom3.shared.model.auction.BidTransaction newBid = new com.nhom3.shared.model.auction.BidTransaction(
                                0, bidder, bidData.getAmount(), java.time.LocalDateTime.now(), "Đặt giá qua mạng"
                            );
                            // 4. GỌI XUỐNG SERVICE (Chìa khóa để đồng bộ đa luồng - Thread-safe)
                            boolean isBidSuccess = auctionService.placeBid(currentAuction, newBid);

                            if (isBidSuccess) {
                                bidResultPayload = new ResultPayload(true, "Đặt giá thành công", -1, "", "", "", "", "");
                            } else {
                                bidResultPayload = new ResultPayload(false, "Có người đã trả giá cao hơn, vui lòng thử lại!", -1, "", "", "", "", "");
                            }
                        } catch (IllegalStateException e) {
                            // Bắt lỗi luật chơi do Service quăng ra (Ví dụ: Phiên chưa mở, phiên đã kết thúc...)
                            bidResultPayload = new ResultPayload(false, e.getMessage(), -1, "", "", "", "", "");
                        } catch (Exception e) {
                            e.printStackTrace();
                            bidResultPayload = new ResultPayload(false, "Lỗi hệ thống máy chủ!", -1, "", "", "", "", "");
                        }
                        // 5. Trả về Client
                        Packet bidResponse = new Packet(PacketType.PLACE_BID, bidResultPayload);
                        out.write(gson.toJson(bidResponse));
                        out.newLine();
                        out.flush();
                        break;
                    
                    case LOAD_BID_HISTORY:
                        // 1. Nhận yêu cầu
                        AuctionIdPayload reqData = gson.fromJson(request.getPayload(), AuctionIdPayload.class);
                        System.out.println("[SERVER] Client xin lịch sử của Auction ID: " + reqData.getAuctionId());
                        
                        // 2. Gọi Database lấy lịch sử xịn
                        AuctionDAO dao = new AuctionDAOImpl();
                        List<BidTransaction> dbHistory = dao.getBidHistory(reqData.getAuctionId());
                        System.out.println("[SERVER] Database trả về: " + dbHistory.size() + " dòng.");
                        
                        // 3. Chuyển Lịch sử Xịn thành Lịch sử Đơn giản (Chống lỗi Gson)
                        List<BidHistoryResponsePayload.SimpleBid> simpleList = new ArrayList<>();
                        for (BidTransaction b : dbHistory) {
                            String bName = b.getBidder() != null ? b.getBidder().getUserInfo().getName() : "Ẩn danh";
                            simpleList.add(new BidHistoryResponsePayload.SimpleBid(
                                b.getAmount(),
                                b.getBidTime().toString(), // Parse LocalDateTime thành String chuẩn ISO
                                b.getNote(),
                                bName
                            ));
                        }

                        // 4. Trả về Client
                        BidHistoryResponsePayload historyResponse = new BidHistoryResponsePayload(reqData.getAuctionId(),simpleList);
                        Packet histPacket = new Packet(PacketType.LOAD_BID_HISTORY, historyResponse);
                        out.write(gson.toJson(histPacket));
                        out.newLine();
                        out.flush();
                        break;

                    case LOAD_SELLER_ITEMS:
                        // 1. Bóc yêu cầu
                        SellerIdPayload sellerReq = gson.fromJson(request.getPayload(), SellerIdPayload.class);

                        // 2. Chọc DB lấy danh sách sản phẩm và trạng thái
                        ItemDAO itemDAO = new ItemDAOImpl();
                        AuctionDAO auctionDAOForSeller = new AuctionDAOImpl();
                        
                        List<Item> itemsFromDb = itemDAO.getItemsBySellerId(sellerReq.getSellerId());
                        Map<Integer, String> statusMap = auctionDAOForSeller.getAuctionStatusBySeller(sellerReq.getSellerId());
                        
                        // 3. Đóng gói thành DTO để né lỗi Gson
                        List<SellerItemsResponsePayload.SellerItemDTO> dtoList = new ArrayList<>();
                        for (Item itm : itemsFromDb) {
                            String stt = statusMap.getOrDefault(itm.getId(), "");
                            dtoList.add(new SellerItemsResponsePayload.SellerItemDTO(
                                itm.getId(), itm.getName(), itm.getType(), itm.getStartPrice(), itm.getCurHighest(), stt
                            ));
                        }

                        // 4. Trả về Client
                        SellerItemsResponsePayload sellerResponse = new SellerItemsResponsePayload(dtoList);
                        Packet sellerPacket = new Packet(PacketType.LOAD_SELLER_ITEMS, sellerResponse);
                        out.write(gson.toJson(sellerPacket));
                        out.newLine();
                        out.flush();
                        break;

                    case PUBLISH_AUCTION:
                        // 1. Giải nén dữ liệu
                        PublishAuctionPayload pubData = gson.fromJson(request.getPayload(), PublishAuctionPayload.class);

                        boolean isPubSuccess = false;
                        String pubMsg = "Lỗi không xác định";

                        try {
                            // 2. Tái tạo dữ liệu
                            LocalDateTime startTime = LocalDateTime.parse(pubData.getStartTime());
                            LocalDateTime endTime = LocalDateTime.parse(pubData.getEndTime());
                            
                            // Tạo Item giả (chỉ cần ID để DB nhận diện)
                            Item dummyItem = new Art(pubData.getItemId(), "", 0);
                            Auction newAuction = new Auction(0, dummyItem, startTime, endTime);

                            // 3. GỌI SERVICE (RẤT QUAN TRỌNG)
                            // Service sẽ tự so sánh: Nếu startTime <= Java_Now -> Set RUNNING ngay lập tức.
                            AuctionService auctionService = new AuctionService();
                            isPubSuccess = auctionService.createAuction(newAuction);
                            
                            if (isPubSuccess) pubMsg = "Đăng bán thành công!";
                            else pubMsg = "Lỗi lưu Database!";
                            
                        } catch (IllegalStateException | IllegalArgumentException e) {
                            pubMsg = e.getMessage(); // Bắt các lỗi do Service ném ra (VD: Đang có phiên chạy rồi)
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        // 4. Trả kết quả
                        ResultPayload pubResult = new ResultPayload(isPubSuccess, pubMsg, -1, "", "", "", "", "");
                        Packet pubResponse = new Packet(PacketType.PUBLISH_AUCTION, pubResult);
                        out.write(gson.toJson(pubResponse));
                        out.newLine();
                        out.flush();
                        break;

                    case LOAD_PURCHASE_HISTORY:
                        BidderIdPayload bidderReq = gson.fromJson(request.getPayload(), BidderIdPayload.class);
                        
                        logger.info("Client xin lịch sử đấu giá của Bidder ID: " + bidderReq.getBidderId());

                        AuctionDAO auctionDAOForHist = new AuctionDAOImpl();
                        List<Auction> dbHistoryList = auctionDAOForHist.getMyBidHistory(bidderReq.getBidderId());
                        
                        List<PurchaseHistoryResponsePayload.HistoryDTO> purDtoList = new ArrayList<>();
                        for (Auction a : dbHistoryList) {
                            double amount = a.getBidHistory().isEmpty() ? 0 : a.getBidHistory().get(0).getAmount();
                            String timeStr = a.getBidHistory().isEmpty() ? "" : a.getBidHistory().get(0).getBidTime().toString();
                            int topBidderId = a.getHighestBidder() != null ? a.getHighestBidder().getId() : -1;

                            // BỌC THÊM CÁC THÔNG SỐ VỀ THỜI GIAN VÀ GIÁ
                            purDtoList.add(new PurchaseHistoryResponsePayload.HistoryDTO(
                                a.getId(), a.getItem().getId(), a.getItem().getName(), amount, timeStr, a.getStatus().name(), topBidderId,
                                a.getItem().getType(), a.getItem().getStartPrice(), a.getItem().getCurHighest(),
                                a.getStartTime().toString(), a.getEndTime().toString()
                            ));
                        }

                        PurchaseHistoryResponsePayload purRes = new PurchaseHistoryResponsePayload(purDtoList);
                        Packet purPacket = new Packet(PacketType.LOAD_PURCHASE_HISTORY, purRes);
                        out.write(gson.toJson(purPacket));
                        out.newLine();
                        out.flush();
                        break;

                    case PLACE_AUTO_BID:
                        AutoBidPayload autoData = gson.fromJson(request.getPayload(), AutoBidPayload.class);

                        logger.info("Yêu cầu cài Auto-Bid từ User ID " + autoData.getUserId() + " cho Phiên ID " + autoData.getAuctionId());

                        AuctionDAO adao = new AuctionDAOImpl();
                        boolean autoSuccess = adao.saveAutoBidConfig(autoData);

                        ResultPayload autoResult = new ResultPayload(
                            autoSuccess, 
                            autoSuccess ? "Hệ thống đã ghi nhận thiết lập Auto-Bid của bạn!" : "Lỗi Database khi cài đặt Auto-Bid!", 
                            -1, "", "", "", "", ""
                        );
                        
                        Packet autoPacket = new Packet(PacketType.PLACE_AUTO_BID, autoResult);
                        out.write(gson.toJson(autoPacket));
                        out.newLine();
                        out.flush();
                        break;

                    case LOAD_DASHBOARD:
                        logger.info("Nhận yêu cầu tải dữ liệu Dashboard.");
                        
                        // Gọi DB 1 lần lấy đủ 5 thông số
                        AuctionDAO dashDao = new AuctionDAOImpl();
                        DashboardResponsePayload dashRes = dashDao.getDashboardStats();
                        
                        // Gửi về Client
                        Packet dashPacket = new Packet(PacketType.LOAD_DASHBOARD, dashRes);
                        out.write(gson.toJson(dashPacket));
                        out.newLine();
                        out.flush();
                        break;
                    // Thêm các case REGISTER, PLACE_BID... tại đây

                }
            }
        } catch (IOException e) {
            logger.error("Client ngắt kết nối");
        } finally {
        try { clientSocket.close(); } catch(IOException ex){}
        }
    }
}