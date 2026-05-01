package com.nhom3;
<<<<<<< HEAD

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.nhom3.server.dao.AuctionDAO;
import com.nhom3.server.service.AuctionService;
import com.nhom3.shared.model.auction.Auction;
import com.nhom3.shared.model.auction.BidTransaction;
import com.nhom3.shared.model.auction.StatusOfAuction;
import com.nhom3.shared.model.item.Item;
import com.nhom3.shared.model.user.Bidder;
import com.nhom3.shared.model.user.Seller;
import com.nhom3.shared.model.user.UserInfo;


class AuctionServiceTest {

    @Mock
    private AuctionDAO auctionDAO;

    @InjectMocks
    private AuctionService auctionService;

    @BeforeEach
    void setUp() {
        // Khởi tạo các mock object
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateAuction_Success() {
        // 1. Giả lập các đối tượng phụ thuộc
        UserInfo info = new UserInfo("email@test.com", "0123456", "Nguyen Van A");
        Seller seller = new Seller(002, info, null); 
        Item item = mock(Item.class); // Tạo một bản sao giả của Item mà không cần new
        
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusHours(1);

        // 2. Thực thi
        auctionService.createAuction(seller, item, 1, start, end);

        // 3. Kiểm tra
        assertEquals(1, seller.getManagedAuctions().size());
    }

    @Test
    void testStartAuction_FromOpenToRunning() {
        // Giả lập auction đang ở trạng thái OPEN
        Item item = mock(Item.class);
        Auction auction = new Auction(1, item, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        auction.setStatus(StatusOfAuction.OPEN);

        auctionService.startAuction(auction);

        assertEquals(StatusOfAuction.RUNNING, auction.getStatus());
    }

    @Test
    void testEndAuction_FromRunningToFinished() {
        // Giả lập auction đang chạy
        Item item = mock(Item.class);
        Auction auction = new Auction(1, item, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        auction.setStatus(StatusOfAuction.RUNNING);
        
        // Giả lập có người đấu giá cao nhất
        UserInfo info = new UserInfo("email@test.com", "0123456", "Nguyen Van A");
        Bidder bidder = new Bidder(10, info, null);
        auction.setHighestBidder(bidder);

        auctionService.endAuction(auction);

        assertEquals(StatusOfAuction.FINISHED, auction.getStatus());
    }

    @Test
    void testCancelAuction_Success() {
        Item item = mock(Item.class);
        Auction auction = new Auction(1, item, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        auction.setStatus(StatusOfAuction.OPEN);

        auctionService.cancelAuction(auction);

        assertEquals(StatusOfAuction.CANCELLED, auction.getStatus());
    }

    @Test
    void testPlaceBid_Success() {
        // 1. Chuẩn bị dữ liệu
        Item item = mock(Item.class);
        item.setCurHighest(100.0);
        Auction auction = new Auction(1, item, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        auction.setBidHistory(new ArrayList<>());
        UserInfo info = new UserInfo("email@test.com", "0123456", "Nguyen Van A");
        Bidder bidder = new Bidder(10, info, null);
        
        BidTransaction bid = new BidTransaction(001, bidder, 150.0, LocalDateTime.now(), "Test Bid");

        // 2. Giả lập hành vi của DAO: Trả về true khi update thành công
        when(auctionDAO.updateHighestBid(eq(auction.getId()), eq(bidder.getId()), eq(150.0)))
            .thenReturn(true);

        // 3. Thực thi
        boolean result = auctionService.placeBid(auction, bid);

        // 4. Kiểm chứng (Assert)
        assertTrue(result);
        assertEquals(150.0, auction.getItem().getCurHighest());
        assertEquals(bidder, auction.getHighestBidder());
        assertEquals(1, auction.getBidHistory().size());
        
        // Kiểm tra xem DAO có thực sự được gọi không
        verify(auctionDAO, times(1)).saveBidTransaction(bid, auction.getId());
    }

    @Test
    void testPlaceBid_Failure() {
        // Giả lập trường hợp giá đặt thấp hơn hoặc lỗi DB
        Item item = mock(Item.class);
        Auction auction = new Auction(1, item, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        UserInfo info = new UserInfo("email@test.com", "0123456", "Nguyen Van A");
        Bidder bidder = new Bidder(10, info, null);
        info.setName("Người Đấu Giá");
        
        BidTransaction bid = new BidTransaction(001, bidder, 50.0, LocalDateTime.now(), "Test Bid");

        // Giả lập DAO trả về false
        when(auctionDAO.updateHighestBid(anyInt(), anyInt(), anyDouble())).thenReturn(false);

        boolean result = auctionService.placeBid(auction, bid);

        assertFalse(result);
        // Đảm bảo không lưu giao dịch nếu update thất bại
        verify(auctionDAO, never()).saveBidTransaction(any(), anyInt());
    }
}    

=======
public class AuctionServiceTest {
>>>>>>> 3c805010ca5d364c348c4c8060518aefb2b2bf43

