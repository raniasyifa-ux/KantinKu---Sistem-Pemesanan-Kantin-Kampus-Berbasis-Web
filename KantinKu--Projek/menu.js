let keranjang = JSON.parse(localStorage.getItem("keranjang")) || {};

function filterMenu() {
    const keyword = document.getElementById("searchMenu").value.toLowerCase().trim();
    const cards = document.querySelectorAll(".menu-card");

    cards.forEach(card => {
        const nama = card.querySelector("h3")?.textContent.toLowerCase() || "";
        const deskripsi = card.querySelector("p")?.textContent.toLowerCase() || "";
        const cocok = nama.includes(keyword) || deskripsi.includes(keyword);
        card.style.display = cocok ? "flex" : "none";
    });
}

function simpan(){
    localStorage.setItem("keranjang", JSON.stringify(keranjang));
    updateCart();
}

function tambahJumlah(id){
    let jumlah = document.getElementById("jumlah-" + id);
    jumlah.textContent = Number(jumlah.textContent) + 1;
}

function kurangiJumlah(id){
    let jumlah = document.getElementById("jumlah-" + id);

    if(Number(jumlah.textContent) > 1){
        jumlah.textContent = Number(jumlah.textContent) - 1;
    }
}

function tambahKeKeranjang(id, nama, harga){
    let jumlah = Number(
        document.getElementById("jumlah-" + id).textContent
    );

    if(keranjang[id]){
        keranjang[id].jumlah += jumlah;
    }else{
        keranjang[id] = {
            nama:nama,
            harga:harga,
            jumlah:jumlah
        };
    }

    simpan();
    tampilNotif(nama + " ditambahkan ke keranjang");
}

function updateCart(){
    let jumlah = Object.values(keranjang)
        .reduce((total,item) => total + item.jumlah, 0);

    document.querySelector(".cart-count").textContent = jumlah;
}

function bukaKeranjang(event){
    if(event) event.preventDefault();

    let list = document.getElementById("cartList");
    list.innerHTML = "";

    let items = Object.values(keranjang);

    if(items.length === 0){
        list.innerHTML = "<p>Keranjang masih kosong.</p>";
    }else{
        items.forEach(item => {
            let div = document.createElement("div");

            div.className = "cart-item";

            div.innerHTML = `
                <span>${item.nama} × ${item.jumlah}</span>
                <strong>Rp${(item.harga * item.jumlah).toLocaleString("id-ID")}</strong>
            `;

            list.appendChild(div);
        });
    }

    hitungTotal();

    document.getElementById("cartOverlay").classList.add("show");
}

function tutupKeranjang(event){
    if(!event || event.target.id === "cartOverlay"){
        document.getElementById("cartOverlay").classList.remove("show");
    }
}

function hitungTotal(){
    let total = Object.values(keranjang)
        .reduce((sum,item) => sum + item.harga * item.jumlah, 0);

    document.getElementById("cartTotal").textContent =
        "Rp" + total.toLocaleString("id-ID");

    document.getElementById("paymentTotal").textContent =
        "Rp" + total.toLocaleString("id-ID");
}

function bukaPembayaran(){
    document.getElementById("cartOverlay").classList.remove("show");

    hitungTotal();

    if (Object.keys(keranjang).length > 0) {
        localStorage.setItem("keranjang", JSON.stringify(keranjang));
        window.location.href = "pembayaran.html";
        return;
    }

    document.getElementById("paymentOverlay").classList.add("show");
}

function tutupPembayaran(event){
    if(!event || event.target.id === "paymentOverlay"){
        document.getElementById("paymentOverlay").classList.remove("show");
    }
}

function prosesPembayaran(){
    if(Object.keys(keranjang).length === 0){
        tampilNotif("Keranjang masih kosong");
        return;
    }

    let metode = document.getElementById("metodePembayaran").value;

    localStorage.setItem("metodePembayaran", metode);
    localStorage.setItem("statusPesanan", "Diproses");

    keranjang = {};
    localStorage.removeItem("keranjang");

    updateCart();

    document.getElementById("paymentOverlay").classList.remove("show");

    tampilNotif("Pembayaran " + metode + " berhasil!");
    setTimeout(() => {
        window.location.href = "status.html";
    }, 800);
}

function tampilNotif(teks){
    let notif = document.getElementById("notif");

    notif.textContent = teks;
    notif.classList.add("show");

    setTimeout(() => {
        notif.classList.remove("show");
    },2000);
}

document.addEventListener("DOMContentLoaded", function() {
    const searchInput = document.getElementById("searchMenu");
    if (searchInput) {
        searchInput.addEventListener("input", filterMenu);
    }
    updateCart();
});