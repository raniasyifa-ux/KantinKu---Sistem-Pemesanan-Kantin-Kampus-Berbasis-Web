
<?php
if (
    PHP_SAPI !== 'cli' &&
    ($_SERVER['REQUEST_METHOD'] ?? '') !== 'CLI' &&
    (($_SERVER['HTTP_HOST'] ?? '') !== 'localhost')
) {
    http_response_code(403);
    exit('Akses ditolak. Jalankan KantinKu melalui http://localhost/Kantinku/.');
}

$host = 'localhost';
$user = 'root';
$password = '';
$database = 'db_kantinku';

$koneksi = mysqli_connect(
    $host,
    $user,
    $password,
    $database
);

if (!$koneksi) {
    die('Koneksi database gagal: ' . mysqli_connect_error());
}

mysqli_set_charset($koneksi, 'utf8mb4');
?>
