<?php
header('Content-Type: application/json; charset=utf-8');
session_start();

require_once __DIR__ . '/../koneksi.php';
mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

function menuResponse(int $status, array $data): void
{
    http_response_code($status);
    echo json_encode($data, JSON_UNESCAPED_UNICODE);
    exit;
}

function menuInput(): array
{
    $data = json_decode(file_get_contents('php://input'), true);
    if (!is_array($data)) {
        menuResponse(400, ['success' => false, 'message' => 'Format data yang dikirim tidak valid.']);
    }
    return $data;
}

function requireSeller(): void
{
    if (($_SESSION['account']['role'] ?? '') !== 'penjual') {
        menuResponse(401, ['success' => false, 'message' => 'Silakan masuk sebagai penjual terlebih dahulu.']);
    }
}

try {
    if ($_SERVER['REQUEST_METHOD'] === 'GET') {
        $result = mysqli_query(
            $koneksi,
            'SELECT id_menu, nama, deskripsi, kategori, harga, gambar, aktif FROM menu ORDER BY id_menu DESC'
        );
        $items = [];
        while ($row = mysqli_fetch_assoc($result)) {
            $image = basename(str_replace('\\', '/', trim($row['gambar'] ?? '')));
            $items[] = [
                'id' => (string) $row['id_menu'],
                'nama' => $row['nama'],
                'deskripsi' => $row['deskripsi'] ?? '',
                'kategori' => $row['kategori'],
                'harga' => (float) $row['harga'],
                'gambar' => $image !== '' && $image !== '.' ? '../images/' . $image : '',
                'aktif' => (bool) $row['aktif'],
                'sellerId' => 'kantinku',
                'toko' => 'KantinKu'
            ];
        }
        menuResponse(200, ['success' => true, 'data' => $items]);
    }

    if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
        menuResponse(405, ['success' => false, 'message' => 'Metode permintaan tidak didukung.']);
    }
    requireSeller();
    $data = menuInput();
    $action = $data['action'] ?? '';

    if ($action === 'save') {
        $id = $data['id'] ?? '';
        $nama = trim($data['nama'] ?? '');
        $deskripsi = trim($data['deskripsi'] ?? '');
        $kategori = $data['kategori'] ?? '';
        $harga = filter_var($data['harga'] ?? null, FILTER_VALIDATE_FLOAT);
        $gambar = basename(str_replace('\\', '/', trim($data['gambar'] ?? '')));

        if ($nama === '' || mb_strlen($nama) > 100 ||
            mb_strlen($deskripsi) > 500 ||
            !in_array($kategori, ['Makanan', 'Minuman'], true) ||
            $harga === false || $harga < 500 || $harga > 999999999) {
            menuResponse(422, ['success' => false, 'message' => 'Data menu tidak valid. Periksa nama, kategori, deskripsi, dan harga.']);
        }
        if ($gambar === '.' || $gambar === '..') {
            $gambar = '';
        }

        if ($id === '') {
            $stmt = mysqli_prepare(
                $koneksi,
                'INSERT INTO menu (nama, deskripsi, kategori, harga, gambar) VALUES (?, ?, ?, ?, ?)'
            );
            mysqli_stmt_bind_param($stmt, 'sssds', $nama, $deskripsi, $kategori, $harga, $gambar);
            mysqli_stmt_execute($stmt);
            $id = (string) mysqli_insert_id($koneksi);
            mysqli_stmt_close($stmt);
        } else {
            $menuId = filter_var($id, FILTER_VALIDATE_INT, ['options' => ['min_range' => 1]]);
            if ($menuId === false) {
                menuResponse(422, ['success' => false, 'message' => 'ID menu tidak valid.']);
            }
            $stmt = mysqli_prepare(
                $koneksi,
                'UPDATE menu SET nama = ?, deskripsi = ?, kategori = ?, harga = ?, gambar = ? WHERE id_menu = ?'
            );
            mysqli_stmt_bind_param($stmt, 'sssdsi', $nama, $deskripsi, $kategori, $harga, $gambar, $menuId);
            mysqli_stmt_execute($stmt);
            mysqli_stmt_close($stmt);
            $check = mysqli_prepare($koneksi, 'SELECT id_menu FROM menu WHERE id_menu = ?');
            mysqli_stmt_bind_param($check, 'i', $menuId);
            mysqli_stmt_execute($check);
            $exists = mysqli_stmt_get_result($check)->num_rows > 0;
            mysqli_stmt_close($check);
            if (!$exists) {
                menuResponse(404, ['success' => false, 'message' => 'Menu tidak ditemukan.']);
            }
            $id = (string) $menuId;
        }

        menuResponse(200, ['success' => true, 'data' => ['id' => $id]]);
    }

    if ($action === 'availability') {
        $menuId = filter_var($data['id'] ?? null, FILTER_VALIDATE_INT, ['options' => ['min_range' => 1]]);
        $active = filter_var($data['aktif'] ?? null, FILTER_VALIDATE_BOOLEAN, FILTER_NULL_ON_FAILURE);
        if ($menuId === false || $active === null) {
            menuResponse(422, ['success' => false, 'message' => 'Status menu tidak valid.']);
        }
        $stmt = mysqli_prepare($koneksi, 'UPDATE menu SET aktif = ? WHERE id_menu = ?');
        $activeInt = (int) $active;
        mysqli_stmt_bind_param($stmt, 'ii', $activeInt, $menuId);
        mysqli_stmt_execute($stmt);
        $found = mysqli_stmt_affected_rows($stmt) !== 0;
        mysqli_stmt_close($stmt);
        if (!$found) {
            $check = mysqli_prepare($koneksi, 'SELECT id_menu FROM menu WHERE id_menu = ?');
            mysqli_stmt_bind_param($check, 'i', $menuId);
            mysqli_stmt_execute($check);
            $exists = mysqli_stmt_get_result($check)->num_rows > 0;
            mysqli_stmt_close($check);
            if (!$exists) {
                menuResponse(404, ['success' => false, 'message' => 'Menu tidak ditemukan.']);
            }
        }
        menuResponse(200, ['success' => true]);
    }

    if ($action === 'delete') {
        $menuId = filter_var($data['id'] ?? null, FILTER_VALIDATE_INT, ['options' => ['min_range' => 1]]);
        if ($menuId === false) {
            menuResponse(422, ['success' => false, 'message' => 'ID menu tidak valid.']);
        }
        $stmt = mysqli_prepare($koneksi, 'DELETE FROM menu WHERE id_menu = ?');
        mysqli_stmt_bind_param($stmt, 'i', $menuId);
        mysqli_stmt_execute($stmt);
        $deleted = mysqli_stmt_affected_rows($stmt);
        mysqli_stmt_close($stmt);
        if ($deleted === 0) {
            menuResponse(404, ['success' => false, 'message' => 'Menu tidak ditemukan.']);
        }
        menuResponse(200, ['success' => true]);
    }

    menuResponse(400, ['success' => false, 'message' => 'Aksi menu tidak dikenal.']);
} catch (mysqli_sql_exception $exception) {
    error_log('Menu API error: ' . $exception->getMessage());
    if ($exception->getCode() === 1451) {
        menuResponse(409, ['success' => false, 'message' => 'Menu yang sudah dipakai dalam pesanan tidak dapat dihapus.']);
    }
    menuResponse(500, ['success' => false, 'message' => 'Gagal memproses menu di database.']);
} catch (Throwable $exception) {
    error_log('Menu API error: ' . $exception->getMessage());
    menuResponse(500, ['success' => false, 'message' => 'Terjadi kesalahan server saat memproses menu.']);
}
