
document.addEventListener('DOMContentLoaded', () => {
    renderPaymentSummary();
    setupPaymentOptions();
});

const demoVirtualAccounts = {
    BCA: '8808 1234 5678 9012',
    BNI: '8809 1234 5678 9012',
    BRI: '8810 1234 5678 9012',
    Mandiri: '8811 1234 5678 9012'
};

function renderPaymentSummary() {
    const items = KantinKu.cart();
    const summary = document.getElementById('ringkasanPesanan');
    const total = items.reduce(
        (sum, item) => sum + Number(item.harga) * Number(item.jumlah),
        0
    );

    const safe = KantinKu.escapeHtml;

    if (!items.length) {
        summary.innerHTML = `
            <p>Keranjang kosong. Tambahkan menu sebelum membayar.</p>
            <a href="../pages/menu.html">Pilih menu</a>
        `;
    } else {
        summary.innerHTML = items.map(item => `
            <div class="order-line">
                <span class="order-line-description">
                    <strong>${safe(item.nama)}</strong>
                    <small>
                        ${Number(item.jumlah)} × ${KantinKu.money(item.harga)}
                    </small>
                </span>

                <strong class="order-line-amount">
                    ${KantinKu.money(Number(item.harga) * Number(item.jumlah))}
                </strong>
            </div>
        `).join('');
    }

    document.getElementById('totalPembayaran').textContent =
        KantinKu.money(total);
}

function setupPaymentOptions() {
    const bankDetails = document.getElementById('bank-payment-details');
    const qrisDetails = document.getElementById('qris-payment-details');
    const confirmButton = document.getElementById('confirm-payment-button');
    const bankSelect = document.getElementById('bank-select');
    const vaResult = document.getElementById('va-result');
    const vaNumber = document.getElementById('va-number');
    const copyStatus = document.getElementById('va-copy-status');

    document.querySelectorAll('input[name="metode"]').forEach(input => {
        input.addEventListener('change', () => {
            bankDetails.hidden = input.value !== 'Transfer Bank';
            qrisDetails.hidden = input.value !== 'QRIS';
            confirmButton.hidden = input.value === 'QRIS';

            if (input.value === 'QRIS') {
                renderQrisPreview();
            } else if (input.value === 'Transfer Bank') {
                renderVirtualAccount();
            }
        });
    });

    bankSelect.addEventListener('change', renderVirtualAccount);

    function renderVirtualAccount() {
        const accountNumber = demoVirtualAccounts[bankSelect.value];

        vaNumber.textContent = accountNumber || '';
        vaResult.hidden = !accountNumber;
    }

    document.getElementById('copy-va-button').addEventListener('click', async () => {
        const value = vaNumber.textContent.replace(/\s/g, '');

        if (!value) {
            KantinKu.notify('Pilih bank terlebih dahulu.', 'warning');
            return;
        }

        let copied = false;
        try {
            if (navigator.clipboard?.writeText) {
                await navigator.clipboard.writeText(value);
                copied = true;
            }
        } catch {
            copied = false;
        }

        if (!copied) {
            const temporary = document.createElement('textarea');
            temporary.value = value;
            temporary.setAttribute('readonly', '');
            temporary.style.position = 'fixed';
            temporary.style.top = '0';
            temporary.style.left = '-9999px';
            document.body.appendChild(temporary);
            temporary.select();
            temporary.setSelectionRange(0, value.length);

            try {
                copied = document.execCommand('copy');
            } catch {
                copied = false;
            } finally {
                temporary.remove();
            }
        }

        const message = copied
            ? 'Nomor VA berhasil disalin ke clipboard.'
            : 'VA belum tersalin. Pilih nomor VA, lalu salin secara manual.';
        copyStatus.textContent = message;
        copyStatus.hidden = false;
        KantinKu.notify(
            message,
            copied ? 'success' : 'warning'
        );
    });

    document.getElementById('qris-continue').addEventListener('click', () => {
        simpanPesanan('QRIS');
    });
}

