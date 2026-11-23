const fulfillmentSteps = {
  'Menunggu konfirmasi': 'Diproses',
  'Diproses': 'Siap diambil',
  'Siap diambil': 'Selesai'
};
let pendingMenuRemoval = null;
let sellerCatalog = [];
let refreshingSellerOrders = false;

document.addEventListener('DOMContentLoaded', () => {
  renderSellerDashboard();
  window.setInterval(() => {
    if (document.visibilityState === 'visible') renderSellerOrders();
  }, 5000);
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') renderSellerOrders();
  });
  document.getElementById('order-search').addEventListener('input', renderSellerDashboard);
  document.getElementById('status-filter').addEventListener('change', renderSellerDashboard);
  document.getElementById('catalog-search').addEventListener('input', renderSellerCatalog);
  document.getElementById('catalog-category').addEventListener('change', renderSellerCatalog);
  document.querySelectorAll('#add-menu-button, [data-add-menu]').forEach(button => {
    button.addEventListener('click', () => openMenuEditor());
  });
  document.getElementById('seller-menu-close').addEventListener('click', closeMenuEditor);
  document.getElementById('seller-menu-cancel').addEventListener('click', closeMenuEditor);
  document.getElementById('seller-menu-form').addEventListener('submit', saveMenuFromForm);
  document.querySelectorAll('[data-seller-view]').forEach(button => button.addEventListener('click', () => switchSellerView(button.dataset.sellerView)));
  document.addEventListener('click', handleSellerAction);
  window.addEventListener('storage', renderSellerDashboard);
});

function switchSellerView(view) {
  document.querySelectorAll('[data-seller-view]').forEach(button => {
    const active = button.dataset.sellerView === view;
    button.classList.toggle('active', active);
    button.setAttribute('aria-selected', String(active));
  });
  document.querySelectorAll('[data-seller-panel]').forEach(panel => {
    panel.hidden = panel.dataset.sellerPanel !== view;
  });
}

function renderSellerDashboard() {
  renderSellerOrders();
  KantinKu.loadCatalog('../api/menu.php')
    .then(items => {
      sellerCatalog = items;
      renderSellerCatalog();
    })
    .catch(error => {
      document.getElementById('seller-catalog').innerHTML = '<p class="empty-state">Menu gagal dimuat. Periksa koneksi database dan API.</p>';
      console.error('Gagal memuat katalog penjual:', error);
    });
}


async function renderSellerOrders() {
    if (refreshingSellerOrders) return;
    refreshingSellerOrders = true;
    const container = document.getElementById('seller-orders');

    container.innerHTML = '<p class="empty-state">Memuat pesanan...</p>';

    try {
        const response = await fetch('../api/pesanan.php');
        const result = await response.json();

        if (!response.ok || !result.success) {
            throw new Error(result.message || 'Gagal mengambil data pesanan.');
        }

        const allOrders = result.data || [];
        const search = document.getElementById('order-search').value
            .trim().toLocaleLowerCase('id-ID');
        const selectedStatus = document.getElementById('status-filter').value;

        const orders = allOrders.filter(order => {
            const searchable = [
                order.id,
                order.buyerName,
                ...(order.items || []).map(item => item.nama)
            ].join(' ').toLocaleLowerCase('id-ID');

            return searchable.includes(search)
                && (selectedStatus === 'semua' || order.status === selectedStatus);
        });

        document.getElementById('count-new').textContent =
            allOrders.filter(order => order.status === 'Menunggu konfirmasi').length;

        document.getElementById('count-payment').textContent =
            allOrders.filter(order =>
                ['Menunggu verifikasi', 'Menunggu pembayaran di tempat']
                    .includes(order.paymentStatus)
            ).length;

        document.getElementById('count-progress').textContent =
            allOrders.filter(order =>
                ['Diproses', 'Siap diambil'].includes(order.status)
            ).length;

        document.getElementById('count-done').textContent =
            allOrders.filter(order => order.status === 'Selesai').length;

        document.getElementById('seller-order-count').textContent =
            `${orders.length} pesanan`;

        document.getElementById('tab-order-count').textContent = allOrders.length;

        container.innerHTML = orders.length
            ? orders.map(renderSellerOrder).join('')
            : '<p class="empty-state">Belum ada pesanan yang cocok.</p>';

    } catch (error) {
        container.innerHTML =
            '<p class="empty-state">Pesanan gagal dimuat. Periksa koneksi database dan API.</p>';

        console.error('Gagal memuat pesanan:', error);
    }
    refreshingSellerOrders = false;
}


