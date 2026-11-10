ALTER TABLE detail_pemesanan
    ADD COLUMN no_hp_pembeli VARCHAR(20) NULL AFTER nama_pembeli;

ALTER TABLE menu
    ADD COLUMN stok INT NOT NULL DEFAULT 0 AFTER aktif;