function renderQrisPreview() {
    const size = 29;
    const modules = Array.from({ length: size }, () => Array(size).fill(false));
    let seed = KantinKu.cart().reduce(
        (total, item) => total + Number(item.harga) * Number(item.jumlah),
        0
    ) || 1;

    const nextBit = () => {
        seed = (seed * 9301 + 49297) % 233280;
        return seed / 233280 > 0.5;
    };

    for (let row = 0; row < size; row += 1) {
        for (let column = 0; column < size; column += 1) {
            modules[row][column] = nextBit();
        }
    }

    [[0, 0], [0, size - 7], [size - 7, 0]].forEach(([top, left]) => {
        for (let row = 0; row < 7; row += 1) {
            for (let column = 0; column < 7; column += 1) {
                modules[top + row][left + column] =
                    row === 0 || row === 6 || column === 0 || column === 6 ||
                    (row >= 2 && row <= 4 && column >= 2 && column <= 4);
            }
        }
    });

    const squares = [];
    modules.forEach((row, y) => row.forEach((filled, x) => {
        if (filled) squares.push(`<rect x="${x}" y="${y}" width="1" height="1"/>`);
    }));

    document.getElementById('qris-code').innerHTML = `
        <svg viewBox="-2 -2 ${size + 4} ${size + 4}" xmlns="http://www.w3.org/2000/svg" aria-hidden="true" focusable="false">
            <rect x="-2" y="-2" width="${size + 4}" height="${size + 4}" fill="#fff"/>
            <g fill="#4C4541">${squares.join('')}</g>
        </svg>
    `;
}

function konfirmasiPembayaran() {
    const items = KantinKu.cart();
    const method = document.querySelector('input[name="metode"]:checked');

    if (!items.length) {
        KantinKu.notify(
            'Keranjang masih kosong. Pilih menu terlebih dahulu.',
            'warning',
            {
                href: '../pages/menu.html',
                label: 'Pilih menu'
            }
        );
        return;
    }

    if (!method) {
        KantinKu.notify('Pilih metode pembayaran sebelum melanjutkan.', 'warning');
        return;
    }

    if (
        method.value === 'Transfer Bank' &&
        !document.getElementById('bank-select').value
    ) {
        KantinKu.notify('Pilih bank tujuan terlebih dahulu.', 'warning');
        document.getElementById('bank-select').focus();
        return;
    }

    if (method.value === 'QRIS') {
        document.getElementById('qris-payment-details').scrollIntoView({ behavior: 'smooth' });
        return;
    }

    simpanPesanan(method.value);
}

function simpanPesanan(method) {
    const items = KantinKu.cart();

    if (!items.length) {
        KantinKu.notify('Keranjang masih kosong.', 'warning');
        return;
    }

    const buyerNameInput = document.getElementById('buyer-name');
    const buyerName = buyerNameInput.value.trim();
    if (!buyerName) {
        KantinKu.notify('Masukkan nama pembeli sebelum melanjutkan pembayaran.', 'warning');
        buyerNameInput.focus();
        return;
    }

    const bank = method === 'Transfer Bank'
        ? document.getElementById('bank-select').value
        : null;

    const now = new Date().toISOString();
    const account = KantinKu.user();

    const order = {
        id: `ORD-${Date.now().toString(36).toUpperCase()}`,
        sellerId: items[0]?.sellerId || KantinKu.storeId,
        toko: items[0]?.toko || 'KantinKu',

        userId: account?.role === 'pembeli' ? account.email.toLowerCase() : 'guest',
        buyerName,

        tanggal: now,
        items: items.map(item => ({ ...item })),
        total: items.reduce(
            (sum, item) => sum + Number(item.harga) * Number(item.jumlah),
            0
        ),

        metode: method,
        bankTujuan: bank,
        virtualAccount: bank ? demoVirtualAccounts[bank] : null,

        paymentStatus: method === 'Tunai'
            ? 'Menunggu pembayaran di tempat'
            : 'Menunggu verifikasi',

        paymentUpdates: [{
            status: method === 'Tunai'
                ? 'Menunggu pembayaran di tempat'
                : 'Menunggu verifikasi pembayaran oleh penjual',
            tanggal: now
        }],

        status: 'Menunggu konfirmasi',

        updates: [{
            status: 'Menunggu konfirmasi',
            tanggal: now
        }]
    };

    const existingOrders = KantinKu.orders();
    KantinKu.saveOrders([order, ...existingOrders]);
    KantinKu.saveCart([]);

    KantinKu.notify('Pesanan berhasil dibuat.', 'success');

    location.href = '../pages/status.html';
}

function kembaliKeHome() {
    location.href = '../pages/keranjang.html';
}
