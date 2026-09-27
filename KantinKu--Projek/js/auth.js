const authParams = new URLSearchParams(location.search);
const selectedRole = document.body.dataset.authRole === 'penjual' ? 'penjual' : 'pembeli';
const roleLabel = selectedRole === 'penjual' ? 'penjual' : 'pembeli';

document.addEventListener('DOMContentLoaded', () => {
  document.getElementById('login-description').textContent = `Masuk ke akun ${roleLabel} KantinKu`;
  document.getElementById('register-description').textContent = `Buat akun ${roleLabel} untuk mulai ${selectedRole === 'penjual' ? 'menerima pesanan' : 'memesan'}`;
  document.querySelectorAll('.auth-submit').forEach(button => {
    button.textContent = `${button.closest('#login-box') ? 'Masuk' : 'Daftar'} sebagai ${roleLabel}`;
  });
  if (authParams.get('view') === 'register') switchAuth('register');
});

function showMessage(message, type = 'success') {
  const element = document.getElementById('auth-message');
  element.textContent = message;
  element.className = `auth-message is-${type}`;
  element.hidden = false;
}

function getUsers() {
  try {
    const stored = JSON.parse(localStorage.getItem('kantinku_db'));
    return Array.isArray(stored) ? stored : [];
  } catch {
    return [];
  }
}

function isValidPassword(password) {
  return /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,}$/.test(password);
}

function handleRegister(event) {
  event.preventDefault();
  const activeSession = getActiveSession();
  if (activeSession) {
    showMessage(`Keluar dari sesi ${activeSession.role} terlebih dahulu sebelum membuat akun baru.`, 'error');
    return;
  }
  const form = event.currentTarget;
  const username = form.elements.namedItem('username').value.trim();
  const email = form.elements.namedItem('email').value.trim().toLowerCase();
  const password = form.elements.namedItem('password').value;
  const users = getUsers();

  if (!/^[a-zA-Z0-9]{4,12}$/.test(username)) {
    showMessage('Username harus 4-12 karakter huruf atau angka.', 'error');
    return;
  }
  if (!isValidPassword(password)) {
    showMessage('Password minimal 8 karakter dan harus mengandung huruf serta angka.', 'error');
    return;
  }
  if (users.some(account => account.email.toLowerCase() === email || account.username.toLowerCase() === username.toLowerCase())) {
    showMessage('Username atau email sudah terdaftar. Gunakan data lain.', 'error');
    return;
  }

  users.push({ username, email, password, role: selectedRole });
  localStorage.setItem('kantinku_db', JSON.stringify(users));
  form.reset();
  switchAuth('login');
  showMessage(`Akun ${roleLabel} berhasil dibuat. Silakan masuk.`, 'success');
}

function handleLogin(event) {
  event.preventDefault();
  const activeSession = getActiveSession();
  if (activeSession) {
    if (activeSession.role !== selectedRole) {
      showMessage(`Sesi ${activeSession.role} masih aktif. Keluar terlebih dahulu untuk masuk sebagai ${roleLabel}.`, 'error');
      return;
    }
    location.href = selectedRole === 'penjual' ? '../pages/penjual.html' : '../pages/pembeli.html';
    return;
  }
  const form = event.currentTarget;
  const userOrEmail = form.elements.namedItem('userOrEmail').value.trim().toLowerCase();
  const password = form.elements.namedItem('password').value;
  const account = getUsers().find(item => item.username.toLowerCase() === userOrEmail || item.email.toLowerCase() === userOrEmail);

  if (!account || account.password !== password) {
    showMessage('Username/email atau password tidak cocok.', 'error');
    return;
  }
  if (account.role !== selectedRole) {
    showMessage(`Akun ini terdaftar sebagai ${account.role}. Gunakan tautan akun ${account.role}.`, 'error');
    return;
  }

  localStorage.setItem('kantinku_session', JSON.stringify(account));
  const allowedPages = selectedRole === 'penjual'
    ? ['../pages/penjual.html']
    : ['../index.html', '../pages/pembeli.html', '../pages/menu.html', '../pages/keranjang.html', '../pages/pembayaran.html', '../pages/status.html'];
  const next = authParams.get('next');
  location.href = allowedPages.includes(next) ? next : (selectedRole === 'penjual' ? '../pages/penjual.html' : '../index.html');
}

function getActiveSession() {
  try {
    const session = JSON.parse(localStorage.getItem('kantinku_session') || 'null');
    return session?.role === 'pembeli' || session?.role === 'penjual' ? session : null;
  } catch {
    return null;
  }
}

function handleResetPassword(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const email = form.elements.namedItem('resetEmail').value.trim().toLowerCase();
  const password = form.elements.namedItem('newPassword').value;
  const users = getUsers();
  const account = users.find(item => item.email.toLowerCase() === email && item.role === selectedRole);

  if (!account) {
    showMessage(`Email tidak ditemukan pada akun ${roleLabel}.`, 'error');
    return;
  }
  if (!isValidPassword(password)) {
    showMessage('Password minimal 8 karakter dan harus mengandung huruf serta angka.', 'error');
    return;
  }
  account.password = password;
  localStorage.setItem('kantinku_db', JSON.stringify(users));
  form.reset();
  switchAuth('login');
  showMessage('Password berhasil diubah. Silakan masuk kembali.', 'success');
}

function switchAuth(type) {
  document.getElementById('login-box').hidden = type !== 'login';
  document.getElementById('register-box').hidden = type !== 'register';
  document.getElementById('reset-box').hidden = type !== 'reset';
  document.getElementById('auth-message').hidden = true;
}