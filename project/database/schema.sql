-- Canonical database schema for the online auction system.
-- Run this file before mock_data.sql when setting up a fresh database.

CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    profile_image MEDIUMTEXT NULL,
    reputation_score INT NOT NULL DEFAULT 100,
    role VARCHAR(20) NOT NULL COMMENT 'ADMIN, SELLER, BIDDER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    seller_id INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    start_price DOUBLE NOT NULL,
    cur_highest DOUBLE DEFAULT 0,
    item_type VARCHAR(50) NOT NULL COMMENT 'ART, ELECTRONICS, VEHICLE, OTHER',
    image MEDIUMTEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS auctions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    item_id INT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    bid_step DOUBLE NOT NULL DEFAULT 50000,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN, RUNNING, FINISHED, PAID, CANCELLED, DEAL_CANCELLED',
    highest_bidder_id INT,
    reputation_penalty INT NOT NULL DEFAULT 0,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE,
    FOREIGN KEY (highest_bidder_id) REFERENCES users(id) ON DELETE SET NULL
);

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

CREATE TABLE IF NOT EXISTS seller_ratings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    auction_id INT NOT NULL,
    buyer_id INT NOT NULL,
    seller_id INT NOT NULL,
    stars INT NOT NULL COMMENT '0..5',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_seller_rating_per_auction_buyer (auction_id, buyer_id),
    FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
    FOREIGN KEY (buyer_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS activity_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    action VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
