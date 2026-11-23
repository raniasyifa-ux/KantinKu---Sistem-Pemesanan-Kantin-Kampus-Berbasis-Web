const authParams = new URLSearchParams(location.search);
const selectedRole = document.body.dataset.authRole === 'penjual' ? 'penjual' : 'pembeli';
const roleLabel = selectedRole === 'penjual' ? 'penjual' : 'pembeli';

function requireLocalhost() {
  const isLocalhost = location.protocol === 'http:' && location.hostname === 'localhost';
  if (isLocalhost) return true;

  document.documentElement.innerHTML = `
    <head><meta charset="UTF-8"><title>Kantinku - Akses Ditolak</title></head>
    <body><main style="max-width:640px;margin:15vh auto;padding:32px;font-family:Segoe UI,sans-serif;text-align:center">
      <h1>Akses melalui XAMPP diperlukan</h1>
      <p>Buka aplikasi melalui <strong>http://localhost/Kantinku/</strong> setelah Apache XAMPP aktif.</p>
    </main></body>`;
  return false;
}

document.addEventListener('DOMContentLoaded', () => {
  if (!requireLocalhost()) return;
  document.getElementById('login-description').textContent = `Masuk ke akun ${roleLabel} KantinKu`;
  document.getElementById('register-description').textContent = `Buat akun ${roleLabel} untuk mulai ${selectedRole === 'penjual' ? 'menerima pesanan' : 'memesan'}`;
  document.querySelector('#login-box .auth-submit').textContent = `Masuk sebagai ${roleLabel}`;
  document.querySelector('#register-box .auth-submit').textContent = `Daftar sebagai ${roleLabel}`;
  if (authParams.get('view') === 'register') switchAuth('register');
});

document.addEventListener('click', event => {
  const toggle = event.target.closest('[data-password-toggle]');
  if (!toggle) return;
  const input = toggle.closest('.password-input').querySelector('input');
  const visible = input.type === 'text';
  input.type = visible ? 'password' : 'text';
  toggle.classList.toggle('is-visible', !visible);
  toggle.setAttribute('aria-label', visible ? 'Tampilkan password' : 'Sembunyikan password');
});

function showMessage(message, type = 'success', form = document.querySelector('#login-box form')) {
  if (!form) return;

  let element = form.querySelector('.auth-feedback');
  if (!element) {
    element = document.createElement('div');
    element.className = 'auth-feedback';
    form.insertAdjacentElement('afterbegin', element);
  }
  element.className = `auth-feedback is-${type}`;
  element.replaceChildren();
  element.setAttribute('role', type === 'error' ? 'alert' : 'status');
  const messages = String(message).split(/\r?\n/).filter(Boolean);
  if (messages.length > 1) {
    const list = document.createElement('ul');
    messages.forEach(text => {
      const item = document.createElement('li');
      item.textContent = text;
      list.append(item);
    });
    element.append(list);
  } else {
    element.textContent = messages[0] || '';
  }
}

function isValidPassword(password) {
  return /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,}$/.test(password) && !/\s/.test(password);
}

async function requestAuth(data) {
  const response = await fetch('../api/auth.php', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  });
  const contentType = response.headers.get('content-type') || '';
  const result = contentType.includes('application/json')
    ? await response.json()
    : { success: false, message: 'Server tidak mengembalikan respons JSON. Pastikan migrasi database sudah diimpor dan Apache XAMPP aktif.' };
  if (!response.ok || !result.success) {
    const error = new Error(result.message || 'Permintaan akun gagal diproses.');
    error.fields = result.fields;
    throw error;
  }
  return result;
}

function clearFields(form, fields) {
  fields.forEach(name => {
    const input = form.elements.namedItem(name);
    if (!input || !('value' in input)) return;
    input.value = '';
    input.classList.add('is-invalid');
    input.setAttribute('aria-invalid', 'true');
    if (input.type === 'text' && input.closest('.password-input')) {
      input.type = 'password';
      const toggle = input.closest('.password-input').querySelector('[data-password-toggle]');
      toggle?.classList.remove('is-visible');
      toggle?.setAttribute('aria-label', 'Tampilkan password');
    }
  });
}

function resetForm(form) {
  form.reset();
  form.querySelectorAll('.password-input input').forEach(input => {
    input.type = 'password';
    const toggle = input.closest('.password-input').querySelector('[data-password-toggle]');
    toggle?.classList.remove('is-visible');
    toggle?.setAttribute('aria-label', 'Tampilkan password');
  });
  form.querySelectorAll('.is-invalid').forEach(input => {
    input.classList.remove('is-invalid');
    input.removeAttribute('aria-invalid');
  });
}

function rejectForm(form, message, fields) {
  const formFields = Array.from(form.elements)
    .filter(element => element.name && element.type !== 'submit' && element.type !== 'button')
    .map(element => element.name);
  clearFields(form, fields?.length ? fields : formFields);
  showMessage(message, 'error', form);
}

document.addEventListener('input', event => {
  if (!event.target.matches('.auth-card input')) return;
  event.target.classList.remove('is-invalid');
  event.target.removeAttribute('aria-invalid');
});

