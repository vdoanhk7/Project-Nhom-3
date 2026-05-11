package com.nhom3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.nhom3.server.dao.AuctionDAO;
import com.nhom3.server.dao.ItemDAO;
import com.nhom3.server.service.ItemService;
import com.nhom3.shared.model.auction.Auction;
import com.nhom3.shared.model.item.Item;
import com.nhom3.shared.model.user.Seller;

public class ItemServiceTest {

    @Mock
    private ItemDAO itemDAO;

    @Mock
    private AuctionDAO auctionDAO;

    private ItemService itemService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        itemService = new ItemService(itemDAO, auctionDAO);
    }

    // --- Tests for createItem ---

    @Test
    void createItem_WithSellerNull_ShouldThrowIllegalArgumentException() {
        Seller seller = null;
        Item item = mock(Item.class);
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            itemService.createItem(seller, item);
        });
        assertEquals("Lỗi xác thực: Không tìm thấy thông tin người bán!", exception.getMessage());
    }

    @Test
    void createItem_WithSellerIdZero_ShouldThrowIllegalArgumentException() {
        Seller seller = mock(Seller.class);
        when(seller.getId()).thenReturn(0);
        Item item = mock(Item.class);
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            itemService.createItem(seller, item);
        });
        assertEquals("Lỗi xác thực: Không tìm thấy thông tin người bán!", exception.getMessage());
    }

    @Test
    void createItem_WithItemNull_ShouldThrowIllegalArgumentException() {
        Seller seller = mock(Seller.class);
        when(seller.getId()).thenReturn(1);
        Item item = null;
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            itemService.createItem(seller, item);
        });
        assertEquals("Tên sản phẩm không được để trống!", exception.getMessage());
    }

    @Test
    void createItem_WithItemNameNull_ShouldThrowIllegalArgumentException() {
        Seller seller = mock(Seller.class);
        when(seller.getId()).thenReturn(1);
        Item item = mock(Item.class);
        when(item.getName()).thenReturn(null);
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            itemService.createItem(seller, item);
        });
        assertEquals("Tên sản phẩm không được để trống!", exception.getMessage());
    }

    @Test
    void createItem_WithItemNameEmpty_ShouldThrowIllegalArgumentException() {
        Seller seller = mock(Seller.class);
        when(seller.getId()).thenReturn(1);
        Item item = mock(Item.class);
        when(item.getName()).thenReturn("   ");
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            itemService.createItem(seller, item);
        });
        assertEquals("Tên sản phẩm không được để trống!", exception.getMessage());
    }

    @Test
    void createItem_WithInvalidItemStartPrice_ShouldThrowIllegalArgumentException() {
        Seller seller = mock(Seller.class);
        when(seller.getId()).thenReturn(1);
        Item item = mock(Item.class);
        when(item.getName()).thenReturn("Valid Name");
        when(item.getStartPrice()).thenReturn(-1.0);
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            itemService.createItem(seller, item);
        });
        assertEquals("Giá khởi điểm không hợp lệ!", exception.getMessage());
    }

    @Test
    void createItem_DBFailure_ShouldReturnFalse() {
        Seller seller = mock(Seller.class);
        when(seller.getId()).thenReturn(1);
        Item item = mock(Item.class);
        when(item.getName()).thenReturn("Valid Name");
        when(item.getStartPrice()).thenReturn(100.0);
        
        when(itemDAO.saveItem(item, 1)).thenReturn(false);
        
        boolean result = itemService.createItem(seller, item);
        assertFalse(result);
        verify(itemDAO).saveItem(item, 1);
    }

    @Test
    void createItem_Success_ShouldReturnTrue() {
        Seller seller = mock(Seller.class);
        when(seller.getId()).thenReturn(1);
        Item item = mock(Item.class);
        when(item.getName()).thenReturn("Valid Name");
        when(item.getStartPrice()).thenReturn(100.0);
        
        when(itemDAO.saveItem(item, 1)).thenReturn(true);
        
        boolean result = itemService.createItem(seller, item);
        assertTrue(result);
        verify(itemDAO).saveItem(item, 1);
    }

    // --- Tests for removeItem ---

    @Test
    void removeItem_ItemInAuction_ShouldThrowIllegalStateException() {
        int sellerId = 1;
        int itemId = 100;
        Auction mockAuction = mock(Auction.class);
        when(auctionDAO.getAuctionByItemId(itemId)).thenReturn(mockAuction);
        
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            itemService.removeItem(sellerId, itemId);
        });
        
        assertEquals("Không thể xóa! Sản phẩm này đã được đưa lên sàn đấu giá. Vui lòng chỉ ẩn hoặc hủy phiên đấu giá.", exception.getMessage());
        verify(auctionDAO).getAuctionByItemId(itemId);
    }

    @Test
    void removeItem_Failure_BecauseItemNotFoundOrNotOwned_ShouldThrowIllegalStateException() {
        int sellerId = 1;
        int itemId = 100;
        when(auctionDAO.getAuctionByItemId(itemId)).thenReturn(null);
        when(itemDAO.deleteItem(itemId, sellerId)).thenReturn(false);
        
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            itemService.removeItem(sellerId, itemId);
        });
        
        assertEquals("Xóa thất bại! Sản phẩm không tồn tại hoặc bạn không có quyền xóa sản phẩm này.", exception.getMessage());
        verify(auctionDAO).getAuctionByItemId(itemId);
        verify(itemDAO).deleteItem(itemId, sellerId);
    }

    @Test
    void removeItem_Success_ShouldReturnTrue() {
        int sellerId = 1;
        int itemId = 100;
        when(auctionDAO.getAuctionByItemId(itemId)).thenReturn(null);
        when(itemDAO.deleteItem(itemId, sellerId)).thenReturn(true);
        
        boolean result = itemService.removeItem(sellerId, itemId);
        
        assertTrue(result);
        verify(auctionDAO).getAuctionByItemId(itemId);
        verify(itemDAO).deleteItem(itemId, sellerId);
    }
}
