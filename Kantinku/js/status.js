document.addEventListener('DOMContentLoaded', renderOrders);
window.setInterval(() => {
  if (document.visibilityState === 'visible') renderOrders(true);
}, 5000);
document.addEventListener('visibilitychange', () => {
  if (document.visibilityState === 'visible') renderOrders(true);
});
window.addEventListener('storage', event => {
  if (event.key === 'kantinku_orders') renderOrders();
});

function formatOrderDate(value) {
  return new Date(value).toLocaleString('id-ID', { dateStyle: 'medium', timeStyle: 'short' });
}

let refreshingOrders = false;
async function renderOrders(silent = false) {
  if (refreshingOrders) return;
  refreshingOrders = true;
  let orders = [];
  try {
      const response = await fetch('../api/pesanan.php');
      const result = await response.json();
      if (!response.ok || !result.success) {
        throw new Error(result.message || 'Pesanan gagal dimuat dari database.');
      }
      orders = result.data;
  } catch (error) {
      document.getElementById('detailPesanan').textContent = 'Pesanan gagal dimuat. Periksa koneksi database lalu muat ulang halaman.';
      document.getElementById('pesananAktif').replaceChildren();
      document.getElementById('riwayatPesanan').replaceChildren();
      if (!silent) KantinKu.notify(error.message || 'Pesanan gagal dimuat dari database.', 'warning');
      console.error('Gagal memuat pesanan pembeli:', error);
    refreshingOrders = false;
    return;
  }
  orders.sort((first, second) => new Date(second.tanggal) - new Date(first.tanggal));
  const active = orders.filter(order => order.status !== 'Selesai');
  const history = orders.filter(order => order.status === 'Selesai');
  const activeCount = document.getElementById('buyer-active-count');
  const historyCount = document.getElementById('buyer-history-count');
  const totalSpent = document.getElementById('buyer-total-spent');
  if (activeCount) activeCount.textContent = active.length;
  if (historyCount) historyCount.textContent = history.length;
  if (totalSpent) totalSpent.textContent = KantinKu.money(orders.reduce((sum, order) => sum + Number(order.total || 0), 0));
  document.getElementById('pesananAktif').innerHTML = active.length
    ? active.map(renderOrderCard).join('')
    : '<p>Belum ada pesanan yang sedang berjalan.</p>';
  document.getElementById('riwayatPesanan').innerHTML = history.length
    ? history.map(renderOrderCard).join('')
    : '<p>Riwayat pesanan selesai akan muncul di sini.</p>';
  document.getElementById('detailPesanan').textContent = orders.length
    ? `Nomor pesanan: ${orders[0].id} · ${orders[0].status}`
    : 'Belum ada pesanan. Pilih menu untuk memulai.';
  document.getElementById('statusPesanan').textContent = orders[0]
    ? `${orders[0].status} · ${orders[0].paymentStatus || 'Menunggu konfirmasi'}`
    : 'Belum ada pesanan';
  refreshingOrders = false;
}

function renderOrderCard(order) {
  const safe = KantinKu.escapeHtml;
  const lines = order.items.map(item => `<li>${safe(item.nama)} × ${item.jumlah}</li>`).join('');
  const updates = [
    ...(order.updates || []).map(update => ({ ...update, type: 'Pesanan' })),
    ...(order.paymentUpdates || []).map(update => ({ ...update, type: 'Pembayaran' }))
  ].sort((first, second) => new Date(first.tanggal) - new Date(second.tanggal))
    .map(update => `<li><span>${safe(update.type)}: ${safe(update.status)}</span><time>${formatOrderDate(update.tanggal)}</time></li>`).join('');
  return `<article class="order-card">
    <div class="order-card-heading"><div><strong>${safe(order.id)}</strong><small>${formatOrderDate(order.tanggal)}</small></div><span class="order-status">${safe(order.status)}</span></div>
    <p class="buyer-payment-status">Status pembayaran: <strong>${safe(order.paymentStatus || 'Menunggu verifikasi')}</strong><small> Dibayar: ${order.paymentDate ? formatOrderDate(order.paymentDate) : '-'}</small></p>
    <ul class="order-items">${lines}</ul>
    <details><summary>Lacak pesanan dan pembayaran</summary><ol class="order-timeline">${updates}</ol></details>
    <div class="order-card-total"><span>${safe(order.metode || '-')}</span><strong>${KantinKu.money(order.total)}</strong></div>
  </article>`;
}
