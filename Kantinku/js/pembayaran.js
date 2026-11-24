let pendingOrder = null;

document.addEventListener('DOMContentLoaded', () => {
    renderPaymentSummary();
    paymentErrorElement().hidden = true;
    setupOrderNote();
    document.getElementById('continue-payment').addEventListener('click', startPayment);
    document.getElementById('confirm-payment').addEventListener('click', confirmPayment);
    document.getElementById('payment-dialog-close').addEventListener('click', () => {
        document.getElementById('payment-dialog').close();
    });
    document.getElementById('back-to-cart').addEventListener('click', () => {
        location.href = '../pages/keranjang.html';
    });
});

function paymentErrorElement() {
    let element = document.getElementById('payment-error');
    if (element) return element;

    element = document.createElement('p');
    element.id = 'payment-error';
    element.className = 'payment-error';
    element.setAttribute('role', 'alert');
    element.hidden = true;
    document.getElementById('confirm-payment').before(element);
    return element;
}

function setupOrderNote() {
    const toggle = document.getElementById('note-toggle');
    const panel = document.getElementById('note-panel');
    const textarea = document.getElementById('order-note');
    const counter = document.getElementById('note-count');
    const label = document.getElementById('note-toggle-label');

    function refresh() {
        counter.textContent = textarea.value.length;
        const hasNote = textarea.value.trim().length > 0;
        label.textContent = panel.hidden
            ? (hasNote ? 'Ubah catatan' : 'Tambah catatan (opsional)')
            : 'Sembunyikan catatan';
        document.querySelectorAll('.note-chip').forEach(chip => {
            const active = textarea.value.toLowerCase().includes(chip.dataset.note.toLowerCase());
            chip.classList.toggle('active', active);
            chip.setAttribute('aria-pressed', String(active));
        });
    }

    toggle.addEventListener('click', () => {
        panel.hidden = !panel.hidden;
        toggle.setAttribute('aria-expanded', String(!panel.hidden));
        if (!panel.hidden) textarea.focus();
        refresh();
    });

    document.querySelectorAll('.note-chip').forEach(chip => {
        chip.addEventListener('click', () => {
            const text = chip.dataset.note;
            const current = textarea.value.trim();
            if (current.toLowerCase().includes(text.toLowerCase())) {
                // klik lagi untuk menghapus catatan cepat tersebut
                textarea.value = current
                    .split(',')
                    .map(part => part.trim())
                    .filter(part => part && part.toLowerCase() !== text.toLowerCase())
                    .join(', ');
            } else {
                const next = current ? `${current}, ${text}` : text;
                if (next.length <= textarea.maxLength) textarea.value = next;
            }
            refresh();
        });
    });

    textarea.addEventListener('input', refresh);
    refresh();
}

function getOrderNote() {
    const textarea = document.getElementById('order-note');
    return textarea ? textarea.value.trim().slice(0, 200) : '';
}

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

function startPayment() {
    paymentErrorElement().hidden = true;
    const items = KantinKu.cart();
    const method = document.querySelector('input[name="metode"]:checked');
    const account = KantinKu.user();
    if (!items.length) {
        KantinKu.notify('Keranjang masih kosong. Pilih menu terlebih dahulu.', 'warning', { href: '../pages/menu.html', label: 'Pilih menu' });
        return;
    }
    if (!method) {
        KantinKu.notify('Pilih metode pembayaran sebelum melanjutkan.', 'warning');
        return;
    }
    if (!account || account.role !== 'pembeli') {
        KantinKu.notify('Silakan masuk sebagai pembeli terlebih dahulu.', 'warning');
        return;
    }

    const orderId = `ORD-${Date.now().toString(36).toUpperCase()}`;
    const total = items.reduce((sum, item) => sum + item.harga * item.jumlah, 0);
    pendingOrder = {
        id: orderId,
        sellerId: items[0]?.sellerId || KantinKu.storeId,
        toko: items[0]?.toko || 'KantinKu',
        userId: orderId,
        buyerName: account.username,
        tanggal: new Date().toISOString(),
        items,
        total,
        metode: method.value,
        catatan: getOrderNote(),
        paymentStatus: 'Disetujui',
        paymentMode: 'local-unverified',
        paymentUpdates: [],
        status: 'Menunggu konfirmasi',
        updates: [{ status: 'Menunggu konfirmasi', tanggal: new Date().toISOString() }]
    };
    renderPaymentDialog(method.value);
}

