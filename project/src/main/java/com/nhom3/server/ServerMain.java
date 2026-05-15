package com.nhom3.server;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

import com.nhom3.server.network.AuctionHandler;
import com.nhom3.server.network.ClientHandler;
import com.nhom3.server.service.AuctionMonitorService;

public class ServerMain {

    public static void main(String[] args) {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        
        AuctionMonitorService monitor = new AuctionMonitorService();
        monitor.startMonitoring();

        Server server;
        try {
            server = new Server();
            System.out.println("Server đang lắng nghe tại cổng " + Server.PORT);
        } catch (IOException e) {
            System.err.println("Không thể khởi động server: " + e.getMessage());
            return;
        }

        // Đăng ký Shutdown Hook để dọn dẹp tài nguyên (Graceful Shutdown) khi tắt ứng dụng
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nĐang thực hiện Graceful Shutdown...");
            server.shutdown();
            AuctionHandler.getInstance().shutdown();
            System.out.println("Server đã được tắt an toàn.");
        }));

        while (!server.getServerSocket().isClosed()) {
            try {
                // Đợi lấy 1 slot trước khi accept kết nối mới
                server.acquireClientSlot();
                
                Socket clientSocket = server.getServerSocket().accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                server.addClientHandler(clientHandler);
                
            } catch (InterruptedException e) {
                System.err.println("Tiến trình chờ kết nối bị gián đoạn.");
                Thread.currentThread().interrupt();
                break;
            } catch (SocketException e) {
                System.out.println("Server socket đã đóng.");
                break;
            } catch (IOException e) {
                System.err.println("Lỗi khi chấp nhận kết nối: " + e.getMessage());
                // Nếu bị lỗi ở socket nhưng chưa tạo được client, phải trả lại slot đã lấy
                server.releaseClientSlot(); 
            }
        }
    }
}
