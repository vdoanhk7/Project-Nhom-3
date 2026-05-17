-- Demo data for the online auction system.
-- Password for all seeded users is: 123456

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE auto_bids;
TRUNCATE TABLE bid_transactions;
TRUNCATE TABLE auctions;
TRUNCATE TABLE activity_logs;
TRUNCATE TABLE items;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO users (id, username, password, full_name, email, phone, role) VALUES
(1, 'admin01', '$2a$10$iQHpPnCg2QA1J5IL6jN0jO0y/vC3Y7rYxPOKD.W.y7fB2QaR4556C', 'Quan tri vien', 'admin@example.com', '0123456789', 'ADMIN'),
(2, 'seller01', '$2a$10$iQHpPnCg2QA1J5IL6jN0jO0y/vC3Y7rYxPOKD.W.y7fB2QaR4556C', 'Nguyen Van Ban', 'seller1@example.com', '0987654321', 'SELLER'),
(3, 'seller02', '$2a$10$iQHpPnCg2QA1J5IL6jN0jO0y/vC3Y7rYxPOKD.W.y7fB2QaR4556C', 'Tran Thi Hang', 'seller2@example.com', '0912345678', 'SELLER'),
(4, 'bidder01', '$2a$10$iQHpPnCg2QA1J5IL6jN0jO0y/vC3Y7rYxPOKD.W.y7fB2QaR4556C', 'Le Mua Nhieu', 'bidder1@example.com', '0901112223', 'BIDDER'),
(5, 'bidder02', '$2a$10$iQHpPnCg2QA1J5IL6jN0jO0y/vC3Y7rYxPOKD.W.y7fB2QaR4556C', 'Pham Tra Gia', 'bidder2@example.com', '0944455566', 'BIDDER');

INSERT INTO items (id, seller_id, name, description, start_price, cur_highest, item_type, image) VALUES
(1, 2, 'Buc tranh Hoa Huong Duong', 'Tranh son dau chu de hoa huong duong, phu hop trang tri phong khach.', 5000000, 5000000, 'ART', NULL),
(2, 2, 'Laptop Gaming Asus ROG', 'Laptop gaming cau hinh cao, phu hop cho do hoa va choi game.', 15000000, 16000000, 'ELECTRONICS', NULL),
(3, 3, 'Xe may Honda SH 150i', 'Xe may Honda SH 150i da qua su dung, giay to day du.', 40000000, 40000000, 'VEHICLE', NULL),
(4, 3, 'Dong ho thong minh Apple Watch', 'Apple Watch tinh trang tot, day sac kem theo.', 3000000, 3500000, 'ELECTRONICS', NULL),
(5, 2, 'Bo suu tap vat pham dac biet', 'Bo suu tap vat pham da dang danh cho nguoi yeu thich suu tam.', 2000000, 2000000, 'OTHER', NULL);

INSERT INTO auctions (id, item_id, start_time, end_time, bid_step, status, highest_bidder_id) VALUES
(1, 2, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 500000, 'FINISHED', 4),
(2, 4, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY), 250000, 'RUNNING', 5),
(3, 1, DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 5 DAY), 200000, 'OPEN', NULL),
(4, 3, DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 3 DAY), 1000000, 'RUNNING', NULL),
(5, 5, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 4 DAY), 100000, 'RUNNING', NULL);

INSERT INTO bid_transactions (auction_id, bidder_id, amount, bid_time, note) VALUES
(1, 5, 15500000, DATE_SUB(NOW(), INTERVAL 4 DAY), 'Auto bid'),
(1, 4, 16000000, DATE_SUB(NOW(), INTERVAL 3 DAY), 'Final winning bid'),
(2, 4, 3200000, DATE_SUB(NOW(), INTERVAL 10 HOUR), 'Opening bid'),
(2, 5, 3500000, DATE_SUB(NOW(), INTERVAL 5 HOUR), 'Higher bid');

INSERT INTO auto_bids (bidder_id, auction_id, max_amount, increment_amount) VALUES
(4, 3, 10000000, 500000);
