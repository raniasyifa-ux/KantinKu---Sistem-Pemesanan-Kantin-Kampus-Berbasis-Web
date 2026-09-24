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

function getPesananAktif() {
    const stored = JSON.parse(localStorage.getItem("pesananAktif"));

    if (!stored) return getKeranjang();
    if (Array.isArray(stored)) return stored;

    return Object.values(stored).map(item => ({
        ...item,
        jumlah: Number(item.jumlah || item.quantity || 1),
        harga: Number(item.harga || 0)
    }));
}

const keranjang = getPesananAktif();
const metode = localStorage.getItem("metodePembayaran");
let statusPesananVal = localStorage.getItem("statusPesanan") || "Menunggu Pembayaran";

const detailPesanan = document.getElementById("detailPesanan");
const statusPesanan = document.getElementById("statusPesanan");

if (keranjang.length > 0) {
    let htmlContent = '<ul style="list-style: none; padding: 0;">';
    let total = 0;

    keranjang.forEach(item => {
        let subtotal = Number(item.harga || 0) * Number(item.jumlah || 1);
        total += subtotal;
        htmlContent += `<li style="margin-bottom: 6px;"><strong>${item.nama}</strong> (${Number(item.jumlah || 1)} porsi) - ${formatRupiah(subtotal)}</li>`;
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