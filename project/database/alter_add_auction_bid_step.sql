SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE auctions ADD COLUMN bid_step DOUBLE NOT NULL DEFAULT 50000 AFTER end_time',
        'SELECT ''bid_step already exists'' AS message'
    )
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'auctions'
      AND COLUMN_NAME = 'bid_step'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
