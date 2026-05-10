package com.nhom3.server.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.nhom3.server.dao.AuctionDAO;
import com.nhom3.server.dao.AuctionDAOImpl;

/**
 * Service chịu trách nhiệm tự động kiểm tra và cập nhật trạng thái các phiên
 * đấu giá.
 * Bao gồm: Mở các phiên đấu giá đến giờ và đóng các phiên đấu giá hết hạn.
 */
public class AuctionMonitorService {
    private static final Logger log = LoggerFactory.getLogger(AuctionMonitorService.class);

    // Các thông số cấu hình giám sát
    private static final int INITIAL_DELAY = 0;
    private static final int MONITOR_PERIOD = 5;
    private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;

    private final AuctionDAO auctionDAO;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    /**
     * Khởi tạo service.
     */
    public AuctionMonitorService() {
        this.auctionDAO = new AuctionDAOImpl();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AuctionMonitor-Worker");
            t.setDaemon(true); // Để thread này không ngăn JVM tắt
            return t;
        });
    }

    /**
     * Bắt đầu tiến trình giám sát định kỳ.
     */
    public void startMonitoring() {
        if (isRunning.getAndSet(true)) {
            log.warn("[Monitor] Service giám sát đã đang chạy.");
            return;
        }

        log.info("[Monitor] Bắt đầu giám sát đấu giá (Chu kỳ: {} {})...", MONITOR_PERIOD, TIME_UNIT);

        scheduler.scheduleAtFixedRate(this::performAuctionChecks, INITIAL_DELAY, MONITOR_PERIOD, TIME_UNIT);
    }

    /**
     * Logic kiểm tra và cập nhật trạng thái các phiên đấu giá.
     */
    private void performAuctionChecks() {
        try {
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());

            // Tự động mở các phiên đấu giá đã đến thời gian bắt đầu
            auctionDAO.startScheduledAuctions(now);

            // Tự động đóng các phiên đấu giá đã đến thời gian kết thúc
            auctionDAO.closeExpiredAuctions(now);

        } catch (Exception e) {
            log.error("[Monitor] Lỗi xảy ra trong quá trình kiểm tra định kỳ: ", e);
        }
    }

    /**
     * Dừng tiến trình giám sát và giải phóng tài nguyên.
     */
    public void stopMonitoring() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            isRunning.set(false);
            log.info("[Monitor] Đã dừng service giám sát.");
        }
    }
}