const KantinKu = (() => {
  const sessionKey = 'kantinku_session';
  const ordersKey = 'kantinku_orders';
  const catalogKey = 'kantinku_catalog';
  const storeId = 'kantinku';
  const defaultCatalog = [
    ['nasi-goreng', 'Nasi Goreng', 15000, 'Nasi goreng gurih dengan telur dan ayam.', 'Makanan', '../images/NASI GORENG.jpeg'],
    ['mie-ayam', 'Mie Ayam', 14000, 'Mie lembut dengan ayam gurih dan kuah.', 'Makanan', '../images/MIE AYAM.jpeg'],
    ['pempek', 'Pempek', 15000, 'Pempek ikan dengan kuah cuko khas.', 'Makanan', '../images/PEMPEK.jpeg'],
    ['siomay', 'Siomay', 13000, 'Siomay lengkap dengan saus kacang.', 'Makanan', '../images/SIOMAY.jpeg'],
    ['batagor', 'Batagor', 13000, 'Batagor crispy dengan saus kacang.', 'Makanan', '../images/BATAGOR.jpeg'],
    ['risol-mayo', 'Risol Mayo', 10000, 'Risol renyah berisi mayo dan smoked beef.', 'Makanan', '../images/RISOL MAYO.jpeg'],
    ['ayam-geprek', 'Ayam Geprek', 18000, 'Ayam crispy dengan sambal pedas.', 'Makanan', '../images/GEPREK.jpeg'],
    ['soto-ayam', 'Soto Ayam', 15000, 'Soto ayam hangat dengan kuah gurih.', 'Makanan', '../images/SOTO.jpeg'],
    ['nasi-ayam-bakar', 'Nasi Ayam Bakar', 20000, 'Nasi dengan ayam bakar berbumbu khas.', 'Makanan', '../images/AYAM BAKAR.jpeg'],
    ['kentang-goreng', 'Kentang Goreng', 10000, 'Kentang goreng renyah dengan saus.', 'Makanan', '../images/KENTANG GORENG.jpeg'],
    ['es-teh', 'Es Teh', 5000, 'Es teh manis yang segar.', 'Minuman', '../images/ES TEH.jpeg'],
    ['es-jeruk', 'Es Jeruk', 7000, 'Perasan jeruk segar dengan es.', 'Minuman', '../images/ES JERUK.jpeg'],
    ['es-milo', 'Es Milo', 9000, 'Milo dingin dengan rasa cokelat.', 'Minuman', '../images/milo.jpeg'],
    ['milky-regal', 'Milky Regal', 12000, 'Susu creamy dengan biskuit regal.', 'Minuman', '../images/REGAL.jpeg'],
    ['es-kopi-susu', 'Es Kopi Susu', 12000, 'Kopi susu dingin dengan rasa creamy.', 'Minuman', '../images/KOPI SUSU.jpeg'],
    ['thai-tea', 'Thai Tea', 10000, 'Thai tea manis dan creamy.', 'Minuman', '../images/THAI TEA.jpeg'],
    ['matcha-latte', 'Matcha Latte', 13000, 'Matcha lembut dengan susu.', 'Minuman', '../images/MATCHA.jpeg'],
    ['cappuccino', 'Cappuccino', 14000, 'Kopi cappuccino dengan foam lembut.', 'Minuman', '../images/CAPPUCINO.jpeg'],
    ['chocolate', 'Chocolate', 11000, 'Minuman cokelat manis dan creamy.', 'Minuman', '../images/COKLAT.jpeg'],
    ['jus-alpukat', 'Jus Alpukat', 12000, 'Jus alpukat segar dan lembut.', 'Minuman', '../images/ALPUKAT.jpeg']
  ].map(([id, nama, harga, deskripsi, kategori, gambar]) => ({
    id, nama, harga, deskripsi, kategori, gambar, aktif: true, sellerId: storeId, toko: 'KantinKu'
  }));

  function read(key, fallback) {
    try {
      const value = localStorage.getItem(key);
      return value === null ? fallback : JSON.parse(value);
    } catch {
      return fallback;
    }
  }

  function user() {
    return read(sessionKey, null);
  }

  function cartKey(account = user()) {
    return `kantinku_cart_${encodeURIComponent((account?.email || 'guest').toLowerCase())}`;
  }

  function cart() {
    const stored = read(cartKey(), []);
    const items = Array.isArray(stored) ? stored : Object.values(stored || {});
    return items.map(item => ({
      ...item,
      jumlah: Number(item.jumlah || item.quantity || 1),
      harga: Number(item.harga || 0)
    }));
  }

  function saveCart(items) {
    localStorage.setItem(cartKey(), JSON.stringify(items));
    updateCartBadge();
  }

  function orders() {
    const stored = read(ordersKey, null);
    if (Array.isArray(stored)) return stored;

    const account = user();
    if (!account) return [];

    const legacyActive = read('pesananAktif', null);
    const legacyHistory = read('riwayatPesanan', []);
    const migrated = [legacyActive, ...(Array.isArray(legacyHistory) ? legacyHistory : [])]
      .filter(Boolean)
      .map(order => ({ ...order, userId: account.email.toLowerCase() }));
    localStorage.setItem(ordersKey, JSON.stringify(migrated));
    return migrated;
  }

  function saveOrders(items) {
    localStorage.setItem(ordersKey, JSON.stringify(items));
  }

  function catalog() {
    const stored = read(catalogKey, null);
    if (Array.isArray(stored)) return stored;
    localStorage.setItem(catalogKey, JSON.stringify(defaultCatalog));
    return defaultCatalog.map(item => ({ ...item }));
  }

  function saveCatalog(items) {
    localStorage.setItem(catalogKey, JSON.stringify(items));
  }

  function money(amount) {
    return new Intl.NumberFormat('id-ID', {
      style: 'currency', currency: 'IDR', maximumFractionDigits: 0
    }).format(Number(amount) || 0);
  }

  function notify(message, type = 'info', action = null) {
    let notice = document.getElementById('app-notice');
    if (!notice) {
      notice = document.createElement('section');
      notice.id = 'app-notice';
      notice.className = 'app-notice';
      notice.setAttribute('role', 'status');
      notice.setAttribute('aria-live', 'polite');
      const header = document.querySelector('[data-app-header]');
      (header || document.body).insertAdjacentElement(header ? 'afterend' : 'afterbegin', notice);
    }
    notice.replaceChildren();
    notice.className = `app-notice is-${type}`;
    const text = document.createElement('span');
    text.textContent = message;
    notice.append(text);
    if (action) {
      const link = document.createElement('a');
      link.href = action.href;
      link.textContent = action.label;
      notice.append(link);
    }
    notice.hidden = false;
  }

  function updateCartBadge() {
    const badge = document.getElementById('cart-badge');
    if (!badge) return;
    const total = cart().reduce((sum, item) => sum + item.jumlah, 0);
    badge.textContent = total;
    badge.hidden = total === 0;
  }

  function renderHeader() {
    const header = document.querySelector('[data-app-header]');
    if (!header) return;
    const account = user();
    const page = header.dataset.page;
    const sellerPortal = account?.role === 'penjual' || document.body.dataset.portal === 'penjual';
    const links = sellerPortal
      ? [['../index-penjual.html', 'Beranda Toko', 'seller-home'], ['../pages/penjual.html', 'Dashboard Toko', 'seller']]
      : [['../index.html', 'Beranda', 'home'], ['../pages/menu.html', 'Menu', 'menu'], ['../pages/keranjang.html', 'Keranjang', 'cart'], ['../pages/pembayaran.html', 'Pembayaran', 'payment'], ['../pages/status.html', 'Pesanan Saya', 'status']];

    const homeUrl = sellerPortal ? '../index-penjual.html' : '../index.html';
    header.innerHTML = `
      <a class="logo" href="${homeUrl}" aria-label="KantinKu Beranda"><span>Kantin</span>Ku</a>
      <nav class="nav-links" aria-label="Navigasi utama">
        ${links.map(([href, label, id]) => `<a href="${href}" class="${page === id ? 'active' : ''}">${label}${id === 'cart' ? ' <span id="cart-badge" class="badge" hidden>0</span>' : ''}</a>`).join('')}
      </nav>
      <div class="nav-auth">
        ${account
          ? `<span class="nav-greeting">Hai, ${escapeHtml(account.username)}</span><button class="btn-login" type="button" data-logout>Keluar</button>`
          : sellerPortal
            ? '<a class="btn-login" href="../pages/auth-penjual.html">Masuk / Daftar Penjual</a>'
              : '<a class="btn-login" href="../pages/auth.html">Masuk / Daftar Pembeli</a>'}
      </div>`;
    updateCartBadge();
  }

  function renderHomeActions() {
    const buyerAction = document.getElementById('home-buyer-action');
    if (!buyerAction) return;

    const account = user();
    if (account?.role === 'penjual') {
      location.replace('../index-penjual.html');
      return;
    }

    buyerAction.href = '../pages/menu.html';
    buyerAction.textContent = 'Jelajahi menu';
  }

  function escapeHtml(value) {
    return String(value ?? '').replace(/[&<>"']/g, character => ({
      '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    })[character]);
  }

  function protectPage() {
    const content = document.querySelector('[data-protected-page]');
    if (!content) return;
    const requiredRole = content.dataset.protectedPage;
    const account = user();
    if (account?.role === requiredRole) return;

    content.hidden = true;
    if (!account) {
      const next = encodeURIComponent(location.pathname.split('/').pop() || '../index.html');
      const authPage = requiredRole === 'penjual' ? '../pages/auth-penjual.html' : '../pages/auth.html';
      notify('Silakan masuk atau daftar terlebih dahulu untuk membuka halaman ini.', 'warning', {
        href: `${authPage}?next=${next}`,
        label: `Masuk sebagai ${requiredRole}`
      });
      return;
    }

    notify(`Portal ${requiredRole} terkunci karena sesi ${account.role} masih aktif. Keluar terlebih dahulu untuk berpindah portal.`, 'warning');
  }

  function guardPortalEntry() {
    const portal = document.body.dataset.portal;
    const account = user();
    if (!portal || !account || account.role === portal) return;
    const content = document.querySelector('[data-portal-content]');
    if (content) content.hidden = true;
    notify(`Portal ${portal} terkunci karena sesi ${account.role} masih aktif. Keluar terlebih dahulu untuk berpindah portal.`, 'warning');
  }

  function logout() {
    const account = user();
    localStorage.removeItem(sessionKey);
    location.href = account?.role === 'penjual' ? '../pages/auth-penjual.html' : '../pages/auth.html';
  }

  document.addEventListener('DOMContentLoaded', () => {
    renderHeader();
    guardPortalEntry();
    renderHomeActions();
    protectPage();
    document.addEventListener('click', event => {
      if (event.target.closest('[data-logout]')) logout();
    });
  });

  return { user, cart, saveCart, orders, saveOrders, catalog, saveCatalog, storeId, money, notify, escapeHtml, updateCartBadge };
})();

window.KantinKu = KantinKu;