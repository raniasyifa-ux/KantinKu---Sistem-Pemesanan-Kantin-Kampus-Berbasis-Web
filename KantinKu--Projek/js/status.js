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

    if (!stored) return null;

    return {
        ...stored,
        items: Array.isArray(stored.items)
            ? stored.items.map(item => ({
                ...item,
                jumlah: Number(item.jumlah || item.quantity || 1),
                harga: Number(item.harga || 0)
            }))
            : Object.values(stored.items || {}).map(item => ({
                ...item,
                jumlah: Number(item.jumlah || item.quantity || 1),
                harga: Number(item.harga || 0)
            }))
    };
}

function getRiwayatPesanan() {
    const stored = JSON.parse(localStorage.getItem("riwayatPesanan"));

    if (!Array.isArray(stored)) return [];

    return stored.map(order => ({
        ...order,
        items: Array.isArray(order.items) ? order.items : []
    })).sort((a, b) => new Date(b.tanggal) - new Date(a.tanggal));
}

function hitungTotal(items) {
    return (items || []).reduce((sum, item) => sum + Number(item.harga || 0) * Number(item.jumlah || 1), 0);
}

function formatTanggal(isoString) {
    if (!isoString) return "-";
    return new Date(isoString).toLocaleString("id-ID", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit"
    });
}

const detailPesanan = document.getElementById("detailPesanan");
const statusPesanan = document.getElementById("statusPesanan");
const pesananAktifEl = document.getElementById("pesananAktif");
const riwayatPesananEl = document.getElementById("riwayatPesanan");

let statusPesananVal = localStorage.getItem("statusPesanan") || "Menunggu Pembayaran";

function renderDetailPesanan(order) {
    if (!order) {
        detailPesanan.innerHTML = "<p>Data pesanan tidak ditemukan.</p>";
        return;
    }

    let htmlContent = '<ul style="list-style: none; padding: 0;">';
    order.items.forEach(item => {
        const subtotal = Number(item.harga || 0) * Number(item.jumlah || 1);
        htmlContent += `<li style="margin-bottom: 6px;"><strong>${item.nama}</strong> (${Number(item.jumlah || 1)} porsi) - ${formatRupiah(subtotal)}</li>`;
    });

    htmlContent += `</ul><hr><p><strong>Total:</strong> ${formatRupiah(order.total || hitungTotal(order.items))}</p>`;
    htmlContent += `<p><strong>Metode Pembayaran:</strong> ${order.metode || localStorage.getItem("metodePembayaran") || "-"}</p>`;

    detailPesanan.innerHTML = htmlContent;
}

function renderPesananAktif() {
    const activeOrder = getPesananAktif();

    if (!activeOrder || !Array.isArray(activeOrder.items) || activeOrder.items.length === 0) {
        pesananAktifEl.innerHTML = "<p>Tidak ada pesanan aktif.</p>";
        return;
    }

    const namaMenu = activeOrder.items.map(item => item.nama).join(", ");
    const status = activeOrder.status || "Diproses";
    pesananAktifEl.innerHTML = `
        <div style="padding: 10px 0; border-bottom: 1px dashed #ddd;">
            <strong>${activeOrder.id}</strong> - ${namaMenu} - <span>${status}</span>
        </div>
    `;
}

function renderRiwayat() {
    const riwayat = getRiwayatPesanan();

    if (riwayat.length === 0) {
        riwayatPesananEl.innerHTML = "<p>Belum ada riwayat pesanan.</p>";
        return;
    }

    riwayatPesananEl.innerHTML = riwayat.map((order) => {
        const namaMenu = (order.items || []).map(item => item.nama).join(", ") || "Pesanan";
        const statusText = order.status || "Selesai";

        return `
            <div style="display: flex; justify-content: space-between; align-items: center; gap: 12px; border-bottom: 1px dashed #ddd; padding: 10px 0;">
                <div>
                    <strong>${order.id}</strong> - ${namaMenu} - ${statusText}
                </div>
                <button class="btn-secondary" onclick="lihatDetail('${order.id}')" style="padding: 6px 10px;">Lihat Detail</button>
            </div>
        `;
    }).join("");
}

function lihatDetail(orderId) {
    const activeOrder = getPesananAktif();
    const riwayat = getRiwayatPesanan();
    const order = [activeOrder, ...riwayat].find(item => item && item.id === orderId);

    if (!order) {
        alert("Detail pesanan tidak ditemukan.");
        return;
    }

    const modal = document.getElementById("detailOrderModal");
    const title = document.getElementById("detailOrderTitle");
    const content = document.getElementById("detailOrderContent");

    title.textContent = `Detail ${order.id}`;

    const itemList = (order.items || []).map(item => {
        const subtotal = Number(item.harga || 0) * Number(item.jumlah || 1);
        return `
            <div class="detail-order-item">
                <div>
                    <strong>${item.nama}</strong>
                    <div class="detail-meta">${Number(item.jumlah || 1)} x ${formatRupiah(Number(item.harga || 0))}</div>
                </div>
                <strong>${formatRupiah(subtotal)}</strong>
            </div>
        `;
    }).join("");

    const total = order.total || hitungTotal(order.items || []);

    content.innerHTML = `
        <div class="detail-meta">Tanggal: ${formatTanggal(order.tanggal)}</div>
        <div class="detail-meta">Metode: ${order.metode || "-"}</div>
        <div class="detail-meta">Status: ${order.status || "Diproses"}</div>
        ${itemList}
        <div class="detail-total">
            <span>Total</span>
            <span>${formatRupiah(total)}</span>
        </div>
    `;

    modal.style.display = "flex";
}

function tutupDetailPesanan() {
    const modal = document.getElementById("detailOrderModal");
    modal.style.display = "none";
}

const activeOrder = getPesananAktif();
renderDetailPesanan(activeOrder);
renderPesananAktif();
renderRiwayat();

statusPesanan.textContent = activeOrder ? (activeOrder.status || "Diproses") : "Tidak Ada Pesanan";
statusPesananVal = activeOrder ? (activeOrder.status || "Diproses") : "Tidak Ada Pesanan";

function ubahStatus() {
    const activeOrder = getPesananAktif();

    if (!activeOrder) {
        alert("Tidak ada pesanan aktif untuk diperbarui.");
        return;
    }

    const riwayat = getRiwayatPesanan();

    if (activeOrder.status === "Diproses") {
        activeOrder.status = "Selesai";
        activeOrder.tanggalSelesai = new Date().toISOString();
        riwayat.unshift({
            ...activeOrder,
            status: "Selesai",
            tanggal: activeOrder.tanggalSelesai
        });
        localStorage.setItem("riwayatPesanan", JSON.stringify(riwayat));
        localStorage.removeItem("pesananAktif");
        localStorage.setItem("statusPesanan", "Selesai");
        statusPesananVal = "Selesai";
        statusPesanan.textContent = "Selesai";
        renderDetailPesanan(null);
        renderPesananAktif();
        renderRiwayat();
        alert("Status pesanan diperbarui menjadi: Selesai");
        return;
    }

    alert("Pesanan sudah selesai.");
}

function formatRupiah(angka) {
    return new Intl.NumberFormat("id-ID", {
        style: "currency",
        currency: "IDR",
        minimumFractionDigits: 0
    }).format(angka);
}

function kembaliKeHome() {
    window.location.href = "../index.html";
}