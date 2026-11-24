ALTER TABLE detail_pemesanan
    ADD COLUMN tracking_token CHAR(64) DEFAULT NULL AFTER total_jumlah,
    ADD UNIQUE KEY unique_tracking_token (tracking_token);
