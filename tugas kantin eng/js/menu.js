// ========================================
// DATA JUMLAH MENU
// ========================================

let jumlahMenu = {
    "nasi-goreng": 1,
    "mie-ayam": 1,
    "es-milo": 1,
    "milky-regal": 1
};


// ========================================
// TAMBAH JUMLAH
// ========================================

function tambahJumlah(id) {

    jumlahMenu[id]++;

    document.getElementById("jumlah-" + id).textContent =
        jumlahMenu[id];
}


// ========================================
// KURANGI JUMLAH
// ========================================

function kurangiJumlah(id) {

    if (jumlahMenu[id] > 1) {

        jumlahMenu[id]--;

        document.getElementById("jumlah-" + id).textContent =
            jumlahMenu[id];
    }
}


// ========================================
// TAMBAH MENU KE KERANJANG
// ========================================

function tambahKeKeranjang(id, nama, harga) {

    let keranjang =
        JSON.parse(localStorage.getItem("keranjang")) || [];


    let jumlah = jumlahMenu[id];


    let itemLama = keranjang.find(item => item.id === id);


    if (itemLama) {

        itemLama.jumlah += jumlah;

    } else {

        keranjang.push({

            id: id,
            nama: nama,
            harga: harga,
            jumlah: jumlah

        });

    }


    localStorage.setItem(
        "keranjang",
        JSON.stringify(keranjang)
    );


    alert(nama + " berhasil ditambahkan ke keranjang!");


    // Setelah ditambahkan,
    // quantity dikembalikan menjadi 1.

    jumlahMenu[id] = 1;

    let elemenJumlah =
        document.getElementById("jumlah-" + id);

    if (elemenJumlah) {

        elemenJumlah.textContent = 1;

    }
}


// ========================================
// MENAMPILKAN KERANJANG
// ========================================

function tampilkanKeranjang() {

    let keranjang =
        JSON.parse(localStorage.getItem("keranjang")) || [];


    let container =
        document.getElementById("keranjang-container");

    let kosong =
        document.getElementById("keranjang-kosong");

    let totalHarga =
        document.getElementById("total-harga");


    if (!container) {
        return;
    }


    // Jika keranjang kosong

    if (keranjang.length === 0) {

        container.innerHTML = "";

        kosong.style.display = "block";

        totalHarga.textContent = "Rp0";

        return;
    }


    kosong.style.display = "none";


    let total = 0;


    container.innerHTML = "";


    keranjang.forEach(function(item, index) {

        let subtotal =
            item.harga * item.jumlah;


        total += subtotal;


        let itemHTML = `

            <div class="cart-item">

                <div>

                    <h3>${item.nama}</h3>

                    <p>
                        Harga:
                        Rp${item.harga.toLocaleString("id-ID")}
                    </p>

                    <p>
                        Jumlah:
                        ${item.jumlah}
                    </p>

                    <p>
                        Subtotal:
                        Rp${subtotal.toLocaleString("id-ID")}
                    </p>

                </div>


                <div>

                    <button onclick="kurangiItemKeranjang(${index})">
                        −
                    </button>

                    <button onclick="tambahItemKeranjang(${index})">
                        +
                    </button>

                    <button onclick="hapusItemKeranjang(${index})">
                        Hapus
                    </button>

                </div>

            </div>

        `;


        container.innerHTML += itemHTML;

    });


    totalHarga.textContent =
        "Rp" + total.toLocaleString("id-ID");
}


// ========================================
// TAMBAH JUMLAH DI KERANJANG
// ========================================

function tambahItemKeranjang(index) {

    let keranjang =
        JSON.parse(localStorage.getItem("keranjang")) || [];


    keranjang[index].jumlah++;


    localStorage.setItem(
        "keranjang",
        JSON.stringify(keranjang)
    );


    tampilkanKeranjang();
}


// ========================================
// KURANGI JUMLAH DI KERANJANG
// ========================================

function kurangiItemKeranjang(index) {

    let keranjang =
        JSON.parse(localStorage.getItem("keranjang")) || [];


    if (keranjang[index].jumlah > 1) {

        keranjang[index].jumlah--;

    } else {

        keranjang.splice(index, 1);

    }


    localStorage.setItem(
        "keranjang",
        JSON.stringify(keranjang)
    );


    tampilkanKeranjang();
}


// ========================================
// HAPUS ITEM
// ========================================

function hapusItemKeranjang(index) {

    let keranjang =
        JSON.parse(localStorage.getItem("keranjang")) || [];


    keranjang.splice(index, 1);


    localStorage.setItem(
        "keranjang",
        JSON.stringify(keranjang)
    );


    tampilkanKeranjang();
}


// ========================================
// LANJUT KE PEMBAYARAN
// ========================================

function lanjutPembayaran() {

    let keranjang =
        JSON.parse(localStorage.getItem("keranjang")) || [];


    if (keranjang.length === 0) {

        alert("Keranjang masih kosong!");

        return;
    }


    window.location.href = "pembayaran.html";
}


// ========================================
// JALANKAN SAAT HALAMAN SELESAI DIMUAT
// ========================================

document.addEventListener("DOMContentLoaded", function() {

    tampilkanKeranjang();

});