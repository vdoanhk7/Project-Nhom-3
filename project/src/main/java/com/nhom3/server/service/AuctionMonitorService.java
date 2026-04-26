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
                auctionDAO.closeExpiredAuctions();
                
                // 2. Tương lai (Phần Socket): Thông báo cho các Client đang xem biết là phiên đã kết thúc
                // notifyClientsAuctionFinished();
                
            } catch (Exception e) {
                log.error("Lỗi trong lúc tuần tra: " + e.getMessage());
            }
        }, 0, 5, TimeUnit.SECONDS); // 0 = chạy ngay lập tức, 5 = khoảng cách giữa các lần chạy
    }

    public void stopMonitoring() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            log.info("[Monitor] Đã tắt luồng tuần tra.");
        }
    }
}