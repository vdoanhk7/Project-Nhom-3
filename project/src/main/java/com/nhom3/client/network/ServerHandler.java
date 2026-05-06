package com.nhom3.client.network;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.nhom3.shared.model.user.User;
import com.nhom3.shared.network.packet.Packet;
import com.nhom3.shared.network.payload.ResultPayload;
import com.nhom3.client.controller.LoginController;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import com.nhom3.client.controller.DashboardController;
import com.nhom3.shared.model.user.UserInfo;
import com.nhom3.shared.model.user.Bidder;
import com.nhom3.shared.model.user.Seller;
import com.nhom3.shared.model.user.Admin;
import com.nhom3.shared.network.payload.BidHistoryResponsePayload;
import com.nhom3.client.controller.ViewItemDetailController;
import com.nhom3.shared.model.user.UserContact;


public class ServerHandler extends Thread {
    private ServerConnection serverConnection;
    private boolean isListening;
    private static final Logger logger = LoggerFactory.getLogger(ServerHandler.class);
    private Gson gson = new Gson();

    public ServerHandler() {
        this.serverConnection = ServerConnection.getInstance();
        isListening = true;
    }

    public ServerConnection getServerConnection() { return serverConnection; }

    @Override
    public void run() {
        while (isListening) {
            try {
                String jsonResponse = serverConnection.receiveResponse();
                Packet response = gson.fromJson(jsonResponse, Packet.class);
                System.out.println("[CLIENT - RAW RECEIVE] Vừa nhận phản hồi loại: " + response.getType());
                // ĐẨY VÀO LUỒNG GIAO DIỆN CHÍNH CỦA JAVAFX
                Platform.runLater(() -> {
                    switch (response.getType()) {
                        case LOGIN:
                            String tempJson = gson.toJson(response.getPayload()); 
                            ResultPayload loginResult = gson.fromJson(tempJson, ResultPayload.class);
                            
                            logger.info("Server phản hồi Đăng nhập: {}", loginResult.getResult());
                            
                            Platform.runLater(() -> {
                                try {
                                    if (LoginController.getInstance() != null) {
                                        // Nếu đăng nhập thành công, tái tạo lại Object User
                                        // Trong case LOGIN:
                                        User loggedInUser = null;
                                        if (loginResult.getResult()) {
                                            UserInfo info = new UserInfo(loginResult.getUsername(), "", loginResult.getFullName());
                                            // TÁI TẠO USER CONTACT THAY VÌ ĐỂ NULL
                                            UserContact contact = new UserContact(loginResult.getEmail(), loginResult.getPhone());
                                            
                                            if ("BIDDER".equals(loginResult.getRole())) {
                                                loggedInUser = new Bidder(loginResult.getUserId(), info, contact);
                                            } else if ("SELLER".equals(loginResult.getRole())) {
                                                loggedInUser = new Seller(loginResult.getUserId(), info, contact);
                                            } else {
                                                loggedInUser = new Admin(loginResult.getUserId(), info, contact);
                                            }
                                        }
                                        
                                        // Gửi lên giao diện
                                        LoginController.getInstance().handleLoginResult(loginResult.getResult(), loggedInUser);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace(); // In ra lỗi nếu UI bị sập
                                }
                            });
                            break;
                        
                        case REGISTER:
                            String regResJson = gson.toJson(response.getPayload());
                            ResultPayload regResultPayload = gson.fromJson(regResJson, ResultPayload.class);
                            
                            logger.info("Server phản hồi Đăng ký: {}", regResultPayload.getResult());
                            
                            // GỌI VỀ GIAO DIỆN (Bắt buộc dùng Platform.runLater)
                            Platform.runLater(() -> {
                                try {
                                    if (com.nhom3.client.controller.SignupController.getInstance() != null) {
                                        com.nhom3.client.controller.SignupController.getInstance().handleSignupResult(
                                            regResultPayload.getResult(), 
                                            regResultPayload.getMessage()
                                        );
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            });
                            break;

                        case PLACE_BID:
                            String bidResJson = gson.toJson(response.getPayload());
                            ResultPayload bidRes = gson.fromJson(bidResJson, ResultPayload.class);
                            
                            logger.info("Server phản hồi Đặt giá: {}", bidRes.getResult());
                            
                            // Ném lên UI Thread
                            Platform.runLater(() -> {
                                try {
                                    if (com.nhom3.client.controller.ViewItemDetailController.getInstance() != null) {
                                        com.nhom3.client.controller.ViewItemDetailController.getInstance().handleBidResult(
                                            bidRes.getResult(),
                                            bidRes.getMessage()
                                        );
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            });
                            break;
                        
                        case LOAD_BID_HISTORY:
                            String histJson = gson.toJson(response.getPayload());
                            BidHistoryResponsePayload histResult = gson.fromJson(histJson, BidHistoryResponsePayload.class);
                            
                            // Đẩy lên UI
                            Platform.runLater(() -> {
                                try {
                                    if (ViewItemDetailController.getInstance() != null) {
                                        ViewItemDetailController.getInstance().handleLoadHistoryResult(histResult.getAuctionId(), histResult.getHistoryList());
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            });
                            break;

                        case LOAD_SELLER_ITEMS:
                            String itemsResJson = gson.toJson(response.getPayload());
                            com.nhom3.shared.network.payload.SellerItemsResponsePayload itemsResult = gson.fromJson(itemsResJson, com.nhom3.shared.network.payload.SellerItemsResponsePayload.class);
                            
                            Platform.runLater(() -> {
                                try {
                                    if (com.nhom3.client.controller.ManageItemController.getInstance() != null) {
                                        com.nhom3.client.controller.ManageItemController.getInstance().handleLoadItemsResult(itemsResult.getItems());
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            });
                            break;

                        case PUBLISH_AUCTION:
                            String pubResJson = gson.toJson(response.getPayload());
                            ResultPayload pubRes = gson.fromJson(pubResJson, ResultPayload.class);
                            
                            Platform.runLater(() -> {
                                try {
                                    if (com.nhom3.client.controller.PublishAuctionController.getInstance() != null) {
                                        com.nhom3.client.controller.PublishAuctionController.getInstance().handlePublishResult(
                                            pubRes.getResult(), pubRes.getMessage()
                                        );
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            });
                            break;

                        case LOAD_PURCHASE_HISTORY:
                            String purHistJson = gson.toJson(response.getPayload());
                            com.nhom3.shared.network.payload.PurchaseHistoryResponsePayload purHistRes = gson.fromJson(purHistJson, com.nhom3.shared.network.payload.PurchaseHistoryResponsePayload.class);
                            
                            Platform.runLater(() -> {
                                try {
                                    if (com.nhom3.client.controller.PurchaseHistoryController.getInstance() != null) {
                                        com.nhom3.client.controller.PurchaseHistoryController.getInstance().handleLoadHistoryResult(purHistRes.getHistoryList());
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            });
                            break;

                        case PLACE_AUTO_BID:
                            String autoResJson = gson.toJson(response.getPayload());
                            ResultPayload autoRes = gson.fromJson(autoResJson, ResultPayload.class);
                            
                            Platform.runLater(() -> {
                                Alert alert = new Alert(autoRes.getResult() ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
                                alert.setTitle("Auto-Bid");
                                alert.setHeaderText(null);
                                alert.setContentText(autoRes.getMessage());
                                alert.showAndWait();
                            });
                            break;

                        case LOAD_DASHBOARD:
                            String dashJson = gson.toJson(response.getPayload());
                            com.nhom3.shared.network.payload.DashboardResponsePayload dashResult = gson.fromJson(dashJson, com.nhom3.shared.network.payload.DashboardResponsePayload.class);
                            
                            Platform.runLater(() -> {
                                try {
                                    if (DashboardController.getInstance() != null) {
                                        DashboardController.getInstance().handleDashboardData(dashResult);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            });
                            break;

                        default:
                            logger.warn("Loại gói tin không xác định: {}", response.getType());
                    }
                });

            } catch (IOException e) {
                logger.error("Lỗi mất kết nối. Đang thử kết nối lại...", e);
                isListening = false; 
            }
        }
    }
}