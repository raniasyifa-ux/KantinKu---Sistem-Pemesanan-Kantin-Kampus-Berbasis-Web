USE db_kantinku;
ALTER TABLE users
    ADD COLUMN role ENUM('pembeli', 'penjual') NOT NULL DEFAULT 'pembeli' AFTER password;

ALTER TABLE menu
    ADD COLUMN aktif TINYINT(1) NOT NULL DEFAULT 1 AFTER gambar;
