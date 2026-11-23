CREATE TABLE IF NOT EXISTS riwayat_status_pesanan (
    id_riwayat INT NOT NULL AUTO_INCREMENT,
    id_pemesanan INT NOT NULL,
    status VARCHAR(40) NOT NULL,
    dibuat_pada DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_riwayat),
    KEY idx_riwayat_pesanan (id_pemesanan, dibuat_pada),
    CONSTRAINT fk_riwayat_pesanan
        FOREIGN KEY (id_pemesanan) REFERENCES detail_pemesanan (id_pemesanan)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS riwayat_status_pembayaran (
    id_riwayat INT NOT NULL AUTO_INCREMENT,
    id_pemesanan INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    dibuat_pada DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_riwayat),
    KEY idx_riwayat_pembayaran (id_pemesanan, dibuat_pada),
    CONSTRAINT fk_riwayat_pembayaran
        FOREIGN KEY (id_pemesanan) REFERENCES detail_pemesanan (id_pemesanan)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO riwayat_status_pesanan (id_pemesanan, status, dibuat_pada)
SELECT dp.id_pemesanan, dp.status, dp.tanggal_pemesanan
FROM detail_pemesanan dp
WHERE NOT EXISTS (
    SELECT 1
    FROM riwayat_status_pesanan history
    WHERE history.id_pemesanan = dp.id_pemesanan
);

INSERT INTO riwayat_status_pembayaran (id_pemesanan, status, dibuat_pada)
SELECT p.id_pemesanan, p.status_pembayaran, COALESCE(p.tanggal_pembayaran, dp.tanggal_pemesanan)
FROM pembayaran p
JOIN detail_pemesanan dp ON dp.id_pemesanan = p.id_pemesanan
WHERE NOT EXISTS (
    SELECT 1
    FROM riwayat_status_pembayaran history
    WHERE history.id_pemesanan = p.id_pemesanan
);
