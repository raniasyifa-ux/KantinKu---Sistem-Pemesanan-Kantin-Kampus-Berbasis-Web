let pendingOrder = null;

document.addEventListener('DOMContentLoaded', () => {
    renderPaymentSummary();
    document.querySelectorAll('input[name="metode"]').forEach(input => {
        input.addEventListener('change', updatePaymentOptions);
    });
    document.getElementById('continue-payment').addEventListener('click', startPayment);
    document.getElementById('confirm-payment').addEventListener('click', confirmPayment);
    document.getElementById('copy-virtual-account').addEventListener('click', copyVirtualAccount);
    document.getElementById('buyer-name').addEventListener('input', event => {
        event.currentTarget.setCustomValidity('');
    });
    document.getElementById('payment-dialog-close').addEventListener('click', () => {
        document.getElementById('payment-dialog').close();
    });
    document.getElementById('back-to-cart').addEventListener('click', () => {
        location.href = '../pages/keranjang.html';
    });
});

function renderPaymentSummary() {
    const items = KantinKu.cart();
    const summary = document.getElementById('ringkasanPesanan');
    const total = items.reduce((sum, item) => sum + item.harga * item.jumlah, 0);
    const safe = KantinKu.escapeHtml;

    if (!items.length) {
        summary.innerHTML = '<p>Keranjang kosong. Tambahkan menu sebelum membayar.</p><a href="../pages/menu.html">Pilih menu</a>';
    } else {
        summary.innerHTML = items.map(item => `
            <div class="order-line">
                <span class="order-line-description"><strong>${safe(item.nama)}</strong><small>${item.jumlah} × ${KantinKu.money(item.harga)}</small></span>
                <strong class="order-line-amount">${KantinKu.money(item.harga * item.jumlah)}</strong>
            </div>`).join('');
    }
    document.getElementById('totalPembayaran').textContent = KantinKu.money(total);
    document.getElementById('continue-payment').disabled = !items.length;
}

function updatePaymentOptions() {
    const method = document.querySelector('input[name="metode"]:checked')?.value;
    document.getElementById('bank-selection').hidden = method !== 'Transfer Bank';
}

function startPayment() {
    const items = KantinKu.cart();
    const method = document.querySelector('input[name="metode"]:checked');
    const bank = document.getElementById('bank-name').value;
    const buyerName = document.getElementById('buyer-name');
    if (!items.length) {
        KantinKu.notify('Keranjang masih kosong. Pilih menu terlebih dahulu.', 'warning', { href: '../pages/menu.html', label: 'Pilih menu' });
        return;
    }
    if (!method) {
        KantinKu.notify('Pilih metode pembayaran sebelum melanjutkan.', 'warning');
        return;
    }
    if (buyerName.value.trim().length < 2) {
        buyerName.setCustomValidity('Nama pembeli minimal 2 karakter dan tidak boleh hanya spasi.');
        buyerName.reportValidity();
        return;
    }
    if (!buyerName.reportValidity()) return;

    const orderId = `ORD-${Date.now().toString(36).toUpperCase()}`;
    const total = items.reduce((sum, item) => sum + item.harga * item.jumlah, 0);
    pendingOrder = {
        id: orderId,
        sellerId: items[0]?.sellerId || KantinKu.storeId,
        toko: items[0]?.toko || 'KantinKu',
        userId: orderId,
        buyerName: buyerName.value.trim(),
        tanggal: new Date().toISOString(),
        items,
        total,
        metode: method.value === 'Transfer Bank' ? `Transfer Bank (${bank})` : method.value,
        paymentStatus: 'Disetujui',
        paymentMode: 'local-unverified',
        paymentUpdates: [],
        status: 'Menunggu konfirmasi',
        updates: [{ status: 'Menunggu konfirmasi', tanggal: new Date().toISOString() }]
    };
    renderPaymentDialog(method.value, bank);
}

function renderPaymentDialog(method, bank) {
    const dialog = document.getElementById('payment-dialog');
    const qrisPanel = document.getElementById('payment-qris-panel');
    const bankPanel = document.getElementById('payment-bank-panel');
    const cashPanel = document.getElementById('payment-cash-panel');
    const heading = document.getElementById('payment-dialog-title');
    const amount = KantinKu.money(pendingOrder.total);
    const safe = KantinKu.escapeHtml;

    qrisPanel.hidden = method !== 'QRIS';
    bankPanel.hidden = method !== 'Transfer Bank';
    cashPanel.hidden = method !== 'Tunai / COD';
    document.getElementById('payment-dialog-order').textContent = pendingOrder.id;
    document.getElementById('payment-buyer-name').textContent = pendingOrder.buyerName;
    document.querySelectorAll('.payment-amount').forEach(element => {
        element.textContent = amount;
    });
    if (method === 'QRIS') {
        heading.textContent = 'Pembayaran QRIS';
        const qrContainer = document.getElementById('payment-qr-code');
        qrContainer.replaceChildren();
        if (typeof QRCode === 'undefined') {
            qrContainer.textContent = 'QR tidak dapat dimuat. Periksa koneksi internet lalu muat ulang halaman.';
        } else {
            new QRCode(qrContainer, {
                text: `KANTINKU-ORDER|ORDER:${pendingOrder.id}|AMOUNT:${pendingOrder.total}`,
                width: 220,
                height: 220,
                colorDark: '#153b32',
                colorLight: '#ffffff'
            });
        }
    } else if (method === 'Transfer Bank') {
        heading.textContent = `Virtual Account ${bank}`;
        const bankCodes = { BCA: '880', BNI: '988', BRI: '887', Mandiri: '70012', Permata: '84' };
        const orderDigits = pendingOrder.id.replace(/\D/g, '').slice(-10).padStart(10, '0');
        document.getElementById('payment-bank-name').textContent = safe(bank);
        document.getElementById('payment-virtual-account').textContent = `${bankCodes[bank]}${orderDigits}`;
    } else {
        heading.textContent = 'Pembayaran tunai';
    }

    dialog.showModal();
}

function confirmPayment() {
    if (!pendingOrder) return;
    const confirmedAt = new Date().toISOString();
    pendingOrder.paymentUpdates.push({
        status: 'Konfirmasi lokal - belum diverifikasi penyedia pembayaran',
        tanggal: confirmedAt
    });
    KantinKu.saveOrders([pendingOrder, ...KantinKu.orders()]);
    KantinKu.saveBuyerOrderId(pendingOrder.id);
    KantinKu.saveCart([]);
    const orderId = pendingOrder.id;
    document.getElementById('payment-dialog').close();
    pendingOrder = null;
    location.href = `../pages/status.html?id=${encodeURIComponent(orderId)}`;
}

async function copyVirtualAccount() {
    const account = document.getElementById('payment-virtual-account').textContent;
    try {
        await navigator.clipboard.writeText(account);
        KantinKu.notify('Nomor Virtual Account berhasil disalin.', 'success');
    } catch {
        const field = document.createElement('textarea');
        field.value = account;
        field.setAttribute('readonly', '');
        field.style.position = 'fixed';
        field.style.opacity = '0';
        document.body.append(field);
        field.select();
        const copied = document.execCommand('copy');
        field.remove();
        if (copied) KantinKu.notify('Nomor Virtual Account berhasil disalin.', 'success');
        else KantinKu.notify('Nomor Virtual Account tidak dapat disalin otomatis. Silakan salin secara manual.', 'warning');
    }
}
