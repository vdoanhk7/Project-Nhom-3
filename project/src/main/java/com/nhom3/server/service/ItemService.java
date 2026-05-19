package com.nhom3.server.service;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.nhom3.server.dao.AuctionDAO;
import com.nhom3.server.dao.AuctionDAOImpl;
import com.nhom3.server.dao.ItemDAO;
import com.nhom3.server.dao.ItemDAOImpl;
import com.nhom3.server.exception.InvalidItemException;
import com.nhom3.server.exception.ItemOperationException;
import com.nhom3.shared.model.auction.Auction;
import com.nhom3.shared.model.item.Item;
import com.nhom3.shared.model.user.Seller;

public class ItemService {
    private static final Logger log = LoggerFactory.getLogger(ItemService.class);
    private final ItemDAO itemDAO;
    private final AuctionDAO auctionDAO;

    public ItemService() {
        this.itemDAO = new ItemDAOImpl();
        this.auctionDAO = new AuctionDAOImpl();
    }

    public ItemService(ItemDAO itemDAO, AuctionDAO auctionDAO) {
        this.itemDAO = itemDAO;
        this.auctionDAO = auctionDAO;
    }

    public boolean createItem(Seller seller, Item newItem) throws InvalidItemException {
        log.info("Bắt đầu xử lý yêu cầu thêm sản phẩm mới từ Seller ID: {}", seller != null ? seller.getId() : "null");

        if (seller == null || seller.getId() <= 0) {
            log.warn("Thêm sản phẩm thất bại: Thông tin Seller không hợp lệ.");
            throw new InvalidItemException("ITEM_SELLER_INVALID", "Lỗi xác thực: Không tìm thấy thông tin người bán!");
        }
        if (newItem == null || newItem.getName() == null || newItem.getName().trim().isEmpty()) {
            log.warn("Thêm sản phẩm thất bại: Tên sản phẩm trống.");
            throw new InvalidItemException("ITEM_NAME_REQUIRED", "Tên sản phẩm không được để trống!");
        }
        if (newItem.getStartPrice() < 0) {
            log.warn("Thêm sản phẩm thất bại: Giá khởi điểm âm ({}).", newItem.getStartPrice());
            throw new InvalidItemException("ITEM_START_PRICE_INVALID", "Giá khởi điểm không hợp lệ!");
        }

        boolean isSuccess = itemDAO.saveItem(newItem, seller.getId());

        if (isSuccess) {
            log.info("Đã lưu thành công sản phẩm '{}' cho Seller ID: {}", newItem.getName(), seller.getId());
            log.info("[OOP printInfo] Created item detail: {}", newItem.printInfo());
            return true;
        } else {
            log.error("Lỗi Database khi lưu sản phẩm '{}'", newItem.getName());
            return false;
        }
    }

    public boolean updateItem(Item item) throws InvalidItemException {
        if (item == null || item.getName() == null || item.getName().trim().isEmpty()) {
            log.warn("Cập nhật thất bại: Tên sản phẩm trống.");
            throw new InvalidItemException("ITEM_NAME_REQUIRED", "Tên sản phẩm không được để trống!");
        }
        log.info("Bắt đầu xử lý yêu cầu cập nhật sản phẩm ID: {}", item.getId());
        if (item.getStartPrice() < 0) {
            log.warn("Cập nhật thất bại: Giá khởi điểm âm ({}).", item.getStartPrice());
            throw new InvalidItemException("ITEM_START_PRICE_INVALID", "Giá khởi điểm không hợp lệ!");
        }

        boolean isSuccess = itemDAO.updateItem(item);

        if (isSuccess) {
            log.info("Đã cập nhật thành công sản phẩm ID: {}", item.getId());
            log.info("[OOP printInfo] Updated item detail: {}", item.printInfo());
            return true;
        } else {
            log.error("Lỗi Database khi cập nhật sản phẩm ID: {}", item.getId());
            return false;
        }
    }

    public boolean removeItem(int sellerId, int itemId) throws ItemOperationException {
        log.info("Bắt đầu xử lý yêu cầu xóa sản phẩm ID: {} từ Seller ID: {}", itemId, sellerId);

        // Nếu đã có phiên đấu giá (dù là đang chạy hay đã kết thúc), TUYỆT ĐỐI KHÔNG
        // ĐƯỢC XÓA
        // vì sẽ làm mất lịch sử giao dịch và gây lỗi Khóa ngoại trong DB.
        Auction existingAuction = auctionDAO.getAuctionByItemId(itemId);
        if (existingAuction != null) {
            log.warn("Từ chối xóa: Sản phẩm ID {} đã được liên kết với phiên đấu giá ID {}.", itemId,
                    existingAuction.getId());
            throw new ItemOperationException(
                    "ITEM_LINKED_TO_AUCTION",
                    "Không thể xóa! Sản phẩm này đã được đưa lên sàn đấu giá. Vui lòng chỉ ẩn hoặc hủy phiên đấu giá.");
        }

        boolean isSuccess = itemDAO.deleteItem(itemId, sellerId);

        if (isSuccess) {
            log.info("Đã xóa vĩnh viễn sản phẩm ID: {}", itemId);
            return true;
        } else {
            log.warn("Xóa thất bại: Sản phẩm ID {} không tồn tại hoặc Seller ID {} không có quyền sở hữu.", itemId,
                    sellerId);
            throw new ItemOperationException(
                    "ITEM_DELETE_NOT_ALLOWED",
                    "Xóa thất bại! Sản phẩm không tồn tại hoặc bạn không có quyền xóa sản phẩm này.");
        }
    }
}
