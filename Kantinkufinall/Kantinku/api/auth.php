<?php
header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store, no-cache, must-revalidate, max-age=0');
session_start();

require_once __DIR__ . '/../koneksi.php';
mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

function authResponse(int $status, array $data): void
{
    http_response_code($status);
    echo json_encode($data, JSON_UNESCAPED_UNICODE);
    exit;
}

function authInput(): array
{
    $data = json_decode(file_get_contents('php://input'), true);
    if (!is_array($data)) {
        authResponse(400, ['success' => false, 'message' => 'Format data yang dikirim tidak valid.']);
    }
    return $data;
}

function publicAccount(array $account): array
{
    return [
        'id' => (int) $account['id_user'],
        'username' => $account['username'],
        'email' => $account['email'],
        'role' => $account['role']
    ];
}

try {
    $data = authInput();
    $action = $data['action'] ?? '';

    if ($action === 'register') {
        $username = trim($data['username'] ?? '');
        $email = strtolower(trim($data['email'] ?? ''));
        $password = $data['password'] ?? '';
        $role = $data['role'] ?? '';

        if (!preg_match('/^[a-zA-Z0-9]{4,12}$/', $username)) {
            authResponse(422, ['success' => false, 'message' => 'Username harus 4-12 karakter huruf atau angka.', 'fields' => ['username']]);
        }
        if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
            authResponse(422, ['success' => false, 'message' => 'Format email tidak valid. Periksa kembali alamat email.', 'fields' => ['email']]);
        }
        if (!preg_match('/^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,}$/', $password) || preg_match('/\s/', $password)) {
            authResponse(422, ['success' => false, 'message' => 'Password minimal 8 karakter, harus mengandung huruf dan angka, serta tidak boleh mengandung spasi.', 'fields' => ['password']]);
        }
        if (!in_array($role, ['pembeli', 'penjual'], true)) {
            authResponse(422, ['success' => false, 'message' => 'Jenis akun tidak valid.']);
        }

        $stmt = mysqli_prepare($koneksi, 'SELECT id_user FROM users WHERE BINARY username = ? LIMIT 1');
        mysqli_stmt_bind_param($stmt, 's', $username);
        mysqli_stmt_execute($stmt);
        $duplicateUsername = mysqli_fetch_assoc(mysqli_stmt_get_result($stmt));
        mysqli_stmt_close($stmt);
        $stmt = mysqli_prepare($koneksi, 'SELECT id_user FROM users WHERE LOWER(email) = ? LIMIT 1');
        mysqli_stmt_bind_param($stmt, 's', $email);
        mysqli_stmt_execute($stmt);
        $duplicateEmail = mysqli_fetch_assoc(mysqli_stmt_get_result($stmt));
        mysqli_stmt_close($stmt);
        if ($duplicateUsername || $duplicateEmail) {
            $fields = [];
            $messages = [];
            if ($duplicateUsername) {
                $fields[] = 'username';
                $messages[] = 'Username tersebut sudah digunakan. Pilih username lain.';
            }
            if ($duplicateEmail) {
                $fields[] = 'email';
                $messages[] = 'Email tersebut sudah terdaftar. Gunakan email lain.';
            }
            authResponse(409, ['success' => false, 'message' => implode("\n", $messages), 'fields' => $fields]);
        }

        $passwordHash = password_hash($password, PASSWORD_DEFAULT);
        $stmt = mysqli_prepare(
            $koneksi,
            'INSERT INTO users (nama, email, username, password, role) VALUES (?, ?, ?, ?, ?)'
        );
        mysqli_stmt_bind_param($stmt, 'sssss', $username, $email, $username, $passwordHash, $role);
        try {
            mysqli_stmt_execute($stmt);
        } catch (mysqli_sql_exception $exception) {
            if ($exception->getCode() === 1062) {
                mysqli_stmt_close($stmt);
                authResponse(409, ['success' => false, 'message' => "Username atau email sudah digunakan.\nPeriksa kembali kedua kolom tersebut.", 'fields' => ['username', 'email']]);
            }
            throw $exception;
        }
        mysqli_stmt_close($stmt);

        authResponse(201, ['success' => true, 'message' => 'Akun berhasil dibuat. Silakan masuk.']);
    }

    if ($action === 'login') {
        $userOrEmail = trim($data['userOrEmail'] ?? '');
        $emailLookup = strtolower($userOrEmail);
        $password = $data['password'] ?? '';
        $role = $data['role'] ?? '';

        if (!in_array($role, ['pembeli', 'penjual'], true)) {
            authResponse(422, ['success' => false, 'message' => 'Jenis akun tidak valid.']);
        }

        $stmt = mysqli_prepare(
            $koneksi,
            'SELECT id_user, email, username, password, role FROM users WHERE BINARY username = ? OR LOWER(email) = ? LIMIT 1'
        );
        mysqli_stmt_bind_param($stmt, 'ss', $userOrEmail, $emailLookup);
        mysqli_stmt_execute($stmt);
        $result = mysqli_stmt_get_result($stmt);
        $account = mysqli_fetch_assoc($result);
        mysqli_stmt_close($stmt);

        if (!$account) {
            $isEmail = filter_var($userOrEmail, FILTER_VALIDATE_EMAIL) !== false;
            $message = $isEmail
                ? 'Email tidak ditemukan. Periksa alamat email yang Anda masukkan.'
                : 'Username tidak ditemukan. Periksa penulisan username Anda.';
            authResponse(401, ['success' => false, 'message' => $message . "\nPassword juga tidak dapat diverifikasi.", 'fields' => ['userOrEmail', 'password']]);
        }
        if (!password_verify($password, $account['password'])) {
            authResponse(401, ['success' => false, 'message' => 'Password yang Anda masukkan salah.', 'fields' => ['password']]);
        }
        if ($account['role'] !== $role) {
            authResponse(403, ['success' => false, 'message' => 'Akun ini terdaftar sebagai ' . $account['role'] . '. Silakan masuk melalui halaman akun ' . $account['role'] . '.', 'fields' => ['userOrEmail']]);
        }

        session_regenerate_id(true);
        $_SESSION['account'] = publicAccount($account);
        authResponse(200, ['success' => true, 'data' => $_SESSION['account']]);
    }

    if ($action === 'reset_password') {
        $email = strtolower(trim($data['email'] ?? ''));
        $username = trim($data['username'] ?? '');
        $password = $data['password'] ?? '';
        $role = $data['role'] ?? '';

        if (!filter_var($email, FILTER_VALIDATE_EMAIL) ||
            !in_array($role, ['pembeli', 'penjual'], true) ||
            !preg_match('/^[a-zA-Z0-9]{4,12}$/', $username) ||
            !preg_match('/^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,}$/', $password) ||
            preg_match('/\s/', $password)) {
            authResponse(422, ['success' => false, 'message' => 'Email, username, atau password baru tidak valid. Periksa format masing-masing kolom.', 'fields' => ['resetUsername', 'resetEmail', 'newPassword']]);
        }

        $stmt = mysqli_prepare(
            $koneksi,
            'SELECT id_user FROM users WHERE LOWER(email) = ? AND BINARY username = ? AND role = ? LIMIT 1'
        );
        mysqli_stmt_bind_param($stmt, 'sss', $email, $username, $role);
        mysqli_stmt_execute($stmt);
        $account = mysqli_fetch_assoc(mysqli_stmt_get_result($stmt));
        mysqli_stmt_close($stmt);
        if (!$account) {
            authResponse(401, ['success' => false, 'message' => 'Username dan email tidak cocok dengan akun ' . $role . '.', 'fields' => ['resetUsername', 'resetEmail']]);
        }

        $passwordHash = password_hash($password, PASSWORD_DEFAULT);
        $stmt = mysqli_prepare(
            $koneksi,
            'UPDATE users SET password = ? WHERE LOWER(email) = ? AND BINARY username = ? AND role = ?'
        );
        mysqli_stmt_bind_param($stmt, 'ssss', $passwordHash, $email, $username, $role);
        mysqli_stmt_execute($stmt);
        $updated = mysqli_stmt_affected_rows($stmt);
        mysqli_stmt_close($stmt);

        if ($updated < 1) {
            authResponse(404, ['success' => false, 'message' => 'Email tidak ditemukan pada akun ' . $role . '.']);
        }
        authResponse(200, ['success' => true, 'message' => 'Password berhasil diubah. Silakan masuk kembali.']);
    }

    if ($action === 'logout') {
        $_SESSION = [];
        if (ini_get('session.use_cookies')) {
            $params = session_get_cookie_params();
            setcookie(session_name(), '', time() - 42000, $params['path'], $params['domain'], $params['secure'], $params['httponly']);
        }
        session_destroy();
        authResponse(200, ['success' => true]);
    }

    authResponse(400, ['success' => false, 'message' => 'Aksi akun tidak dikenal.']);
} catch (Throwable $exception) {
    error_log('Auth API error: ' . $exception->getMessage());
    authResponse(500, ['success' => false, 'message' => 'Terjadi kesalahan server saat memproses akun.']);
}
