const keranjang = JSON.parse(localStorage.getItem("keranjang")) || [];
const metode = localStorage.getItem("metodePembayaran");
let statusPesananVal = localStorage.getItem("statusPesanan") || "Menunggu Pembayaran";

const detailPesanan = document.getElementById("detailPesanan");
const statusPesanan = document.getElementById("statusPesanan");

if (keranjang.length > 0) {
    let htmlContent = '<ul style="list-style: none; padding: 0;">';
    let total = 0;

    keranjang.forEach(item => {
        let subtotal = item.harga * item.jumlah;
        total += subtotal;
        htmlContent += `<li style="margin-bottom: 6px;"><strong>${item.nama}</strong> (${item.jumlah} porsi) - ${formatRupiah(subtotal)}</li>`;
    });

    htmlContent += `</ul><hr><p><strong>Total:</strong> ${formatRupiah(total)}</p>`;
    htmlContent += `<p><strong>Metode Pembayaran:</strong> ${metode || "-"}</p>`;

    detailPesanan.innerHTML = htmlContent;
} else {
    detailPesanan.innerHTML = "<p>Data pesanan tidak ditemukan.</p>";
}

statusPesanan.textContent = statusPesananVal;

function ubahStatus() {
    if (statusPesananVal === "Menunggu Pembayaran") {
        statusPesananVal = "Diproses";
    } else if (statusPesananVal === "Diproses") {
        statusPesananVal = "Selesai";
    } else {
        alert("Pesanan sudah selesai.");
        return;
    }

    localStorage.setItem("statusPesanan", statusPesananVal);
    statusPesanan.textContent = statusPesananVal;
    alert("Status pesanan diperbarui menjadi: " + statusPesananVal);
}

function formatRupiah(angka) {
    return new Intl.NumberFormat("id-ID", {
        style: "currency",
        currency: "IDR",
        minimumFractionDigits: 0
    }).format(angka);
}

function kembaliKeHome() {
    window.location.href = "index.html";
}