// Mengambil elemen tombol dari HTML
const btnMenu = document.getElementById("btnMenu");
const btnBayar = document.getElementById("btnBayar");
const btnStatus = document.getElementById("btnStatus");

// Mengambil elemen pop-up
const popup = document.getElementById("popup");
const popupTitle = document.getElementById("popupTitle");
const popupMessage = document.getElementById("popupMessage");
const btnClose = document.getElementById("btnClose");


// Fungsi untuk menampilkan pop-up
function tampilkanPopup(judul, pesan) {
    popupTitle.textContent = judul;
    popupMessage.textContent = pesan;
    popup.style.display = "flex";
}


// Tombol Menu
btnMenu.addEventListener("click", function() {
    tampilkanPopup(
        "🍜 Menu KantinKu",
        "Halaman Menu masih dalam tahap pengembangan. Coming soon ya!"
    );
});


// Tombol Pembayaran
btnBayar.addEventListener("click", function() {
    tampilkanPopup(
        "💳 Pembayaran KantinKu",
        "Halaman Pembayaran masih dalam tahap pengembangan. Coming soon ya!"
    );
});


// Tombol Status
btnStatus.addEventListener("click", function() {
    tampilkanPopup(
        "📦 Status Pesanan",
        "Halaman Status masih dalam tahap pengembangan. Coming soon ya!"
    );
});


// Tombol tutup pop-up
btnClose.addEventListener("click", function() {
    popup.style.display = "none";
}); 