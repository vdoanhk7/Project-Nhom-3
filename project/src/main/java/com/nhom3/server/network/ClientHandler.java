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
                
                switch (request.getType()) {
                    case LOGIN:
                        String tempJson = gson.toJson(request.getPayload()); 
                        LoginPayload login = gson.fromJson(tempJson, LoginPayload.class);

                        logger.info("Yêu cầu đăng nhập từ: " + login.getUsername());
                        
                        // 1. GỌI DATABASE THẬT SỰ Ở ĐÂY
                        User user = authService.login(login.getUsername(), login.getPassword());
                        
                        // 2. ĐÓNG GÓI KẾT QUẢ
                        ResultPayload resultPayload;
                        if (user != null) {
                            // Trích xuất thông tin để gửi đi tránh lỗi Gson
                            resultPayload = new ResultPayload(
                                true, "Đăng nhập thành công", 
                                user.getId(), 
                                user.getUserInfo().getUserName(), 
                                user.getUserInfo().getName(), 
                                user.getRole().name()
                            );
                        } else {
                            resultPayload = new ResultPayload(false, "Sai tài khoản hoặc mật khẩu", -1, "", "", "");
                        }
                        
                        // 3. GỬI LẠI CLIENT (Lưu ý: set type là LOGIN để Client Handler dễ phân loại)
                        Packet response = new Packet(PacketType.LOGIN, resultPayload);
                        out.write(gson.toJson(response));
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