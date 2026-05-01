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

public class ClientHandler extends Thread {
    private final Socket clientSocket;
    private final AuthService authService; // Khởi tạo Service xử lý DB

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        this.authService = new AuthService(); 
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
                        String tempJson = gson.toJson(request.getPayload()); 
                        LoginPayload login = gson.fromJson(tempJson, LoginPayload.class);

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
                        String regJson = gson.toJson(request.getPayload());
                        RegisterPayload regData = gson.fromJson(regJson, RegisterPayload.class);                       
                        logger.info("Yêu cầu ĐĂNG KÝ từ user: " + regData.getUsername() + " (Role: " + regData.getRole() + ")");
                        // 2. Khởi tạo đối tượng User để lưu Database
                        UserInfo info = new UserInfo(regData.getUsername(), regData.getPassword(), regData.getFullName());
                        UserContact contact = new UserContact(regData.getEmail(), regData.getPhone());              
                        User newUser;
                        if ("SELLER".equals(regData.getRole())) {
                            newUser = new com.nhom3.shared.model.user.Seller(0, info, contact);
                        } else {
                            newUser = new com.nhom3.shared.model.user.Bidder(0, info, contact);
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
                        String bidJson = gson.toJson(request.getPayload());
                        com.nhom3.shared.network.payload.BidPayload bidData = gson.fromJson(bidJson, com.nhom3.shared.network.payload.BidPayload.class);
                        logger.info("Nhận yêu cầu Đặt giá: " + bidData.getAmount() + " từ User ID: " + bidData.getUserId());

                        // 1. GỌI DB ĐỂ UPDATE GIÁ LUÔN (SQL ĐÃ BẢO VỆ MỌI LUẬT CHƠI)
                        com.nhom3.server.dao.AuctionDAO auctionDAO = new com.nhom3.server.dao.AuctionDAOImpl();
                        boolean isBidSuccess = auctionDAO.updateHighestBid(bidData.getAuctionId(), bidData.getUserId(), bidData.getAmount());

                        // 2. NẾU UPDATE THÀNH CÔNG -> LƯU LỊCH SỬ VÀ GIA HẠN THỜI GIAN
                        if (isBidSuccess) {
                            // Lưu vào bảng Lịch sử (Bid Transactions)
                            com.nhom3.shared.model.user.Bidder dummyBidder = new com.nhom3.shared.model.user.Bidder(bidData.getUserId(), null, null);
                            com.nhom3.shared.model.auction.BidTransaction newBid = new com.nhom3.shared.model.auction.BidTransaction(0, dummyBidder, bidData.getAmount(), java.time.LocalDateTime.now(), "Đặt giá qua mạng");
                            boolean saveResult = auctionDAO.saveBidTransaction(newBid, bidData.getAuctionId());
                            System.out.println("[SERVER] Đã lưu lịch sử đặt giá vào DB: " + saveResult + " (Cho Auction ID: " + bidData.getAuctionId() + ")");
                            // Tính năng chống "bắn tỉa" phút chót: Cộng thêm 2 phút nếu đặt giá ở 30s cuối
                            java.time.LocalDateTime realEndTime = auctionDAO.getEndTime(bidData.getAuctionId());
                            if (realEndTime != null) {
                                long secondsLeft = java.time.temporal.ChronoUnit.SECONDS.between(java.time.LocalDateTime.now(), realEndTime);
                                if (secondsLeft > 0 && secondsLeft <= 30) {
                                    auctionDAO.extendAuctionTime(bidData.getAuctionId(), 2);
                                }
                            }
                        }

                        // 3. ĐÓNG GÓI TRẢ VỀ CHO CLIENT
                        ResultPayload bidResultPayload;
                        if (isBidSuccess) {
                            // Nhớ truyền đủ 8 tham số cho ResultPayload (các thuộc tính sau để rỗng)
                            bidResultPayload = new ResultPayload(true, "Đặt giá thành công", -1, "", "", "", "", "");
                        } else {
                            bidResultPayload = new ResultPayload(false, "Phiên đấu giá đã kết thúc hoặc có người trả giá cao hơn!", -1, "", "", "", "", "");
                        }

                        Packet bidResponse = new Packet(PacketType.PLACE_BID, bidResultPayload);
                        out.write(gson.toJson(bidResponse));
                        out.newLine();
                        out.flush();
                        break;
                    
                    case LOAD_BID_HISTORY:
                        // 1. Nhận yêu cầu
                        String reqJson = gson.toJson(request.getPayload());
                        AuctionIdPayload reqData = gson.fromJson(reqJson, AuctionIdPayload.class);
                        System.out.println("[SERVER] Client xin lịch sử của Auction ID: " + reqData.getAuctionId());
                        
                        // 2. Gọi Database lấy lịch sử xịn
                        AuctionDAO dao = new AuctionDAOImpl();
                        java.util.List<BidTransaction> dbHistory = dao.getBidHistory(reqData.getAuctionId());
                        System.out.println("[SERVER] Database trả về: " + dbHistory.size() + " dòng.");
                        
                        // 3. Chuyển Lịch sử Xịn thành Lịch sử Đơn giản (Chống lỗi Gson)
                        java.util.List<BidHistoryResponsePayload.SimpleBid> simpleList = new java.util.ArrayList<>();
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
                        String sellerJson = gson.toJson(request.getPayload());
                        com.nhom3.shared.network.payload.SellerIdPayload sellerReq = gson.fromJson(sellerJson, com.nhom3.shared.network.payload.SellerIdPayload.class);
                        
                        // 2. Chọc DB lấy danh sách sản phẩm và trạng thái
                        com.nhom3.server.dao.ItemDAO itemDAO = new com.nhom3.server.dao.ItemDAOImpl();
                        com.nhom3.server.dao.AuctionDAO auctionDAOForSeller = new com.nhom3.server.dao.AuctionDAOImpl();
                        
                        java.util.List<com.nhom3.shared.model.item.Item> itemsFromDb = itemDAO.getItemsBySellerId(sellerReq.getSellerId());
                        java.util.Map<Integer, String> statusMap = auctionDAOForSeller.getAuctionStatusBySeller(sellerReq.getSellerId());
                        
                        // 3. Đóng gói thành DTO để né lỗi Gson
                        java.util.List<com.nhom3.shared.network.payload.SellerItemsResponsePayload.SellerItemDTO> dtoList = new java.util.ArrayList<>();
                        for (com.nhom3.shared.model.item.Item itm : itemsFromDb) {
                            String stt = statusMap.getOrDefault(itm.getId(), "");
                            dtoList.add(new com.nhom3.shared.network.payload.SellerItemsResponsePayload.SellerItemDTO(
                                itm.getId(), itm.getName(), itm.getType(), itm.getStartPrice(), itm.getCurHighest(), stt
                            ));
                        }

                        // 4. Trả về Client
                        com.nhom3.shared.network.payload.SellerItemsResponsePayload sellerResponse = new com.nhom3.shared.network.payload.SellerItemsResponsePayload(dtoList);
                        Packet sellerPacket = new Packet(PacketType.LOAD_SELLER_ITEMS, sellerResponse);
                        out.write(gson.toJson(sellerPacket));
                        out.newLine();
                        out.flush();
                        break;

                    case PUBLISH_AUCTION:
                        // 1. Giải nén dữ liệu
                        String pubJson = gson.toJson(request.getPayload());
                        com.nhom3.shared.network.payload.PublishAuctionPayload pubData = gson.fromJson(pubJson, com.nhom3.shared.network.payload.PublishAuctionPayload.class);
                        
                        boolean isPubSuccess = false;
                        String pubMsg = "Lỗi không xác định";

                        try {
                            // 2. Tái tạo dữ liệu
                            java.time.LocalDateTime startTime = java.time.LocalDateTime.parse(pubData.getStartTime());
                            java.time.LocalDateTime endTime = java.time.LocalDateTime.parse(pubData.getEndTime());
                            
                            // Tạo Item giả (chỉ cần ID để DB nhận diện)
                            com.nhom3.shared.model.item.Item dummyItem = new com.nhom3.shared.model.item.Art(pubData.getItemId(), "", 0);
                            com.nhom3.shared.model.auction.Auction newAuction = new com.nhom3.shared.model.auction.Auction(0, dummyItem, startTime, endTime);

                            // 3. GỌI SERVICE (RẤT QUAN TRỌNG)
                            // Service sẽ tự so sánh: Nếu startTime <= Java_Now -> Set RUNNING ngay lập tức.
                            com.nhom3.server.service.AuctionService auctionService = new com.nhom3.server.service.AuctionService();
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
                    // Thêm các case REGISTER, PLACE_BID... tại đây

                }
            }
        } catch (IOException e) {
            logger.error("Client ngắt kết nối");
        }
    }
}