window.addEventListener('pagehide', () => {
  document.querySelectorAll('.auth-card form').forEach(resetForm);
  document.querySelectorAll('.auth-feedback').forEach(element => element.remove());
});

async function handleRegister(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const username = form.elements.namedItem('username').value.trim();
  const email = form.elements.namedItem('email').value.trim().toLowerCase();
  const password = form.elements.namedItem('password').value;

  const invalidFields = [];
  const errors = [];
  if (!username) {
    invalidFields.push('username');
    errors.push('Username belum diisi.');
  } else if (!/^[a-zA-Z0-9]{4,12}$/.test(username)) {
    invalidFields.push('username');
    errors.push('Username harus 4-12 karakter huruf atau angka.');
  }
  if (!email) {
    invalidFields.push('email');
    errors.push('Email belum diisi.');
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    invalidFields.push('email');
    errors.push('Format email tidak valid. Periksa kembali alamat email.');
  }
  if (!password) {
    invalidFields.push('password');
    errors.push('Password belum diisi.');
  } else if (!isValidPassword(password)) {
    invalidFields.push('password');
    errors.push('Password minimal 8 karakter, mengandung huruf dan angka, serta tidak boleh mengandung spasi.');
  }
  if (invalidFields.length) {
    rejectForm(form, errors.join('\n'), invalidFields);
    return;
  }

  try {
    await requestAuth({ action: 'register', username, email, password, role: selectedRole });
    resetForm(form);
    switchAuth('login');
    showMessage(`Akun ${roleLabel} berhasil dibuat. Silakan masuk.`, 'success', document.querySelector('#login-box form'));
  } catch (error) {
    rejectForm(form, error.message || 'Akun gagal dibuat. Silakan coba kembali.', error.fields);
  }
}

async function handleLogin(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const userOrEmail = form.elements.namedItem('userOrEmail').value.trim();
  const password = form.elements.namedItem('password').value;
  const missingFields = [];
  const missingMessages = [];
  if (!userOrEmail) {
    missingFields.push('userOrEmail');
    missingMessages.push('Username atau email belum diisi.');
  }
  if (!password) {
    missingFields.push('password');
    missingMessages.push('Password belum diisi.');
  }
  if (missingFields.length) {
    rejectForm(form, missingMessages.join('\n'), missingFields);
    return;
  }
  try {
    const result = await requestAuth({ action: 'login', userOrEmail, password, role: selectedRole });
    localStorage.setItem('kantinku_session', JSON.stringify(result.data));
    resetForm(form);
    const allowedPages = selectedRole === 'penjual'
      ? ['../pages/penjual.php']
      : ['../index.html', '../pages/pembeli.html', '../pages/menu.html', '../pages/keranjang.html', '../pages/pembayaran.html', '../pages/status.html'];
    const next = authParams.get('next');
    location.href = allowedPages.includes(next) ? next : (selectedRole === 'penjual' ? '../pages/penjual.php' : '../index.html');
  } catch (error) {
    rejectForm(form, error.message || 'Login gagal. Periksa username/email dan password.', error.fields);
  }
}

async function handleResetPassword(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const email = form.elements.namedItem('resetEmail').value.trim().toLowerCase();
  const username = form.elements.namedItem('resetUsername').value.trim();
  const password = form.elements.namedItem('newPassword').value;
  const invalidFields = [];
  const errors = [];
  if (!username) {
    invalidFields.push('resetUsername');
    errors.push('Username belum diisi.');
  } else if (!/^[a-zA-Z0-9]{4,12}$/.test(username)) {
    invalidFields.push('resetUsername');
    errors.push('Username harus 4-12 karakter huruf atau angka.');
  }
  if (!email) {
    invalidFields.push('resetEmail');
    errors.push('Email belum diisi.');
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    invalidFields.push('resetEmail');
    errors.push('Format email tidak valid. Periksa kembali alamat email.');
  }
  if (!password) {
    invalidFields.push('newPassword');
    errors.push('Password baru belum diisi.');
  } else if (!isValidPassword(password)) {
    invalidFields.push('newPassword');
    errors.push('Password baru minimal 8 karakter, mengandung huruf dan angka, serta tidak boleh mengandung spasi.');
  }
  if (invalidFields.length) {
    rejectForm(form, errors.join('\n'), invalidFields);
    return;
  }

  try {
    const result = await requestAuth({ action: 'reset_password', email, username, password, role: selectedRole });
    resetForm(form);
    switchAuth('login');
    showMessage(result.message, 'success', document.querySelector('#login-box form'));
  } catch (error) {
    rejectForm(form, error.message || `Password akun ${roleLabel} gagal diubah.`, error.fields);
  }
}

function switchAuth(type) {
  document.querySelectorAll('.auth-card form').forEach(resetForm);
  document.getElementById('login-box').hidden = type !== 'login';
  document.getElementById('register-box').hidden = type !== 'register';
  document.getElementById('reset-box').hidden = type !== 'reset';
  document.querySelectorAll('.auth-feedback').forEach(element => element.remove());
  history.replaceState(null, '', location.pathname + location.search);
}