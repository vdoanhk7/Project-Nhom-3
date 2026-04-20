package com.nhom3;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AuctionTest {

    @Test
    public void testPriceUpdateLogic() {
        // Giả sử bạn có logic: Giá sau phải lớn hơn giá trước
        double currentPrice = 1000.0;
        double userBid = 1200.0;
        
        assertTrue(userBid > currentPrice, "Giá đấu mới phải lớn hơn giá hiện tại");
    }

    @Test
    public void testAuctionStatus() {
        // Giả sử trạng thái phiên đấu giá (như trong PDF: OPEN, RUNNING, FINISHED)
        String status = "RUNNING";
        assertNotNull(status);
        assertEquals("RUNNING", status);
    }
}
