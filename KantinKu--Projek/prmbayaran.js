document.addEventListener("DOMContentLoaded", function() {
    tampilkanRingkasanPesanan();
});

function tampilkanRingkasanPesanan() {
    const keranjang = JSON.parse(localStorage.getItem("keranjang")) || [];
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
        let subtotal = item.harga * item.jumlah;
        total += subtotal;

        htmlContent += `
            <div style="display: flex; justify-content: space-between; align-items: center; border-bottom: 1px dashed #ccc; padding-bottom: 8px;">
                <div>
                    <strong>${item.nama}</strong><br>
                    <small style="color: #6C6265;">${item.jumlah} x ${formatRupiah(item.harga)}</small>
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
    const keranjang = JSON.parse(localStorage.getItem("keranjang")) || [];
    const metode = document.querySelector('input[name="metode"]:checked');

    if (keranjang.length === 0) {
        alert("Belum ada pesanan!");
        return;
    }

    if (!metode) {
        alert("Silakan pilih metode pembayaran terlebih dahulu!");
        return;
    }

    localStorage.setItem("metodePembayaran", metode.value);
    localStorage.setItem("statusPesanan", "Diproses");

    alert("Pembayaran berhasil dikonfirmasi menggunakan " + metode.value);
    window.location.href = "status.html";
}

function kembaliKeHome() {
    window.location.href = "index.html";
}