-- Tạo Database trên Cloud (bỏ qua lệnh tạo vì Cloud đã tạo sẵn defaultdb)

-- 1. Bảng users
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    profile_image MEDIUMTEXT NULL,
    role VARCHAR(20) NOT NULL COMMENT 'Các quyền: ADMIN, SELLER, BIDDER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Bảng items
CREATE TABLE IF NOT EXISTS items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    seller_id INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    start_price DOUBLE NOT NULL,
    cur_highest DOUBLE DEFAULT 0,
    item_type VARCHAR(50) NOT NULL COMMENT 'Các loại: ART, ELECTRONICS, VEHICLE...',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 3. Bảng auctions
CREATE TABLE IF NOT EXISTS auctions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    item_id INT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    bid_step DOUBLE NOT NULL DEFAULT 50000,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN, RUNNING, FINISHED, PAID, CANCELLED',
    highest_bidder_id INT,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE,
    FOREIGN KEY (highest_bidder_id) REFERENCES users(id) ON DELETE SET NULL
);

-- 4. Bảng bid_transactions
CREATE TABLE IF NOT EXISTS bid_transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    auction_id INT NOT NULL,
    bidder_id INT NOT NULL,
    amount DOUBLE NOT NULL,
    bid_time DATETIME NOT NULL,
    note VARCHAR(255),
    FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
    FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 5. Bảng auto_bids
CREATE TABLE IF NOT EXISTS auto_bids (
    id INT AUTO_INCREMENT PRIMARY KEY,
    bidder_id INT NOT NULL,
    auction_id INT NOT NULL,
    max_amount DOUBLE NOT NULL,
    increment_amount DOUBLE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
    UNIQUE KEY unique_auto_bid (bidder_id, auction_id) 
);

-- 6. Bảng activity_logs
CREATE TABLE IF NOT EXISTS activity_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    action VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
