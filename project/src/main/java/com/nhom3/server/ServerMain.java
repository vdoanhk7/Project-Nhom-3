package com.nhom3.server;

import java.io.IOException;
import java.net.Socket;
import com.nhom3.server.service.AuctionMonitorService;
import com.nhom3.server.network.ClientHandler;

public class ServerMain {

    public static void main(String[] args) {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        // Khởi động các Service cần thiết (nếu có)
        AuctionMonitorService monitor = new AuctionMonitorService();
        monitor.startMonitoring();

        // Khởi tạo Server lắng nghe tại cổng 8080
        // Port 8080 là cổng do người dùng chọn.
        // Sau chuyển sang class khác để khởi tạo server
        Server server;
        try {
            server = new Server();
            System.out.println("Server đang lắng nghe tại cổng " + Server.PORT);
        } catch (IOException e) {
            System.err.println("Không thể khởi động server: " + e.getMessage());
            return;
        }
        // Vòng lặp chính để chấp nhận kết nối từ client
        while (true) {
            try {
                Socket clientSocket = server.getServerSocket().accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                server.addClientHandler(clientHandler);
            } catch (IOException e) {
                System.err.println("Lỗi khi chấp nhận kết nối: " + e.getMessage());
            }
        }
    }
}
