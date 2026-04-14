package com.nhom3.sever.service;

import com.nhom3.sever.dao.AuctionDAO;
import com.nhom3.sever.dao.AuctionDAOImpl;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionMonitorService {
    private final AuctionDAO auctionDAO;
    private final ScheduledExecutorService scheduler;

    public AuctionMonitorService() {
        this.auctionDAO = new AuctionDAOImpl();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void startMonitoring() {
        System.out.println("[Monitor] Bắt đầu luồng tuần tra thời gian đấu giá...");
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                auctionDAO.closeExpiredAuctions();
                
                // 2. Tương lai (Phần Socket): Thông báo cho các Client đang xem biết là phiên đã kết thúc
                // notifyClientsAuctionFinished();
                
            } catch (Exception e) {
                System.err.println("Lỗi trong lúc tuần tra: " + e.getMessage());
            }
        }, 0, 5, TimeUnit.SECONDS); // 0 = chạy ngay lập tức, 5 = khoảng cách giữa các lần chạy
    }

    public void stopMonitoring() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            System.out.println("[Monitor] Đã tắt luồng tuần tra.");
        }
    }
}