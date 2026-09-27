document.addEventListener('DOMContentLoaded', renderOrders);
window.addEventListener('storage', event => {
  if (event.key === 'kantinku_orders') renderOrders();
});

function formatOrderDate(value) {
  return new Date(value).toLocaleString('id-ID', { dateStyle: 'medium', timeStyle: 'short' });
}

function renderOrders() {
  const account = KantinKu.user();
  if (!account || account.role !== 'pembeli') return;
  const orders = KantinKu.orders()
    .filter(order => order.userId === account.email.toLowerCase())
    .sort((first, second) => new Date(second.tanggal) - new Date(first.tanggal));
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
    ? `Pesanan terakhir: ${orders[0].id} · ${orders[0].status}`
    : 'Belum ada pesanan. Pilih menu untuk memulai.';
  document.getElementById('statusPesanan').textContent = orders[0]?.status || 'Belum ada pesanan';
}

function renderOrderCard(order) {
  const safe = KantinKu.escapeHtml;
  const lines = order.items.map(item => `<li>${safe(item.nama)} × ${item.jumlah}</li>`).join('');
  const updates = (order.updates || []).map(update => `<li><span>${safe(update.status)}</span><time>${formatOrderDate(update.tanggal)}</time></li>`).join('');
  return `<article class="order-card">
    <div class="order-card-heading"><div><strong>${safe(order.id)}</strong><small>${formatOrderDate(order.tanggal)}</small></div><span class="order-status">${safe(order.status)}</span></div>
    <p class="buyer-payment-status">Pembayaran: <strong>${safe(order.paymentStatus || 'Menunggu verifikasi')}</strong></p>
    <ul class="order-items">${lines}</ul>
    <details><summary>Lacak pesanan</summary><ol class="order-timeline">${updates}</ol></details>
    <div class="order-card-total"><span>${safe(order.metode || '-')}</span><strong>${KantinKu.money(order.total)}</strong></div>
  </article>`;
}