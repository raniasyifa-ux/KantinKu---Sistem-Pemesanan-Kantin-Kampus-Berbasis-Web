document.addEventListener('DOMContentLoaded', renderPaymentSummary);

function renderPaymentSummary() {
    const items = KantinKu.cart();
    const summary = document.getElementById('ringkasanPesanan');
    const total = items.reduce((sum, item) => sum + item.harga * item.jumlah, 0);
    const safe = KantinKu.escapeHtml;

    if (!items.length) {
        summary.innerHTML = '<p>Keranjang kosong. Tambahkan menu sebelum membayar.</p><a href="menu.html">Pilih menu</a>';
    } else {
        summary.innerHTML = items.map(item => `
            <div class="order-line">
                <span class="order-line-description"><strong>${safe(item.nama)}</strong><small>${item.jumlah} × ${KantinKu.money(item.harga)}</small></span>
                <strong class="order-line-amount">${KantinKu.money(item.harga * item.jumlah)}</strong>
            </div>`).join('');
    }
    document.getElementById('totalPembayaran').textContent = KantinKu.money(total);
}

function konfirmasiPembayaran() {
    const account = KantinKu.user();
    const items = KantinKu.cart();
    const method = document.querySelector('input[name="metode"]:checked');
    if (!items.length) {
        KantinKu.notify('Keranjang masih kosong. Pilih menu terlebih dahulu.', 'warning', { href: 'menu.html', label: 'Pilih menu' });
        return;
    }
    if (!method) {
        KantinKu.notify('Pilih metode pembayaran sebelum melanjutkan.', 'warning');
        return;
    }

    const order = {
        id: `ORD-${Date.now().toString(36).toUpperCase()}`,
        sellerId: items[0]?.sellerId || KantinKu.storeId,
        toko: items[0]?.toko || 'KantinKu',
        userId: account.email.toLowerCase(),
        buyerName: account.username,
        tanggal: new Date().toISOString(),
        items,
        total: items.reduce((sum, item) => sum + item.harga * item.jumlah, 0),
        metode: method.value,
        paymentStatus: method.value === 'Tunai' ? 'Menunggu pembayaran di tempat' : 'Menunggu verifikasi',
        paymentUpdates: [{
            status: method.value === 'Tunai' ? 'Menunggu pembayaran di tempat' : 'Menunggu verifikasi',
            tanggal: new Date().toISOString()
        }],
        status: 'Menunggu konfirmasi',
        updates: [{ status: 'Menunggu konfirmasi', tanggal: new Date().toISOString() }]
    };
    KantinKu.saveOrders([order, ...KantinKu.orders()]);
    KantinKu.saveCart([]);
    location.href = 'status.html';
}

function kembaliKeHome() {
    location.href = 'keranjang.html';
}