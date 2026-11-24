<?php
header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store, no-cache, must-revalidate, max-age=0');
session_start();

require_once __DIR__ . '/../koneksi.php';

mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

function responseJson(int $status, array $data): void
{
    http_response_code($status);
    echo json_encode($data, JSON_UNESCAPED_UNICODE);
    exit;
}

function getJsonInput(): array
{
    $raw = file_get_contents('php://input');
    $data = json_decode($raw, true);

    if (!is_array($data)) {
        responseJson(400, [
            'success' => false,
            'message' => 'Format data yang dikirim tidak valid.'
        ]);
    }

    return $data;
}

$method = $_SERVER['REQUEST_METHOD'];

try {
    /*
     * GET: mengambil daftar pesanan untuk dashboard penjual.
     */
    if ($method === 'GET') {
        $isSeller = ($_SESSION['account']['role'] ?? '') === 'penjual';
        $isBuyer = ($_SESSION['account']['role'] ?? '') === 'pembeli';
        if (!$isSeller && !$isBuyer) {
            responseJson(401, [
                'success' => false,
                'message' => 'Silakan masuk sebagai pembeli atau penjual terlebih dahulu.'
            ]);
        }
        if ($isBuyer) {
            $idUser = (int) $_SESSION['account']['id'];
            $where = 'WHERE dp.id_user = ?';
        } else {
            $where = '';
        }

        $sql = "
            SELECT
                dp.id_pemesanan,
                dp.nama_pembeli,
                dp.tanggal_pemesanan,
                dp.status,
                dp.total_jumlah,
                dp.tracking_token,
                dp.catatan, -- [CATATAN]
                p.metode_pembayaran,
                p.status_pembayaran,
                p.tanggal_pembayaran,
                oi.id_order_item,
                oi.menu_id,
                oi.jumlah,
                oi.harga_satuan,
                oi.subtotal,
                m.nama AS nama_menu
            FROM detail_pemesanan dp
            LEFT JOIN pembayaran p
                ON p.id_pemesanan = dp.id_pemesanan
            LEFT JOIN order_item oi
                ON oi.id_pemesanan = dp.id_pemesanan
            LEFT JOIN menu m
                ON m.id_menu = oi.menu_id
            $where
            ORDER BY dp.tanggal_pemesanan DESC, dp.id_pemesanan DESC
        ";

        if ($isSeller) {
            $result = mysqli_query($koneksi, $sql);
        } else {
            $stmt = mysqli_prepare($koneksi, $sql);
            mysqli_stmt_bind_param($stmt, 'i', $idUser);
            mysqli_stmt_execute($stmt);
            $result = mysqli_stmt_get_result($stmt);
        }
        $orders = [];

        while ($row = mysqli_fetch_assoc($result)) {
            $id = (int) $row['id_pemesanan'];

            if (!isset($orders[$id])) {
                $orders[$id] = [
                    'id' => (string) $id,
                    'trackingToken' => $row['tracking_token'],
                    'catatan' => $row['catatan'] ?? '', // [CATATAN]
                    'buyerName' => $row['nama_pembeli'],
                    'tanggal' => $row['tanggal_pemesanan'],
                    'status' => $row['status'],
                    'total' => (float) $row['total_jumlah'],
                    'metode' => $row['metode_pembayaran'] ?? '-',
                    'paymentStatus' => $row['status_pembayaran'] ?? 'Menunggu verifikasi',
                    'paymentDate' => $row['tanggal_pembayaran'],
                    'items' => [],
                    'updates' => [],
                    'paymentUpdates' => []
                ];
            }

            if ($row['id_order_item'] !== null) {
                $orders[$id]['items'][] = [
                    'id' => (string) $row['menu_id'],
                    'nama' => $row['nama_menu'] ?? 'Menu tidak ditemukan',
                    'jumlah' => (int) $row['jumlah'],
                    'harga' => (float) $row['harga_satuan'],
                    'subtotal' => (float) $row['subtotal']
                ];
            }
        }

        if ($orders) {
            $orderIds = implode(',', array_map('intval', array_keys($orders)));
            $statusResult = mysqli_query(
                $koneksi,
                "SELECT id_pemesanan, status, dibuat_pada
                 FROM riwayat_status_pesanan
                 WHERE id_pemesanan IN ($orderIds)
                 ORDER BY dibuat_pada, id_riwayat"
            );
            while ($history = mysqli_fetch_assoc($statusResult)) {
                $orders[(int) $history['id_pemesanan']]['updates'][] = [
                    'status' => $history['status'],
                    'tanggal' => $history['dibuat_pada']
                ];
            }

            $paymentHistoryResult = mysqli_query(
                $koneksi,
                "SELECT id_pemesanan, status, dibuat_pada
                 FROM riwayat_status_pembayaran
                 WHERE id_pemesanan IN ($orderIds)
                 ORDER BY dibuat_pada, id_riwayat"
            );
            while ($history = mysqli_fetch_assoc($paymentHistoryResult)) {
                $orders[(int) $history['id_pemesanan']]['paymentUpdates'][] = [
                    'status' => $history['status'],
                    'tanggal' => $history['dibuat_pada']
                ];
            }

            foreach ($orders as &$order) {
                if (!$order['updates']) {
                    $order['updates'][] = [
                        'status' => $order['status'],
                        'tanggal' => $order['tanggal']
                    ];
                }
                if (!$order['paymentUpdates']) {
                    $order['paymentUpdates'][] = [
                        'status' => $order['paymentStatus'],
                        'tanggal' => $order['tanggal']
                    ];
                }
            }
            unset($order);
        }

        responseJson(200, [
            'success' => true,
            'data' => array_values($orders)
        ]);
    }

    /*
     * Semua perubahan data dilakukan melalui POST.
     */
    if ($method !== 'POST') {
        responseJson(405, [
            'success' => false,
            'message' => 'Metode permintaan tidak didukung.'
        ]);
    }

    $data = getJsonInput();
    $action = $data['action'] ?? 'buat_pesanan';

    /*
     * POST action: buat_pesanan
     * Menyimpan satu pesanan, semua item, dan data pembayaran.
     */
    if ($action === 'buat_pesanan') {
        if (($_SESSION['account']['role'] ?? '') !== 'pembeli') {
            responseJson(401, [
                'success' => false,
                'message' => 'Silakan masuk sebagai pembeli sebelum membuat pesanan.'
            ]);
        }
        $idUser = (int) $_SESSION['account']['id'];
        $namaPembeli = $_SESSION['account']['username'];
        $metode = trim($data['metode_pembayaran'] ?? '');
        $items = $data['items'] ?? [];
        $catatan = mb_substr(trim((string) ($data['catatan'] ?? '')), 0, 200); // [CATATAN]
        $catatan = $catatan === '' ? null : $catatan; // [CATATAN]

        $metodeDiizinkan = [
            'QRIS',
            'Tunai / COD'
        ];

        if (mb_strlen($namaPembeli) < 2 || mb_strlen($namaPembeli) > 100) {
            responseJson(422, [
                'success' => false,
                'message' => 'Nama pembeli harus terdiri dari 2 sampai 100 karakter.'
            ]);
        }

        if (!in_array($metode, $metodeDiizinkan, true)) {
            responseJson(422, [
                'success' => false,
                'message' => 'Metode pembayaran tidak valid.'
            ]);
        }

        if (!is_array($items) || count($items) === 0) {
            responseJson(422, [
                'success' => false,
                'message' => 'Keranjang masih kosong.'
            ]);
        }

        if (count($items) > 100) {
            responseJson(422, [
                'success' => false,
                'message' => 'Jumlah jenis menu dalam pesanan terlalu banyak.'
            ]);
        }

        /*
         * Ambil harga dari database, bukan harga yang dikirim browser.
         * ID menu pada katalog web saat ini berupa slug, misalnya "nasi-goreng".
         * Karena itu pencarian memakai nama menu yang sesuai dengan database.
         */
        $validatedItems = [];
        $total = 0;

        foreach ($items as $item) {
            $namaMenu = trim($item['nama'] ?? '');
            $jumlah = filter_var(
                $item['jumlah'] ?? null,
                FILTER_VALIDATE_INT,
                ['options' => ['min_range' => 1, 'max_range' => 999]]
            );

            if ($namaMenu === '' || $jumlah === false) {
                responseJson(422, [
                    'success' => false,
                    'message' => 'Data menu atau jumlah pesanan tidak valid.'
                ]);
            }

            $stmtMenu = mysqli_prepare(
                $koneksi,
                'SELECT id_menu, nama, harga FROM menu WHERE nama = ? AND aktif = 1 LIMIT 1'
            );
            mysqli_stmt_bind_param($stmtMenu, 's', $namaMenu);
            mysqli_stmt_execute($stmtMenu);
            $menuResult = mysqli_stmt_get_result($stmtMenu);
            $menu = mysqli_fetch_assoc($menuResult);
            mysqli_stmt_close($stmtMenu);

            if (!$menu) {
                responseJson(422, [
                    'success' => false,
                    'message' => 'Menu "' . $namaMenu . '" tidak ditemukan di database.'
                ]);
            }

            $harga = (float) $menu['harga'];
            $subtotal = $harga * $jumlah;
            $total += $subtotal;

            $validatedItems[] = [
                'menu_id' => (int) $menu['id_menu'],
                'nama' => $menu['nama'],
                'jumlah' => $jumlah,
                'harga' => $harga,
                'subtotal' => $subtotal
            ];
        }

        mysqli_begin_transaction($koneksi);

        try {
            $statusPesanan = 'Menunggu konfirmasi';
            $trackingToken = bin2hex(random_bytes(32));

            $stmtOrder = mysqli_prepare(
                $koneksi,
                'INSERT INTO detail_pemesanan
                    (id_user, nama_pembeli, status, total_jumlah, tracking_token)
                 VALUES (?, ?, ?, ?, ?)'
            );

            mysqli_stmt_bind_param(
                $stmtOrder,
                'issds',
                $idUser,
                $namaPembeli,
                $statusPesanan,
                $total,
                $trackingToken
            );
            mysqli_stmt_execute($stmtOrder);

            $idPesanan = (int) mysqli_insert_id($koneksi);
            mysqli_stmt_close($stmtOrder);

            // [CATATAN] simpan catatan pembeli (opsional)
            if ($catatan !== null) {
                $stmtCatatan = mysqli_prepare(
                    $koneksi,
                    'UPDATE detail_pemesanan SET catatan = ? WHERE id_pemesanan = ?'
                );
                mysqli_stmt_bind_param($stmtCatatan, 'si', $catatan, $idPesanan);
                mysqli_stmt_execute($stmtCatatan);
                mysqli_stmt_close($stmtCatatan);
            }

            $stmtItem = mysqli_prepare(
                $koneksi,
                'INSERT INTO order_item
                    (id_pemesanan, menu_id, jumlah, harga_satuan, subtotal)
                 VALUES (?, ?, ?, ?, ?)'
            );

            foreach ($validatedItems as $item) {
                mysqli_stmt_bind_param(
                    $stmtItem,
                    'iiidd',
                    $idPesanan,
                    $item['menu_id'],
                    $item['jumlah'],
                    $item['harga'],
                    $item['subtotal']
                );
                mysqli_stmt_execute($stmtItem);
            }

            mysqli_stmt_close($stmtItem);

            $statusPembayaran = 'Menunggu verifikasi';

            if ($metode === 'Tunai / COD') {
                $statusPembayaran = 'Menunggu pembayaran di tempat';
            }

            $stmtPayment = mysqli_prepare(
                $koneksi,
                'INSERT INTO pembayaran
                    (id_pemesanan, metode_pembayaran, status_pembayaran, tanggal_pembayaran)
                 VALUES (?, ?, ?, CURRENT_TIMESTAMP)'
            );

            mysqli_stmt_bind_param(
                $stmtPayment,
                'iss',
                $idPesanan,
                $metode,
                $statusPembayaran
            );
            mysqli_stmt_execute($stmtPayment);
            mysqli_stmt_close($stmtPayment);

            $stmtOrderHistory = mysqli_prepare(
                $koneksi,
                'INSERT INTO riwayat_status_pesanan (id_pemesanan, status) VALUES (?, ?)'
            );
            mysqli_stmt_bind_param($stmtOrderHistory, 'is', $idPesanan, $statusPesanan);
            mysqli_stmt_execute($stmtOrderHistory);
            mysqli_stmt_close($stmtOrderHistory);

            $stmtPaymentHistory = mysqli_prepare(
                $koneksi,
                'INSERT INTO riwayat_status_pembayaran (id_pemesanan, status) VALUES (?, ?)'
            );
            mysqli_stmt_bind_param($stmtPaymentHistory, 'is', $idPesanan, $statusPembayaran);
            mysqli_stmt_execute($stmtPaymentHistory);
            mysqli_stmt_close($stmtPaymentHistory);

            mysqli_commit($koneksi);

            responseJson(201, [
                'success' => true,
                'message' => 'Pesanan berhasil disimpan.',
                'data' => [
                    'id' => (string) $idPesanan,
                    'trackingToken' => $trackingToken,
                    'buyerName' => $namaPembeli,
                    'tanggal' => date('Y-m-d H:i:s'),
                    'status' => $statusPesanan,
                    'total' => $total,
                    'metode' => $metode,
                    'catatan' => $catatan ?? '', // [CATATAN]
                    'paymentStatus' => $statusPembayaran,
                    'paymentDate' => date('Y-m-d H:i:s'),
                    'items' => $validatedItems
                ]
            ]);
        } catch (Throwable $e) {
            mysqli_rollback($koneksi);
            throw $e;
        }
    }

    /*
     * POST action: ubah_status
     * Dipakai dashboard penjual untuk mengubah status pesanan/pembayaran.
     */
    if ($action === 'ubah_status') {
        if (($_SESSION['account']['role'] ?? '') !== 'penjual') {
            responseJson(401, [
                'success' => false,
                'message' => 'Silakan masuk sebagai penjual untuk memperbarui status pesanan.'
            ]);
        }
        $idPesanan = filter_var(
            $data['id_pemesanan'] ?? null,
            FILTER_VALIDATE_INT,
            ['options' => ['min_range' => 1]]
        );

        $jenis = $data['jenis'] ?? '';
        $status = trim($data['status'] ?? '');

        if ($idPesanan === false) {
            responseJson(422, [
                'success' => false,
                'message' => 'ID pesanan tidak valid.'
            ]);
        }

        if (!in_array($jenis, ['pesanan', 'pembayaran'], true)) {
            responseJson(422, [
                'success' => false,
                'message' => 'Jenis status tidak valid.'
            ]);
        }

        mysqli_begin_transaction($koneksi);
        try {
            $stmtCurrent = mysqli_prepare(
                $koneksi,
                'SELECT dp.status AS order_status, p.status_pembayaran AS payment_status
                 FROM detail_pemesanan dp
                 LEFT JOIN pembayaran p ON p.id_pemesanan = dp.id_pemesanan
                 WHERE dp.id_pemesanan = ?
                 FOR UPDATE'
            );
            mysqli_stmt_bind_param($stmtCurrent, 'i', $idPesanan);
            mysqli_stmt_execute($stmtCurrent);
            $current = mysqli_fetch_assoc(mysqli_stmt_get_result($stmtCurrent));
            mysqli_stmt_close($stmtCurrent);

            if (!$current) {
                mysqli_rollback($koneksi);
                responseJson(404, [
                    'success' => false,
                    'message' => 'Pesanan tidak ditemukan.'
                ]);
            }

            if ($jenis === 'pembayaran') {
                $expectedPaymentStatus = $current['payment_status'] === 'Menunggu pembayaran di tempat'
                    ? 'Berhasil'
                    : 'Disetujui';
                if (!in_array($current['payment_status'], ['Menunggu verifikasi', 'Menunggu pembayaran di tempat'], true) ||
                    $status !== $expectedPaymentStatus) {
                    mysqli_rollback($koneksi);
                    responseJson(409, [
                        'success' => false,
                        'message' => 'Status pembayaran sudah berubah atau tidak cocok dengan metode pembayarannya. Muat ulang daftar pesanan.'
                    ]);
                }

                $stmtUpdate = mysqli_prepare(
                    $koneksi,
                    'UPDATE pembayaran SET status_pembayaran = ?, tanggal_pembayaran = CURRENT_TIMESTAMP WHERE id_pemesanan = ?'
                );
                mysqli_stmt_bind_param($stmtUpdate, 'si', $status, $idPesanan);
                mysqli_stmt_execute($stmtUpdate);
                mysqli_stmt_close($stmtUpdate);

                $stmtHistory = mysqli_prepare(
                    $koneksi,
                    'INSERT INTO riwayat_status_pembayaran (id_pemesanan, status) VALUES (?, ?)'
                );
                mysqli_stmt_bind_param($stmtHistory, 'is', $idPesanan, $status);
                mysqli_stmt_execute($stmtHistory);
                mysqli_stmt_close($stmtHistory);

                if ($current['order_status'] === 'Menunggu konfirmasi') {
                    $statusPesanan = 'Diproses';
                    $stmtUpdateOrder = mysqli_prepare(
                        $koneksi,
                        'UPDATE detail_pemesanan SET status = ? WHERE id_pemesanan = ?'
                    );
                    mysqli_stmt_bind_param($stmtUpdateOrder, 'si', $statusPesanan, $idPesanan);
                    mysqli_stmt_execute($stmtUpdateOrder);
                    mysqli_stmt_close($stmtUpdateOrder);

                    $stmtOrderHistory = mysqli_prepare(
                        $koneksi,
                        'INSERT INTO riwayat_status_pesanan (id_pemesanan, status) VALUES (?, ?)'
                    );
                    mysqli_stmt_bind_param($stmtOrderHistory, 'is', $idPesanan, $statusPesanan);
                    mysqli_stmt_execute($stmtOrderHistory);
                    mysqli_stmt_close($stmtOrderHistory);
                }
            } else {
                $nextStatuses = [
                    'Menunggu konfirmasi' => 'Diproses',
                    'Diproses' => 'Siap diambil',
                    'Siap diambil' => 'Selesai'
                ];
                $expectedStatus = $nextStatuses[$current['order_status']] ?? null;

                if ($status !== $expectedStatus) {
                    mysqli_rollback($koneksi);
                    responseJson(409, [
                        'success' => false,
                        'message' => 'Status pesanan sudah berubah atau urutan proses tidak valid. Muat ulang daftar pesanan.'
                    ]);
                }
                if (!in_array($current['payment_status'], ['Disetujui', 'Berhasil'], true)) {
                    mysqli_rollback($koneksi);
                    responseJson(409, [
                        'success' => false,
                        'message' => 'Pesanan hanya dapat diproses setelah pembayaran dikonfirmasi penjual.'
                    ]);
                }

                $stmtUpdate = mysqli_prepare(
                    $koneksi,
                    'UPDATE detail_pemesanan SET status = ? WHERE id_pemesanan = ?'
                );
                mysqli_stmt_bind_param($stmtUpdate, 'si', $status, $idPesanan);
                mysqli_stmt_execute($stmtUpdate);
                mysqli_stmt_close($stmtUpdate);

                $stmtHistory = mysqli_prepare(
                    $koneksi,
                    'INSERT INTO riwayat_status_pesanan (id_pemesanan, status) VALUES (?, ?)'
                );
                mysqli_stmt_bind_param($stmtHistory, 'is', $idPesanan, $status);
                mysqli_stmt_execute($stmtHistory);
                mysqli_stmt_close($stmtHistory);
            }

            mysqli_commit($koneksi);
        } catch (Throwable $e) {
            mysqli_rollback($koneksi);
            throw $e;
        }

        responseJson(200, [
            'success' => true,
            'message' => 'Status berhasil diperbarui.'
        ]);
    }

    responseJson(400, [
        'success' => false,
        'message' => 'Aksi tidak dikenali.'
    ]);
} catch (Throwable $e) {
    error_log('API pesanan KantinKu: ' . $e->getMessage());

    responseJson(500, [
        'success' => false,
        'message' => 'Terjadi kesalahan saat memproses pesanan. Periksa struktur tabel dan koneksi database.'
    ]);
}
