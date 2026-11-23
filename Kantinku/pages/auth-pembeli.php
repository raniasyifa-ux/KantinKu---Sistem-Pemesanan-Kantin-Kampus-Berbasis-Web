<?php
header('Cache-Control: no-store, no-cache, must-revalidate, max-age=0');
header('Pragma: no-cache');
?>
<!DOCTYPE html>
<html lang="id">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Akun Pembeli - KantinKu</title>
  <link rel="stylesheet" href="../css/auth.css?v=20261110-2">
</head>
<body class="auth-body buyer-auth-body" data-auth-role="pembeli">
  <div class="auth-card">
    <div id="login-box">
      <div class="auth-header"><h2>Masuk sebagai pembeli</h2><p id="login-description">Masuk untuk memesan di KantinKu</p></div>
      <form onsubmit="handleLogin(event)" novalidate autocomplete="off">
        <div class="form-group"><label>Username / Email</label><input type="text" name="userOrEmail" placeholder="Username atau email pembeli" required></div>
        <div class="form-group password-group"><label>Password</label><div class="password-input"><input type="password" name="password" placeholder="Password" required><button type="button" class="password-toggle" data-password-toggle aria-label="Tampilkan password">◉</button></div></div>
        <div class="form-options"><a href="#" class="forgot-link" onclick="switchAuth('reset')">Lupa password?</a></div>
        <button type="submit" class="btn-primary auth-submit">Masuk ke dashboard</button>
      </form>
      <p class="switch-auth">Belum punya akun pembeli? <a href="#" onclick="switchAuth('register')">Buat akun</a></p>
      <p class="switch-auth portal-back"><a href="../index.html">Kembali ke Portal Pembeli</a></p>
    </div>
    <div id="register-box" hidden>
      <div class="auth-header"><h2>Buat akun pembeli</h2><p id="register-description">Daftar untuk memesan dan melihat pesananmu.</p></div>
      <form onsubmit="handleRegister(event)" novalidate autocomplete="off">
        <div class="form-group"><label>Username (4-12 karakter)</label><input type="text" name="username" placeholder="Contoh: Dihar" required minlength="4" maxlength="12"></div>
        <div class="form-group"><label>Email</label><input type="email" name="email" placeholder="kamu@email.com" required></div>
        <div class="form-group password-group"><label>Password (min. 8 karakter, huruf dan angka)</label><div class="password-input"><input type="password" name="password" placeholder="Buat password" required><button type="button" class="password-toggle" data-password-toggle aria-label="Tampilkan password">◉</button></div></div>
        <button type="submit" class="btn-primary auth-submit">Buat akun pembeli</button>
      </form>
      <p class="switch-auth">Sudah punya akun? <a href="#" onclick="switchAuth('login')">Masuk di sini</a></p>
    </div>
    <div id="reset-box" hidden>
      <div class="auth-header"><h2>Lupa password pembeli</h2><p>Verifikasi dengan username dan email terdaftar, lalu buat password baru.</p></div>
      <form onsubmit="handleResetPassword(event)" novalidate autocomplete="off">
        <div class="form-group"><label>Username</label><input type="text" name="resetUsername" placeholder="Username terdaftar" required minlength="4" maxlength="12"></div>
        <div class="form-group"><label>Email</label><input type="email" name="resetEmail" placeholder="Email terdaftar" required></div>
        <div class="form-group password-group"><label>Password baru</label><div class="password-input"><input type="password" name="newPassword" placeholder="Password baru" required><button type="button" class="password-toggle" data-password-toggle aria-label="Tampilkan password">◉</button></div></div>
        <button type="submit" class="btn-primary auth-submit">Simpan password</button>
      </form>
      <p class="switch-auth"><a href="#" onclick="switchAuth('login')">Kembali ke login pembeli</a></p>
    </div>
  </div>
  <script src="../js/auth.js?v=20261110-2"></script>
</body>
</html>
