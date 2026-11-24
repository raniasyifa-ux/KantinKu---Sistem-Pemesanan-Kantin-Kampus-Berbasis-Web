INSERT INTO menu (nama, deskripsi, kategori, harga, gambar, aktif)
SELECT catalog.nama, catalog.deskripsi, catalog.kategori, catalog.harga, catalog.gambar, 1
FROM (
    SELECT 'Nasi Goreng' AS nama, 'Nasi goreng gurih dengan telur dan ayam.' AS deskripsi, 'Makanan' AS kategori, 15000 AS harga, 'NASI GORENG.jpeg' AS gambar
    UNION ALL SELECT 'Mie Ayam', 'Mie lembut dengan ayam gurih dan kuah.', 'Makanan', 14000, 'MIE AYAM.jpeg'
    UNION ALL SELECT 'Pempek', 'Pempek ikan dengan kuah cuko khas.', 'Makanan', 15000, 'PEMPEK.jpeg'
    UNION ALL SELECT 'Siomay', 'Siomay lengkap dengan saus kacang.', 'Makanan', 13000, 'SIOMAY.jpeg'
    UNION ALL SELECT 'Batagor', 'Batagor crispy dengan saus kacang.', 'Makanan', 13000, 'BATAGOR.jpeg'
    UNION ALL SELECT 'Ayam Geprek', 'Ayam crispy dengan sambal pedas.', 'Makanan', 18000, 'GEPREK.jpeg'
    UNION ALL SELECT 'Soto Ayam', 'Soto ayam hangat dengan kuah gurih.', 'Makanan', 15000, 'SOTO.jpeg'
    UNION ALL SELECT 'Nasi Ayam Bakar', 'Nasi dengan ayam bakar berbumbu khas.', 'Makanan', 20000, 'AYAM BAKAR.jpeg'
    UNION ALL SELECT 'Kentang Goreng', 'Kentang goreng renyah dengan saus.', 'Makanan', 10000, 'KENTANG GORENG.jpeg'
    UNION ALL SELECT 'Es Jeruk', 'Perasan jeruk segar dengan es.', 'Minuman', 7000, 'ES JERUK.jpeg'
    UNION ALL SELECT 'Es Milo', 'Milo dingin dengan rasa cokelat.', 'Minuman', 9000, 'milo.jpeg'
    UNION ALL SELECT 'Es Kopi Susu', 'Kopi susu dingin dengan rasa creamy.', 'Minuman', 12000, 'KOPI SUSU.jpeg'
    UNION ALL SELECT 'Thai Tea', 'Thai tea manis dan creamy.', 'Minuman', 10000, 'THAI TEA.jpeg'
    UNION ALL SELECT 'Matcha Latte', 'Matcha lembut dengan susu.', 'Minuman', 13000, 'MATCHA.jpeg'
    UNION ALL SELECT 'Cappuccino', 'Kopi cappuccino dengan foam lembut.', 'Minuman', 14000, 'CAPPUCINO.jpeg'
    UNION ALL SELECT 'Chocolate', 'Minuman cokelat manis dan creamy.', 'Minuman', 11000, 'COKLAT.jpeg'
    UNION ALL SELECT 'Jus Alpukat', 'Jus alpukat segar dan lembut.', 'Minuman', 12000, 'ALPUKAT.jpeg'
) AS catalog
LEFT JOIN menu existing ON LOWER(existing.nama) = LOWER(catalog.nama)
WHERE existing.id_menu IS NULL;
