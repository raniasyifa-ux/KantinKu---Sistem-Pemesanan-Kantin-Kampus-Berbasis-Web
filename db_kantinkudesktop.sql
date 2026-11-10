-- phpMyAdmin SQL Dump
-- version 5.2.0
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Nov 10, 2026 at 11:32 AM
-- Server version: 10.4.24-MariaDB
-- PHP Version: 8.1.6

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `db_kantinkudesktop`
--

-- --------------------------------------------------------

--
-- Table structure for table `detail_pemesanan`
--

CREATE TABLE `detail_pemesanan` (
  `id_pemesanan` int(11) NOT NULL,
  `nama_pembeli` varchar(100) NOT NULL,
  `no_hp_pembeli` varchar(20) DEFAULT NULL,
  `tanggal_pemesanan` datetime NOT NULL DEFAULT current_timestamp(),
  `status` varchar(50) NOT NULL DEFAULT 'Menunggu konfirmasi',
  `total_jumlah` decimal(12,2) NOT NULL DEFAULT 0.00,
  `tracking_token` varchar(64) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Dumping data for table `detail_pemesanan`
--

INSERT INTO `detail_pemesanan` (`id_pemesanan`, `nama_pembeli`, `no_hp_pembeli`, `tanggal_pemesanan`, `status`, `total_jumlah`, `tracking_token`) VALUES
(40, 'rania', '088271760826', '2026-11-10 17:24:00', 'Menunggu konfirmasi', '12000.00', '778121d5da1e4378905388fecadb131e762239cdf82942a790f6b2f6f93f3f3b'),
(41, 'Gledis', '08818273910', '2026-11-10 17:31:00', 'Menunggu konfirmasi', '26000.00', '967793d691a54c98a01450c89919d85f73b82df58a3d4b5e90e8ef20d7204b23');

-- --------------------------------------------------------

--
-- Table structure for table `menu`
--

CREATE TABLE `menu` (
  `id_menu` int(11) NOT NULL,
  `nama` varchar(100) NOT NULL,
  `deskripsi` text DEFAULT NULL,
  `kategori` varchar(50) NOT NULL,
  `harga` decimal(10,2) NOT NULL,
  `gambar` varchar(255) DEFAULT NULL,
  `aktif` tinyint(1) NOT NULL DEFAULT 1,
  `stok` int(11) NOT NULL DEFAULT 20
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Dumping data for table `menu`
--

INSERT INTO `menu` (`id_menu`, `nama`, `deskripsi`, `kategori`, `harga`, `gambar`, `aktif`, `stok`) VALUES
(6, 'Risol Mayo', 'Risol renyah berisi mayo dan smoked beef.', 'Makanan', '12000.00', 'RISOL MAYO.jpeg', 1, 19),
(7, 'Es Teh', 'Es teh manis yang segar.', 'Minuman', '5000.00', 'ES TEH.jpeg', 1, 20),
(9, 'Milky Regal', 'Susu creamy dengan biskuit regal.', 'Minuman', '12000.00', 'REGAL.jpeg', 1, 20),
(12, 'Nasi Goreng', 'Nasi goreng gurih dengan telur dan ayam.', 'Makanan', '15000.00', 'NASI GORENG.jpeg', 1, 20),
(13, 'Mie Ayam', 'Mie lembut dengan ayam gurih dan kuah.', 'Makanan', '14000.00', 'MIE AYAM.jpeg', 1, 20),
(14, 'Pempek', 'Pempek ikan dengan kuah cuko khas.', 'Makanan', '15000.00', 'PEMPEK.jpeg', 1, 20),
(15, 'Siomay', 'Siomay lengkap dengan saus kacang.', 'Makanan', '13000.00', 'SIOMAY.jpeg', 1, 20),
(16, 'Batagor', 'Batagor crispy dengan saus kacang.', 'Makanan', '13000.00', 'BATAGOR.jpeg', 1, 20),
(17, 'Ayam Geprek', 'Ayam crispy dengan sambal pedas.', 'Makanan', '18000.00', 'GEPREK.jpeg', 1, 20),
(18, 'Soto Ayam', 'Soto ayam hangat dengan kuah gurih.', 'Makanan', '15000.00', 'SOTO.jpeg', 1, 20),
(19, 'Nasi Ayam Bakar', 'Nasi dengan ayam bakar berbumbu khas.', 'Makanan', '20000.00', 'AYAM BAKAR.jpeg', 1, 20),
(20, 'Kentang Goreng', 'Kentang goreng renyah dengan saus.', 'Makanan', '10000.00', 'KENTANG GORENG.jpeg', 1, 20),
(21, 'Es Jeruk', 'Perasan jeruk segar dengan es.', 'Minuman', '7000.00', 'ES JERUK.jpeg', 1, 20),
(22, 'Es Milo', 'Milo dingin dengan rasa cokelat.', 'Minuman', '9000.00', 'milo.jpeg', 1, 20),
(23, 'Es Kopi Susu', 'Kopi susu dingin dengan rasa creamy.', 'Minuman', '12000.00', 'KOPI SUSU.jpeg', 1, 20),
(24, 'Thai Tea', 'Thai tea manis dan creamy.', 'Minuman', '10000.00', 'THAI TEA.jpeg', 1, 20),
(25, 'Matcha Latte', 'Matcha lembut dengan susu.', 'Minuman', '13000.00', 'MATCHA.jpeg', 1, 20),
(26, 'Cappuccino', 'Kopi cappuccino dengan foam lembut.', 'Minuman', '14000.00', 'CAPPUCINO.jpeg', 1, 19),
(27, 'Chocolate', 'Minuman cokelat manis dan creamy.', 'Minuman', '11000.00', 'COKLAT.jpeg', 1, 20),
(28, 'Jus Alpukat', 'Jus alpukat segar dan lembut.', 'Minuman', '12000.00', 'ALPUKAT.jpeg', 1, 19);

-- --------------------------------------------------------

--
-- Table structure for table `order_item`
--

CREATE TABLE `order_item` (
  `id_order_item` int(11) NOT NULL,
  `id_pemesanan` int(11) NOT NULL,
  `menu_id` int(11) NOT NULL,
  `jumlah` int(11) NOT NULL,
  `harga_satuan` decimal(10,2) NOT NULL,
  `subtotal` decimal(12,2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Dumping data for table `order_item`
--

INSERT INTO `order_item` (`id_order_item`, `id_pemesanan`, `menu_id`, `jumlah`, `harga_satuan`, `subtotal`) VALUES
(52, 40, 6, 1, '12000.00', '12000.00'),
(53, 41, 26, 1, '14000.00', '14000.00'),
(54, 41, 28, 1, '12000.00', '12000.00');

-- --------------------------------------------------------

--
-- Table structure for table `pembayaran`
--

CREATE TABLE `pembayaran` (
  `id_pembayaran` int(11) NOT NULL,
  `id_pemesanan` int(11) NOT NULL,
  `metode_pembayaran` varchar(50) NOT NULL,
  `status_pembayaran` varchar(50) NOT NULL DEFAULT 'Menunggu verifikasi',
  `tanggal_pembayaran` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Dumping data for table `pembayaran`
--

INSERT INTO `pembayaran` (`id_pembayaran`, `id_pemesanan`, `metode_pembayaran`, `status_pembayaran`, `tanggal_pembayaran`) VALUES
(40, 40, 'QRIS', 'Menunggu verifikasi', '2026-11-10 10:24:19'),
(41, 41, 'Tunai / COD', 'Menunggu pembayaran di tempat', '2026-11-10 10:31:41');

-- --------------------------------------------------------

--
-- Table structure for table `riwayat_status_pembayaran`
--

CREATE TABLE `riwayat_status_pembayaran` (
  `id_riwayat` int(11) NOT NULL,
  `id_pemesanan` int(11) NOT NULL,
  `status` varchar(50) NOT NULL,
  `dibuat_pada` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Dumping data for table `riwayat_status_pembayaran`
--

INSERT INTO `riwayat_status_pembayaran` (`id_riwayat`, `id_pemesanan`, `status`, `dibuat_pada`) VALUES
(36, 40, 'Menunggu verifikasi', '2026-11-10 10:24:19'),
(37, 41, 'Menunggu pembayaran di tempat', '2026-11-10 10:31:41');

-- --------------------------------------------------------

--
-- Table structure for table `riwayat_status_pesanan`
--

CREATE TABLE `riwayat_status_pesanan` (
  `id_riwayat` int(11) NOT NULL,
  `id_pemesanan` int(11) NOT NULL,
  `status` varchar(50) NOT NULL,
  `dibuat_pada` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Dumping data for table `riwayat_status_pesanan`
--

INSERT INTO `riwayat_status_pesanan` (`id_riwayat`, `id_pemesanan`, `status`, `dibuat_pada`) VALUES
(57, 40, 'Menunggu konfirmasi', '2026-11-10 10:24:19'),
(58, 41, 'Menunggu konfirmasi', '2026-11-10 10:31:41');

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id_user` int(11) NOT NULL,
  `username` varchar(50) NOT NULL,
  `email` varchar(100) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` varchar(20) NOT NULL DEFAULT 'penjual',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`id_user`, `username`, `email`, `password`, `role`, `created_at`) VALUES
(13, 'raniapku0@gmail.com', 'ransyff', '$2y$10$cPRobGj/z3ZfubJLBr1vTOE0CQWAtnTXY/.6/6hcNeS98IHEslhqG', 'penjual', '2026-10-22 16:49:52'),
(16, 'kantin@gmail.com', 'tokoKami', '$2y$10$mTddxAnmzfCHUirXIW2Mf.uP47iEgdLj2HFHD.4Z3NCd4hv.3z3Ei', 'penjual', '2026-11-06 01:07:54'),
(17, 'eng@gmail.com', 'tokoEng', '$2y$10$4xg6s65ZSm7IWWHb2JIJZeq0jhfvPIcLB9uUWM/W4TBd.eeDUbKl6', 'penjual', '2026-11-06 01:51:56');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `detail_pemesanan`
--
ALTER TABLE `detail_pemesanan`
  ADD PRIMARY KEY (`id_pemesanan`),
  ADD KEY `idx_tanggal` (`tanggal_pemesanan`),
  ADD KEY `idx_status` (`status`);

--
-- Indexes for table `menu`
--
ALTER TABLE `menu`
  ADD PRIMARY KEY (`id_menu`);

--
-- Indexes for table `order_item`
--
ALTER TABLE `order_item`
  ADD PRIMARY KEY (`id_order_item`),
  ADD KEY `fk_order_item_pemesanan` (`id_pemesanan`),
  ADD KEY `fk_order_item_menu` (`menu_id`);

--
-- Indexes for table `pembayaran`
--
ALTER TABLE `pembayaran`
  ADD PRIMARY KEY (`id_pembayaran`),
  ADD UNIQUE KEY `id_pemesanan` (`id_pemesanan`);

--
-- Indexes for table `riwayat_status_pembayaran`
--
ALTER TABLE `riwayat_status_pembayaran`
  ADD PRIMARY KEY (`id_riwayat`),
  ADD KEY `fk_riwayat_pembayaran` (`id_pemesanan`);

--
-- Indexes for table `riwayat_status_pesanan`
--
ALTER TABLE `riwayat_status_pesanan`
  ADD PRIMARY KEY (`id_riwayat`),
  ADD KEY `fk_riwayat_pesanan` (`id_pemesanan`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id_user`),
  ADD UNIQUE KEY `username` (`username`),
  ADD UNIQUE KEY `email` (`email`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `detail_pemesanan`
--
ALTER TABLE `detail_pemesanan`
  MODIFY `id_pemesanan` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=42;

--
-- AUTO_INCREMENT for table `menu`
--
ALTER TABLE `menu`
  MODIFY `id_menu` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=30;

--
-- AUTO_INCREMENT for table `order_item`
--
ALTER TABLE `order_item`
  MODIFY `id_order_item` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=55;

--
-- AUTO_INCREMENT for table `pembayaran`
--
ALTER TABLE `pembayaran`
  MODIFY `id_pembayaran` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=42;

--
-- AUTO_INCREMENT for table `riwayat_status_pembayaran`
--
ALTER TABLE `riwayat_status_pembayaran`
  MODIFY `id_riwayat` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=38;

--
-- AUTO_INCREMENT for table `riwayat_status_pesanan`
--
ALTER TABLE `riwayat_status_pesanan`
  MODIFY `id_riwayat` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=59;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `id_user` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=19;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `order_item`
--
ALTER TABLE `order_item`
  ADD CONSTRAINT `fk_order_item_menu` FOREIGN KEY (`menu_id`) REFERENCES `menu` (`id_menu`) ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_order_item_pemesanan` FOREIGN KEY (`id_pemesanan`) REFERENCES `detail_pemesanan` (`id_pemesanan`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `pembayaran`
--
ALTER TABLE `pembayaran`
  ADD CONSTRAINT `fk_pembayaran_pemesanan` FOREIGN KEY (`id_pemesanan`) REFERENCES `detail_pemesanan` (`id_pemesanan`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `riwayat_status_pembayaran`
--
ALTER TABLE `riwayat_status_pembayaran`
  ADD CONSTRAINT `fk_riwayat_pembayaran` FOREIGN KEY (`id_pemesanan`) REFERENCES `detail_pemesanan` (`id_pemesanan`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `riwayat_status_pesanan`
--
ALTER TABLE `riwayat_status_pesanan`
  ADD CONSTRAINT `fk_riwayat_pesanan` FOREIGN KEY (`id_pemesanan`) REFERENCES `detail_pemesanan` (`id_pemesanan`) ON DELETE CASCADE ON UPDATE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