function belongsToStore(order) {
  if (order.sellerId) return order.sellerId === KantinKu.storeId;
  return !Array.isArray(order.items) || order.items.some(item => !item.sellerId || item.sellerId === KantinKu.storeId);
}

function getPaymentStatus(order) {
  if (order.paymentStatus) return order.paymentStatus;
  return order.metode === 'Tunai' ? 'Menunggu pembayaran di tempat' : 'Menunggu verifikasi';
}

function renderSellerOrder(order) {
  const safe = KantinKu.escapeHtml;
  const items = (order.items || []).map(item => `<li><span class="seller-item-name">${safe(item.nama)}</span><span class="seller-item-quantity">×${Number(item.jumlah)}</span><strong>${KantinKu.money(Number(item.harga) * Number(item.jumlah))}</strong></li>`).join('');
  const updates = (order.updates || []).map(update => `<li><span>${safe(update.status)}</span><time>${formatSellerDate(update.tanggal)}</time></li>`).join('');
  const paymentUpdates = (order.paymentUpdates || []).map(update => `<li><span>${safe(update.status)}</span><time>${formatSellerDate(update.tanggal)}</time></li>`).join('');
  const paymentStatus = getPaymentStatus(order);
  let paymentAction = '';
  if (paymentStatus === 'Menunggu verifikasi') {
    paymentAction = `<button class="seller-action-button" type="button" data-seller-action="approve-payment" data-order-id="${safe(order.id)}">Setujui pembayaran &amp; terima pesanan</button>`;
  } else if (paymentStatus === 'Menunggu pembayaran di tempat') {
    paymentAction = `<button class="seller-action-button" type="button" data-seller-action="confirm-cash" data-order-id="${safe(order.id)}">Konfirmasi uang &amp; terima pesanan</button>`;
  }

  const nextStatus = fulfillmentSteps[order.status];
  const fulfillmentAction = nextStatus && ['Disetujui', 'Berhasil'].includes(paymentStatus)
   ? `<button class="seller-action-button is-secondary" type="button" data-seller-action="advance-order" data-order-id="${safe(order.id)}" data-current-status="${safe(order.status)}">${nextStatus === 'Diproses' ? 'Terima pesanan' : nextStatus === 'Siap diambil' ? 'Tandai siap diambil' : 'Selesaikan pesanan'}</button>`    : '';

  const paymentLabel = paymentStatus === 'Disetujui' && ['simulasi', 'local-unverified'].includes(order.paymentMode)
    ? 'Menunggu verifikasi pembayaran'
    : paymentStatus;

  return `<article class="seller-order-card">
    <header class="seller-order-heading"><div><p class="eyebrow">${safe(order.id)}</p><strong>${safe(order.buyerName || 'Pembeli')}</strong><small>${formatSellerDate(order.tanggal)}</small></div><span class="order-status">${safe(order.status || 'Menunggu konfirmasi')}</span></header>
    <p class="seller-buyer-contact">Identitas pembeli: nomor pesanan</p>
    <ul class="seller-order-items">${items}</ul>
    <div class="seller-payment-row"><div class="seller-payment-method"><span>Metode pembayaran</span><strong>${safe(order.metode || '-')}</strong></div><div class="seller-payment-total"><span>Total pesanan</span><strong>${KantinKu.money(order.total)}</strong></div></div>
    <div class="seller-payment-status-line"><span>Status pembayaran</span><strong class="payment-status-label">${safe(paymentLabel)}</strong><small>Tanggal: ${formatSellerDate(order.paymentDate)}</small></div>
    <details class="seller-order-history"><summary>Lihat progres pesanan dan pembayaran</summary><div class="seller-history-columns"><div><h3>Status pesanan</h3><ol>${updates || '<li>Pesanan dibuat</li>'}</ol></div><div><h3>Status pembayaran</h3><ol>${paymentUpdates || `<li>${safe(paymentStatus)}</li>`}</ol></div></div></details>
    <div class="seller-order-actions">${paymentAction}${fulfillmentAction}</div>
  </article>`;
}

function formatSellerDate(value) {
  return value ? new Date(value).toLocaleString('id-ID', { dateStyle: 'medium', timeStyle: 'short' }) : '-';
}

