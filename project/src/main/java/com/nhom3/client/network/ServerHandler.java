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
import com.nhom3.shared.model.user.UserInfo;
import com.nhom3.shared.model.user.Bidder;
import com.nhom3.shared.model.user.Seller;
import com.nhom3.shared.model.user.Admin;

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
                                        User loggedInUser = null;
                                        if (loginResult.getResult()) {
                                            UserInfo info = new UserInfo(loginResult.getUsername(), "", loginResult.getFullName());
                                            // Tái tạo dựa trên Role
                                            if ("BIDDER".equals(loginResult.getRole())) {
                                                loggedInUser = new Bidder(loginResult.getUserId(), info, null);
                                            } else if ("SELLER".equals(loginResult.getRole())) {
                                                loggedInUser = new Seller(loginResult.getUserId(), info, null);
                                            } else {
                                                loggedInUser = new Admin(loginResult.getUserId(), info, null);
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
                            
                        // Tương tự cho các case REGISTER, PLACE_BID, UPDATE_MARKET...
                        case REGISTER:
                            // Xử lý gọi về SignupController
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