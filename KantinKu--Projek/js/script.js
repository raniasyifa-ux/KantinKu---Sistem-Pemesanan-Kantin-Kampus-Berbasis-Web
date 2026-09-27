document.addEventListener('DOMContentLoaded', () => {
  updateCartBadge();
  checkLoginStatus();
});

function getKeranjang() {
  const stored = JSON.parse(localStorage.getItem('keranjang'));

  if (!stored) return [];
  if (Array.isArray(stored)) return stored;

  return Object.values(stored).map(item => ({
    ...item,
    jumlah: Number(item.jumlah || item.quantity || 1),
    harga: Number(item.harga || 0)
  }));
}

// Memperbarui indikator jumlah item pada badge navbar
function updateCartBadge() {
  const keranjang = getKeranjang();
  const totalItems = keranjang.reduce((sum, item) => sum + (item.jumlah || item.quantity || 1), 0);

  const cartBadge = document.getElementById('cart-badge');
  if (cartBadge) {
    cartBadge.textContent = totalItems;
    cartBadge.style.display = totalItems > 0 ? 'inline-block' : 'none';
  }
}

// Memeriksa status login sesi pengguna
function checkLoginStatus() {
  const currentUser = JSON.parse(localStorage.getItem('kantinku_session'));
  const authContainer = document.querySelector('.nav-auth');

  if (currentUser && authContainer) {
    authContainer.innerHTML = `
      <span style="color: var(--text-color); font-weight: 600; margin-right: 10px;">
        Hai, ${currentUser.username}
      </span>
      <button id="btn-logout" class="btn-login" style="cursor: pointer; background: transparent;">Logout</button>
    `;

    document.getElementById('btn-logout').addEventListener('click', () => {
      localStorage.removeItem('kantinku_session');
      window.location.reload();
    });
  }
}