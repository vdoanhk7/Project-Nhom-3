-- Sử dụng Database Cloud mặc định

-- Xóa dữ liệu cũ nếu chạy lại nhiều lần (chỉ dùng cho môi trường test)
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE auto_bids;
TRUNCATE TABLE bid_transactions;
TRUNCATE TABLE auctions;
TRUNCATE TABLE items;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

-- Thêm Users (Mật khẩu đang để tạm là '123456', tùy thuộc hệ thống của bạn có băm mật khẩu hay không)
INSERT INTO users (id, username, password, full_name, email, phone, role) VALUES
(1, 'admin01', '123456', 'Quản trị viên', 'admin@example.com', '0123456789', 'ADMIN'),
(2, 'seller01', '123456', 'Nguyễn Văn Bán', 'seller1@example.com', '0987654321', 'SELLER'),
(3, 'seller02', '123456', 'Trần Thị Hàng', 'seller2@example.com', '0912345678', 'SELLER'),
(4, 'bidder01', '123456', 'Lê Mua Nhiều', 'bidder1@example.com', '0901112223', 'BIDDER'),
(5, 'bidder02', '123456', 'Phạm Trả Giá', 'bidder2@example.com', '0944455566', 'BIDDER');

-- Thêm Items (Sản phẩm)
INSERT INTO items (id, seller_id, name, start_price, cur_highest, item_type) VALUES
(1, 2, 'Bức tranh Hoa Hướng Dương', 5000000, 5000000, 'ART'),
(2, 2, 'Laptop Gaming Asus ROG', 15000000, 16000000, 'ELECTRONICS'),
(3, 3, 'Xe máy Honda SH 150i', 40000000, 40000000, 'VEHICLE'),
(4, 3, 'Đồng hồ thông minh Apple Watch', 3000000, 3500000, 'ELECTRONICS');

-- Thêm Auctions (Phiên đấu giá)
-- Phiên 1: Đã kết thúc (FINISHED)
INSERT INTO auctions (id, item_id, start_time, end_time, status, highest_bidder_id) VALUES
(1, 2, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 'FINISHED', 4);

-- Phiên 2: Đang diễn ra (RUNNING)
INSERT INTO auctions (id, item_id, start_time, end_time, status, highest_bidder_id) VALUES
(2, 4, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY), 'RUNNING', 5);

-- Phiên 3: Chưa bắt đầu (OPEN)
INSERT INTO auctions (id, item_id, start_time, end_time, status, highest_bidder_id) VALUES
(3, 1, DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 5 DAY), 'OPEN', NULL);

-- Phiên 4: Mới mở, đang chạy (RUNNING) - chưa có ai trả giá
INSERT INTO auctions (id, item_id, start_time, end_time, status, highest_bidder_id) VALUES
(4, 3, DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 3 DAY), 'RUNNING', NULL);

-- Thêm Lịch sử trả giá (Bid Transactions)
-- Cho Laptop Gaming (Auction 1 - FINISHED)
INSERT INTO bid_transactions (auction_id, bidder_id, amount, bid_time, note) VALUES
(1, 5, 15500000, DATE_SUB(NOW(), INTERVAL 4 DAY), 'Trả giá tự động'),
(1, 4, 16000000, DATE_SUB(NOW(), INTERVAL 3 DAY), 'Quyết tâm mua');

-- Cho Apple Watch (Auction 2 - RUNNING)
INSERT INTO bid_transactions (auction_id, bidder_id, amount, bid_time, note) VALUES
(2, 4, 3200000, DATE_SUB(NOW(), INTERVAL 10 HOUR), 'Trả giá mồi'),
(2, 5, 3500000, DATE_SUB(NOW(), INTERVAL 5 HOUR), 'Vượt mặt');

-- Thêm Cấu hình Auto Bid (Tự động trả giá)
INSERT INTO auto_bids (bidder_id, auction_id, max_amount, increment_amount) VALUES
(4, 3, 10000000, 500000);
