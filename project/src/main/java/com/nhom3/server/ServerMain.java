package com.nhom3.server;

import com.nhom3.server.network.AuctionServer;
import com.nhom3.server.service.AuctionMonitorService;

public class ServerMain {
    public static void main(String[] args) {
        // Khởi động các Service cần thiết (nếu có)
        AuctionMonitorService monitor = new AuctionMonitorService();
        monitor.startMonitoring();

        // Khởi tạo Server lắng nghe tại cổng 8080
        // Port 8080 là cổng do người dùng chọn.
        AuctionServer server = new AuctionServer(8080, 100);

        // start() chứa vòng lặp vô hạn accept() nên nó sẽ giữ chương trình chạy liên
        // tục.
        server.start();
    }
}
