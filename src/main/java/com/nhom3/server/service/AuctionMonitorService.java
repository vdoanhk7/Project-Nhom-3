package com.nhom3.server.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.nhom3.server.dao.AuctionDAO;
import com.nhom3.server.dao.AuctionDAOImpl;

public class AuctionMonitorService {
    private final AuctionDAO auctionDAO;
    private final ScheduledExecutorService scheduler;
    private static final Logger log = LoggerFactory.getLogger(AuctionMonitorService.class);

    public AuctionMonitorService() {
        this.auctionDAO = new AuctionDAOImpl();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void startMonitoring() {
        log.info("[Monitor] Bắt đầu luồng tuần tra thời gian đấu giá...");
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // LẤY GIỜ CHUẨN CỦA SERVER JAVA
                java.sql.Timestamp javaNow = java.sql.Timestamp.valueOf(java.time.LocalDateTime.now());
                
                // Truyền đồng hồ cho Bác bảo vệ
                auctionDAO.startScheduledAuctions(javaNow); 
                auctionDAO.closeExpiredAuctions(javaNow);
                
            } catch (Exception e) {
                log.error("Lỗi trong lúc tuần tra: " + e.getMessage());
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    public void stopMonitoring() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            log.info("[Monitor] Đã tắt luồng tuần tra.");
        }
    }

    
}