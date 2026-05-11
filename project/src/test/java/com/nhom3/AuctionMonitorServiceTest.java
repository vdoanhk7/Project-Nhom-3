package com.nhom3;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.nhom3.server.dao.AuctionDAO;
import com.nhom3.server.service.AuctionMonitorService;

public class AuctionMonitorServiceTest {

    @Mock
    private AuctionDAO auctionDAO;

    @Mock
    private ScheduledExecutorService scheduler;

    private AuctionMonitorService auctionMonitorService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        auctionMonitorService = new AuctionMonitorService(auctionDAO, scheduler);
    }

    @Test
    void startMonitoring_ShouldScheduleAtFixedRate() {
        auctionMonitorService.startMonitoring();

        verify(scheduler, times(1)).scheduleAtFixedRate(
                any(Runnable.class),
                eq(0L),
                eq(5L),
                eq(TimeUnit.SECONDS));
    }

    @Test
    void startMonitoring_CalledTwice_ShouldScheduleOnlyOnce() {
        auctionMonitorService.startMonitoring();
        auctionMonitorService.startMonitoring();

        verify(scheduler, times(1)).scheduleAtFixedRate(
                any(Runnable.class),
                anyLong(),
                anyLong(),
                any(TimeUnit.class));
    }

    @Test
    void stopMonitoring_ShouldShutdownScheduler() {
        // Mock scheduler.isShutdown() to return false initially
        when(scheduler.isShutdown()).thenReturn(false);

        auctionMonitorService.startMonitoring(); // Start first to set isRunning to true
        auctionMonitorService.stopMonitoring();

        verify(scheduler, times(1)).shutdown();

        try {
            Field isRunningField = AuctionMonitorService.class.getDeclaredField("isRunning");
            isRunningField.setAccessible(true);
            AtomicBoolean isRunning = (AtomicBoolean) isRunningField.get(auctionMonitorService);
            assertFalse(isRunning.get());
        } catch (Exception e) {
            // Ignore reflection errors in tests
        }
    }

    @Test
    void stopMonitoring_WhenSchedulerAlreadyShutdown_ShouldNotShutdownAgain() {
        when(scheduler.isShutdown()).thenReturn(true);

        auctionMonitorService.stopMonitoring();

        verify(scheduler, never()).shutdown();
    }

    @Test
    void performAuctionChecks_ShouldCallDaoMethods() throws Exception {
        // Start monitoring to capture the Runnable
        auctionMonitorService.startMonitoring();

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).scheduleAtFixedRate(
                runnableCaptor.capture(),
                anyLong(),
                anyLong(),
                any(TimeUnit.class));

        Runnable checkTask = runnableCaptor.getValue();

        // Execute the task
        checkTask.run();

        // Verify that the DAO methods were called with a Timestamp
        verify(auctionDAO, times(1)).startScheduledAuctions(any(Timestamp.class));
        verify(auctionDAO, times(1)).closeExpiredAuctions(any(Timestamp.class));
    }
}
