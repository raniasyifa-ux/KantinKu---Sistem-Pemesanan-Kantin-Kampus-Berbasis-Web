// Database Sederhana via LocalStorage
let users = JSON.parse(localStorage.getItem('kantinku_db')) || [];
let activeUser = JSON.parse(localStorage.getItem('kantinku_session')) || null;

// Notifikasi Toast
function showToast(msg, type = 'success') {
  const toast = document.getElementById('toast');
  toast.innerText = msg;
  toast.className = `show ${type}`;
  setTimeout(() => { 
    toast.className = toast.className.replace(`show ${type}`, ''); 
  }, 3000);
}

// Proteksi Halaman
function guardAction(event, pageName) {
  if (!activeUser) {
    event.preventDefault();
    showToast(`Silakan Login dulu untuk membuka menu ${pageName}!`, 'error');
  }
}

// Inisialisasi Tampilan Home
function initHome() {
  const navAuth = document.getElementById('nav-auth');
  const dashSection = document.getElementById('dashboard');

  if (activeUser) {
    navAuth.innerHTML = `
      <span>Hai, <b>${activeUser.username}</b></span>
      <button onclick="logout()" class="btn-nav" style="margin-left:10px;">Keluar</button>
    `;
    if (dashSection) {
      dashSection.style.display = 'block';
      dashSection.innerHTML = `
        <div class="dash-header">
          <h2>Dashboard ${activeUser.role === 'penjual' ? 'Penjual (Stan Kantin)' : 'Pembeli'}</h2>
          <span class="badge">${activeUser.role.toUpperCase()}</span>
        </div>
        <p>Selamat datang <b>${activeUser.username}</b>! Anda terhubung sebagai <b>${activeUser.role}</b>.</p>
      `;
    }
  }
}

// Registrasi Akun
function handleRegister(e) {
  e.preventDefault();
  const form = e.target;
  const username = form.username.value.trim();
  const email = form.email.value.trim();
  const password = form.password.value;
  const role = form.role.value;

  try {
    if (!/^[a-zA-Z0-9]{4,12}$/.test(username)) {
      throw "Username harus 4-12 karakter huruf/angka!";
    }
    if (!/^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,}$/.test(password)) {
      throw "Password min. 8 karakter (kombinasi huruf & angka)!";
    }
    if (users.some(u => u.email === email || u.username === username)) {
      throw "Username atau Email sudah terdaftar!";
    }

    users.push({ username, email, password, role });
    localStorage.setItem('kantinku_db', JSON.stringify(users));

    showToast("Pendaftaran berhasil! Mengalihkan ke Login...", "success");
    form.reset();
    setTimeout(() => switchAuth('login'), 1200);

  } catch (err) {
    showToast(err, "error");
    form.reset();
  }
}

// Login Akun
function handleLogin(e) {
  e.preventDefault();
  const form = e.target;
  const userOrEmail = form.userOrEmail.value.trim();
  const password = form.password.value;
  const role = form.role.value;

  try {
    const user = users.find(u => (u.username === userOrEmail || u.email === userOrEmail));
    if (!user) throw "Akun tidak ditemukan!";
    if (user.password !== password) throw "Password salah!";
    if (user.role !== role) throw `Akun ini terdaftar sebagai role '${user.role}'!`;

    activeUser = user;
    localStorage.setItem('kantinku_session', JSON.stringify(activeUser));

    showToast("Login berhasil!", "success");
    setTimeout(() => window.location.href = "../index.html", 1000);

  } catch (err) {
    showToast(err, "error");
    form.password.value = '';
  }
}

// Fitur Reset / Lupa Password
function handleResetPassword(e) {
  e.preventDefault();
  const form = e.target;
  const email = form.resetEmail.value.trim();
  const newPassword = form.newPassword.value;

  try {
    const userIndex = users.findIndex(u => u.email === email);
    if (userIndex === -1) {
      throw "Email tidak ditemukan dalam sistem!";
    }
    if (!/^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,}$/.test(newPassword)) {
      throw "Password baru min. 8 karakter (kombinasi huruf & angka)!";
    }

    // Update password
    users[userIndex].password = newPassword;
    localStorage.setItem('kantinku_db', JSON.stringify(users));

    showToast("Password berhasil diubah! Silakan login kembali.", "success");
    form.reset();
    setTimeout(() => switchAuth('login'), 1200);

  } catch (err) {
    showToast(err, "error");
    form.reset();
  }
}

// Logout
function logout() {
  localStorage.removeItem('kantinku_session');
  location.reload();
}

// Navigasi Tampilan Form di auth.html
function switchAuth(type) {
  document.getElementById('login-box').style.display = type === 'login' ? 'block' : 'none';
  document.getElementById('register-box').style.display = type === 'register' ? 'block' : 'none';
  document.getElementById('reset-box').style.display = type === 'reset' ? 'block' : 'none';
}