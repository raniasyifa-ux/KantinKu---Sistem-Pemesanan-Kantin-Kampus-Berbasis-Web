-- 000_create_base_schema.sql (versi 4)
-- Keadaan database SEBELUM migrasi 001-005.
-- Kolom role, aktif, tracking_token, username, nama_pembeli, metode_pembayaran,
-- tabel order_item, tabel riwayat, dan view akun_* dibuat oleh 001-005.
-- Import urut: 000 -> 001 -> 002 -> 003 -> 004 -> 005

CREATE DATABASE IF NOT EXISTS db_kantinku
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE db_kantinku;

CREATE TABLE IF NOT EXISTS users (
    id_user INT NOT NULL AUTO_INCREMENT,
    nama VARCHAR(100) DEFAULT NULL,
    email VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    PRIMARY KEY (id_user),
    UNIQUE KEY unique_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS menu (
    id_menu INT NOT NULL AUTO_INCREMENT,
    nama VARCHAR(100) NOT NULL,
    deskripsi TEXT,
    kategori VARCHAR(50) NOT NULL,
    harga DECIMAL(12,2) NOT NULL,
    gambar VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (id_menu)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS detail_pemesanan (
    id_pemesanan INT NOT NULL AUTO_INCREMENT,
    id_user INT DEFAULT NULL,
    tanggal_pemesanan DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(40) NOT NULL DEFAULT 'Menunggu konfirmasi',
    total_jumlah DECIMAL(12,2) NOT NULL DEFAULT 0,
    PRIMARY KEY (id_pemesanan),
    KEY idx_pemesanan_user (id_user),
    CONSTRAINT fk_pemesanan_user
        FOREIGN KEY (id_user) REFERENCES users (id_user)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS pembayaran (
    id_pembayaran INT NOT NULL AUTO_INCREMENT,
    id_pemesanan INT NOT NULL,
    metode VARCHAR(50) DEFAULT NULL,
    status_pembayaran VARCHAR(50) NOT NULL DEFAULT 'Menunggu verifikasi',
    tanggal_pembayaran DATETIME DEFAULT NULL,
    PRIMARY KEY (id_pembayaran),
    UNIQUE KEY unique_pembayaran_pemesanan (id_pemesanan),
    CONSTRAINT fk_pembayaran_pemesanan
        FOREIGN KEY (id_pemesanan) REFERENCES detail_pemesanan (id_pemesanan)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