function renderSellerCatalog() {
  const catalog = sellerCatalog;
  const query = document.getElementById('catalog-search').value.trim().toLocaleLowerCase('id-ID');
  const category = document.getElementById('catalog-category').value;
  const filtered = catalog.filter(item => {
    const text = `${item.nama} ${item.deskripsi || ''}`.toLocaleLowerCase('id-ID');
    return text.includes(query) && (category === 'Semua' || item.kategori === category);
  });
  document.getElementById('tab-menu-count').textContent = catalog.length;
  document.getElementById('catalog-result-count').textContent = `${filtered.length} dari ${catalog.length} menu`;
  document.getElementById('seller-catalog').innerHTML = filtered.length
    ? filtered.map(renderSellerMenu).join('')
    : catalog.length
      ? '<p class="empty-state">Tidak ada menu yang cocok dengan pencarian atau kategori.</p>'
      : '<p class="empty-state">Katalog belum berisi menu. Gunakan tombol Tambah menu.</p>';
}

function renderSellerMenu(item) {
  const safe = KantinKu.escapeHtml;
  const actions = pendingMenuRemoval === item.id
    ? `<span class="remove-confirmation">Hapus ${safe(item.nama)}?</span><button type="button" data-seller-action="confirm-remove-menu" data-menu-id="${safe(item.id)}">Ya, hapus</button><button type="button" data-seller-action="cancel-remove-menu" data-menu-id="${safe(item.id)}">Batal</button>`
    : `<button type="button" data-seller-action="edit-menu" data-menu-id="${safe(item.id)}">Edit</button><button type="button" data-seller-action="toggle-menu" data-menu-id="${safe(item.id)}">${item.aktif === false ? 'Tampilkan' : 'Sembunyikan'}</button><button type="button" data-seller-action="remove-menu" data-menu-id="${safe(item.id)}">Hapus</button>`;
  return `<article class="seller-menu-row ${item.aktif === false ? 'is-inactive' : ''}">
    <img src="${safe(encodeURI(item.gambar || ''))}" alt="${safe(item.nama)}" loading="lazy">
    <div class="seller-menu-copy"><strong>${safe(item.nama)}</strong><span>${safe(item.kategori)} · ${KantinKu.money(item.harga)}</span><small>${safe(item.deskripsi || '')}</small></div>
    <span class="menu-availability">${item.aktif === false ? 'Disembunyikan' : 'Tayang'}</span>
    <div class="seller-menu-actions">${actions}</div>
  </article>`;
}

function openMenuEditor(item = null) {
  const form = document.getElementById('seller-menu-form');
  form.reset();
  form.elements.namedItem('id').value = item?.id || '';
  form.elements.namedItem('nama').value = item?.nama || '';
  form.elements.namedItem('deskripsi').value = item?.deskripsi || '';
  form.elements.namedItem('harga').value = item?.harga || '';
  form.elements.namedItem('kategori').value = item?.kategori || 'Makanan';
  form.elements.namedItem('gambar').value = item?.gambar || '';
  document.getElementById('seller-menu-dialog-title').textContent = item ? 'Edit menu' : 'Tambah menu';
  document.getElementById('seller-menu-dialog').showModal();
}

function closeMenuEditor() {
  document.getElementById('seller-menu-dialog').close();
}

async function saveMenuFromForm(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const id = form.elements.namedItem('id').value;
  const name = form.elements.namedItem('nama').value.trim();
  const imageName = form.elements.namedItem('gambar').value.trim();
  if (imageName && /[\\/]/.test(imageName)) {
    KantinKu.notify('Masukkan nama file gambar saja, tanpa folder atau alamat web.', 'warning');
    return;
  }
  const previous = sellerCatalog.find(item => item.id === id);
  const updated = {
    action: 'save',
    id,
    nama: name,
    deskripsi: form.elements.namedItem('deskripsi').value.trim(),
    harga: Number(form.elements.namedItem('harga').value),
    kategori: form.elements.namedItem('kategori').value,
    gambar: imageName || (previous?.gambar ? previous.gambar.split('/').pop() : '')
  };
  try {
    const response = await fetch('../api/menu.php', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(updated)
    });
    const result = await response.json();
    if (!response.ok || !result.success) {
      throw new Error(result.message || 'Menu gagal disimpan.');
    }
    sellerCatalog = await KantinKu.loadCatalog('../api/menu.php');
    closeMenuEditor();
    renderSellerCatalog();
    KantinKu.notify(`Menu ${name} berhasil ${previous ? 'diperbarui' : 'ditambahkan'}.`, 'success');
  } catch (error) {
    KantinKu.notify(error.message || 'Menu gagal disimpan.', 'warning');
  }
}

