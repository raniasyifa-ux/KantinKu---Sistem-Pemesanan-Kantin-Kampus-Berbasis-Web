document.addEventListener("DOMContentLoaded", function() {
    tampilkanRingkasanPesanan();
});

function getKeranjang() {
    const stored = JSON.parse(localStorage.getItem("keranjang"));

    if (!stored) return [];
    if (Array.isArray(stored)) return stored;

    return Object.values(stored).map(item => ({
        ...item,
        jumlah: Number(item.jumlah || item.quantity || 1),
        harga: Number(item.harga || 0)
    }));
}

function tampilkanRingkasanPesanan() {
    const keranjang = getKeranjang();
    const ringkasan = document.getElementById("ringkasanPesanan");
    const totalPembayaran = document.getElementById("totalPembayaran");

    if (keranjang.length === 0) {
        ringkasan.innerHTML = "<p>Belum ada pesanan.</p>";
        totalPembayaran.textContent = "Rp0";
        return;
    }

    let htmlContent = '<div style="display: flex; flex-direction: column; gap: 10px;">';
    let total = 0;

    keranjang.forEach(item => {
        let subtotal = Number(item.harga || 0) * Number(item.jumlah || 1);
        total += subtotal;

        htmlContent += `
            <div style="display: flex; justify-content: space-between; align-items: center; border-bottom: 1px dashed #ccc; padding-bottom: 8px;">
                <div>
                    <strong>${item.nama}</strong><br>
                    <small style="color: #6C6265;">${Number(item.jumlah || 1)} x ${formatRupiah(Number(item.harga || 0))}</small>
                </div>
                <strong>${formatRupiah(subtotal)}</strong>
            </div>
        `;
    });

    htmlContent += '</div>';
    ringkasan.innerHTML = htmlContent;
    totalPembayaran.textContent = formatRupiah(total);
}

function formatRupiah(angka) {
    return new Intl.NumberFormat("id-ID", {
        style: "currency",
        currency: "IDR",
        minimumFractionDigits: 0
    }).format(angka);
}

function konfirmasiPembayaran() {
    const keranjang = getKeranjang();
    const metode = document.querySelector('input[name="metode"]:checked');

    if (keranjang.length === 0) {
        alert("Belum ada pesanan!");
        return;
    }

    if (!metode) {
        alert("Silakan pilih metode pembayaran terlebih dahulu!");
        return;
    }

    const total = keranjang.reduce((sum, item) => sum + Number(item.harga || 0) * Number(item.jumlah || 1), 0);
    const orderId = "ORD" + Date.now().toString().slice(-6);

    const pesananAktif = {
        id: orderId,
        tanggal: new Date().toISOString(),
        items: keranjang,
        total: total,
        metode: metode.value,
        status: "Diproses"
    };

    localStorage.setItem("pesananAktif", JSON.stringify(pesananAktif));
    localStorage.setItem("metodePembayaran", metode.value);
    localStorage.setItem("statusPesanan", "Diproses");
    localStorage.removeItem("keranjang");

    alert("Pembayaran berhasil dikonfirmasi menggunakan " + metode.value);
    window.location.href = "status.html";
}

function kembaliKeHome() {
    window.location.href = "../index.html";
}