function renderPaymentDialog(method) {
    const dialog = document.getElementById('payment-dialog');
    const qrisPanel = document.getElementById('payment-qris-panel');
    const cashPanel = document.getElementById('payment-cash-panel');
    const heading = document.getElementById('payment-dialog-title');
    const amount = KantinKu.money(pendingOrder.total);

    qrisPanel.hidden = method !== 'QRIS';
    cashPanel.hidden = method !== 'Tunai / COD';
    document.getElementById('payment-dialog-order').textContent = pendingOrder.id;
    document.getElementById('payment-buyer-name').textContent = pendingOrder.buyerName;
    const noteRecap = document.getElementById('payment-note-recap');
    noteRecap.hidden = !pendingOrder.catatan;
    document.getElementById('payment-note-text').textContent = pendingOrder.catatan;
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
    } else {
        heading.textContent = 'Pembayaran tunai';
    }

    dialog.showModal();
}


async function confirmPayment() {
    if (!pendingOrder) return;

    const button = document.getElementById('confirm-payment');
    const buttonLabel = button.querySelector('.confirm-payment-icon').nextElementSibling;
    const errorMessage = paymentErrorElement();
    errorMessage.hidden = true;
    const method = document.querySelector(
        'input[name="metode"]:checked'
    )?.value;

    if (!method) {
        KantinKu.notify(
            'Pilih metode pembayaran terlebih dahulu.',
            'warning'
        );
        return;
    }

    button.disabled = true;
    buttonLabel.textContent = 'Menyimpan pesanan...';

    try {
        const response = await fetch('../api/pesanan.php', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                action: 'buat_pesanan',
                metode_pembayaran: method,
                catatan: pendingOrder.catatan || '',
                items: pendingOrder.items.map(item => ({
                    nama: item.nama,
                    jumlah: Number(item.jumlah)
                }))
            })
        });

        const result = await response.json();

        if (!response.ok || !result.success) {
            throw new Error(
                result.message || 'Pesanan gagal disimpan.'
            );
        }

        const savedOrder = result.data;
        const now = new Date().toISOString();

        const localOrder = {
            ...savedOrder,
            catatan: savedOrder.catatan ?? pendingOrder.catatan ?? '',
            userId: KantinKu.user()?.email || 'buyer',
            sellerId: KantinKu.storeId,
            toko: 'KantinKu',
            paymentMode: 'local-unverified',
            paymentUpdates: [{
                status: savedOrder.paymentStatus,
                tanggal: now
            }],
            updates: [{
                status: savedOrder.status,
                tanggal: now
            }]
        };

        try {
            const existingOrders = KantinKu.orders();
            KantinKu.saveOrders([
                localOrder,
                ...existingOrders.filter(
                    order => String(order.id) !== String(savedOrder.id)
                )
            ]);
            KantinKu.saveBuyerOrderId(String(savedOrder.id));
            KantinKu.saveCart([]);
        } catch (storageError) {
            console.error('Pesanan tersimpan di database, tetapi cache browser gagal diperbarui:', storageError);
        }

        document.getElementById('payment-dialog').close();

        pendingOrder = null;

        location.href =
            `../pages/status.html?token=${encodeURIComponent(savedOrder.trackingToken)}`;

    } catch (error) {
        console.error('Gagal menyimpan pesanan:', error);

        errorMessage.textContent = error instanceof TypeError
            ? 'Tidak dapat terhubung ke server. Buka KantinKu melalui http://localhost/Kantinku dan pastikan Apache serta MySQL XAMPP aktif.'
            : error.message || 'Pesanan gagal disimpan. Silakan coba kembali.';
        errorMessage.hidden = false;
    } finally {
        button.disabled = false;
        buttonLabel.textContent = 'Konfirmasi pesanan';
    }
}