function updatePayment(order, status) {
  order.paymentStatus = status;
  order.paymentUpdates = Array.isArray(order.paymentUpdates) ? order.paymentUpdates : [];
  order.paymentUpdates.push({ status, tanggal: new Date().toISOString() });
}

function handleSellerAction(event) {
  const button = event.target.closest('[data-seller-action]');
  if (!button) return;
  const action = button.dataset.sellerAction;
  const orderId = button.dataset.orderId;
  
if (orderId) {
    const orders = KantinKu.orders();
    const order = orders.find(item => String(item.id) === String(orderId));

    // Status pesanan diambil dari database; jangan bergantung pada
    // salinan localStorage yang mungkin sudah tidak terbaru.
    const buttonAction = button.dataset.sellerAction;
    let jenis = '';
    let status = '';

    if (buttonAction === 'approve-payment') {
        jenis = 'pembayaran';
        status = 'Disetujui';
    } else if (buttonAction === 'confirm-cash') {
        jenis = 'pembayaran';
        status = 'Berhasil';
    } else if (buttonAction === 'advance-order') {
        const nextStatus = {
            'Menunggu konfirmasi': 'Diproses',
            'Diproses': 'Siap diambil',
            'Siap diambil': 'Selesai'
        };

        jenis = 'pesanan';
        status = nextStatus[button.dataset.currentStatus] || '';
    }

    if (!jenis || !status) {
        // Status berikutnya dihitung ulang berdasarkan data terbaru
        // jika tombol lama belum memiliki data status.
        if (buttonAction === 'advance-order') {
            renderSellerOrders();
            KantinKu.notify(
                'Muat ulang daftar pesanan sebelum memperbarui status.',
                'warning'
            );
        }
        return;
    }

    button.disabled = true;

    fetch('../api/pesanan.php', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            action: 'ubah_status',
            id_pemesanan: Number(orderId),
            jenis,
            status
        })
    })
    .then(async response => {
        const result = await response.json();

        if (!response.ok || !result.success) {
            throw new Error(result.message || 'Status gagal diperbarui.');
        }

        await renderSellerOrders();
        KantinKu.notify('Status berhasil diperbarui di database.', 'success');
    })
    .catch(error => {
        KantinKu.notify(error.message || 'Gagal memperbarui status.', 'warning');
    })
    .finally(() => {
        button.disabled = false;
    });

    return;
}


  const catalog = sellerCatalog;
  const menu = catalog.find(item => item.id === button.dataset.menuId);
  if (!menu) return;
  if (action === 'edit-menu') openMenuEditor(menu);
  if (action === 'remove-menu') {
    pendingMenuRemoval = menu.id;
    renderSellerCatalog();
  }
  if (action === 'cancel-remove-menu') {
    pendingMenuRemoval = null;
    renderSellerCatalog();
  }
  if (action === 'confirm-remove-menu') {
    button.disabled = true;
    fetch('../api/menu.php', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ action: 'delete', id: menu.id })
    })
      .then(async response => {
        const result = await response.json();
        if (!response.ok || !result.success) throw new Error(result.message || 'Menu gagal dihapus.');
        sellerCatalog = await KantinKu.loadCatalog('../api/menu.php');
        pendingMenuRemoval = null;
        renderSellerCatalog();
        KantinKu.notify(`Menu ${menu.nama} berhasil dihapus.`, 'success');
      })
      .catch(error => KantinKu.notify(error.message || 'Menu gagal dihapus.', 'warning'))
      .finally(() => { button.disabled = false; });
  }
  if (action === 'toggle-menu') {
    button.disabled = true;
    fetch('../api/menu.php', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ action: 'availability', id: menu.id, aktif: menu.aktif === false })
    })
      .then(async response => {
        const result = await response.json();
        if (!response.ok || !result.success) throw new Error(result.message || 'Status menu gagal diperbarui.');
        sellerCatalog = await KantinKu.loadCatalog('../api/menu.php');
        renderSellerCatalog();
        KantinKu.notify(`Menu ${menu.nama} berhasil ${menu.aktif === false ? 'ditampilkan' : 'disembunyikan'}.`, 'success');
      })
      .catch(error => KantinKu.notify(error.message || 'Status menu gagal diperbarui.', 'warning'))
      .finally(() => { button.disabled = false; });
  }
}