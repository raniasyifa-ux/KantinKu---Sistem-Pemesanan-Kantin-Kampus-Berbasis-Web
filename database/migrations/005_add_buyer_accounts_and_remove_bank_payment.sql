USE db_kantinku;

ALTER TABLE users
    ADD COLUMN username VARCHAR(50)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL AFTER nama;

UPDATE users
SET username = CONCAT('user', id_user)
WHERE username IS NULL OR username = '';

ALTER TABLE users
    MODIFY COLUMN username VARCHAR(50) NOT NULL,
    ADD UNIQUE KEY unique_username (username);

ALTER TABLE detail_pemesanan
    ADD COLUMN nama_pembeli VARCHAR(100) NOT NULL DEFAULT '' AFTER id_user;

ALTER TABLE pembayaran
    CHANGE COLUMN metode metode_pembayaran VARCHAR(50) DEFAULT NULL;

CREATE TABLE IF NOT EXISTS order_item (
    id_order_item INT NOT NULL AUTO_INCREMENT,
    id_pemesanan INT NOT NULL,
    menu_id INT NOT NULL,
    jumlah INT NOT NULL,
    harga_satuan DECIMAL(12,2) NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,
    PRIMARY KEY (id_order_item),
    KEY idx_order_item_pemesanan (id_pemesanan),
    CONSTRAINT fk_order_item_pemesanan
        FOREIGN KEY (id_pemesanan) REFERENCES detail_pemesanan (id_pemesanan)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Pesanan lama yang pernah memakai bank tetap dapat dibaca sebagai QRIS,
-- tanpa menyisakan metode bank pada data aktif.
UPDATE pembayaran
SET metode_pembayaran = 'QRIS'
WHERE metode_pembayaran LIKE 'Transfer Bank%';

CREATE OR REPLACE VIEW akun_pembeli AS
SELECT id_user, nama, username, email, password, role
FROM users
WHERE role = 'pembeli';

CREATE OR REPLACE VIEW akun_penjual AS
SELECT id_user, nama, username, email, password, role
FROM users
WHERE role = 'penjual';
