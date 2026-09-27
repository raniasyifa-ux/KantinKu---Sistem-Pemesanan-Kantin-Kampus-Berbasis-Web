let menuData=JSON.parse(localStorage.getItem("menuPenjual"))||{};
let editId="";

const $=id=>document.getElementById(id);
const card=id=>document.querySelector(`.menu-card[data-id="${id}"]`);

function save(){
    localStorage.setItem("menuPenjual",JSON.stringify(menuData));
}

function notif(text){
    $("notif").textContent=text;
    $("notif").classList.add("show");
    setTimeout(()=>$("notif").classList.remove("show"),2000);
}

function rupiah(n){
    return "Rp"+Number(n).toLocaleString("id-ID");
}


/* STOK */

function ubahStok(id,n){

    let c=card(id);
    if(!c)return;

    let e=c.querySelector(".stock-value");
    let stok=Math.max(0,(parseInt(e.textContent)||0)+n);

    e.textContent=stok+" tersedia";
    menuData[id]={...menuData[id],stok};
    save();

    notif("Stok berhasil diperbarui");
}


/* FILTER */

function filterMenu(){

    let q=$("searchMenu").value.toLowerCase().trim();
    let f=document.querySelector(".filter-btn.active").dataset.filter;

    document.querySelectorAll(".menu-card").forEach(c=>{

        let nama=c.querySelector("h3").textContent.toLowerCase();
        let deskripsi=c.querySelector("p").textContent.toLowerCase();

        c.style.display=
            (nama.includes(q)||deskripsi.includes(q)) &&
            (f==="semua"||c.dataset.category===f)
            ? "" : "none";
    });

    document.querySelectorAll(".menu-section").forEach(s=>{
        s.style.display=[...s.querySelectorAll(".menu-card")]
            .some(c=>c.style.display!=="none")?"":"none";
    });
}

$("searchMenu").addEventListener("input",filterMenu);

document.querySelectorAll(".filter-btn").forEach(btn=>{
    btn.onclick=()=>{
        document.querySelectorAll(".filter-btn")
            .forEach(b=>b.classList.remove("active"));
        btn.classList.add("active");
        filterMenu();
    };
});


/* TAMBAH */

function bukaTambahMenu(){
    editId="";
    $("formTitle").textContent="Tambah Menu";
    $("menuForm").reset();
    $("menuOverlay").classList.add("show");
}


/* EDIT */

function bukaEditMenu(id){

    let c=card(id);
    if(!c)return;

    editId=id;

    $("formTitle").textContent="Edit Menu";
    $("namaMenu").value=c.querySelector("h3").textContent.trim();
    $("kategoriMenu").value=c.dataset.category;
    $("deskripsiMenu").value=c.querySelector("p").textContent.trim();
    $("hargaMenu").value=c.querySelector("h4").textContent.replace(/[^\d]/g,"");
    $("stokMenu").value=c.querySelector(".stock-value").textContent.replace(" tersedia","");
    $("gambarMenu").value=c.querySelector("img").getAttribute("src");

    $("menuOverlay").classList.add("show");
}


/* FORM */

$("menuForm").onsubmit=e=>{
    e.preventDefault();

    let data={
        nama:$("namaMenu").value.trim(),
        kategori:$("kategoriMenu").value,
        deskripsi:$("deskripsiMenu").value.trim(),
        harga:Number($("hargaMenu").value),
        stok:Number($("stokMenu").value),
        gambar:$("gambarMenu").value.trim()
    };

    if(!data.nama||!data.kategori||!data.deskripsi||!data.gambar){
        notif("Lengkapi data menu");
        return;
    }

    editId ? updateMenu(editId,data) : tambahMenu(data);
};


/* UPDATE */

function updateMenu(id,d){

    let c=card(id);

    c.dataset.category=d.kategori;
    c.querySelector("img").src=d.gambar;
    c.querySelector("img").alt=d.nama;
    c.querySelector(".category-badge").textContent=d.kategori;
    c.querySelector("h3").textContent=d.nama;
    c.querySelector("p").textContent=d.deskripsi;
    c.querySelector("h4").textContent=rupiah(d.harga);
    c.querySelector(".stock-value").textContent=d.stok+" tersedia";

    menuData[id]=d;
    save();

    $("menuOverlay").classList.remove("show");
    notif("Menu berhasil diperbarui");

    filterMenu();
}


/* TAMBAH */

function tambahMenu(d){

    let id=d.nama.toLowerCase().replace(/[^a-z0-9]+/g,"-");
    if(card(id))id+="-"+Date.now();

    let c=document.createElement("div");

    c.className="menu-card";
    c.dataset.id=id;
    c.dataset.category=d.kategori;

    c.innerHTML=`
        <img src="${d.gambar}" alt="${d.nama}">
        <div class="content">
            <span class="category-badge">${d.kategori}</span>
            <h3>${d.nama}</h3>
            <p>${d.deskripsi}</p>
            <h4>${rupiah(d.harga)}</h4>

            <div class="stock-info">
                <span>Stok</span>
                <strong class="stock-value">${d.stok} tersedia</strong>
                <div class="stock-control">
                    <button onclick="ubahStok('${id}',-1)">−</button>
                    <button onclick="ubahStok('${id}',1)">+</button>
                </div>
            </div>

            <div class="seller-buttons">
                <button onclick="bukaEditMenu('${id}')">Edit</button>
                <button class="hapus" onclick="hapusMenu('${id}')">Hapus</button>
            </div>
        </div>
    `;

    document
        .querySelector(
            d.kategori==="Makanan"
            ? ".makanan-section .menu-container"
            : ".minuman-section .menu-container"
        )
        .appendChild(c);

    menuData[id]=d;
    save();

    $("menuOverlay").classList.remove("show");
    notif("Menu berhasil ditambahkan");

    filterMenu();
}


/* HAPUS */

function hapusMenu(id){

    let c=card(id);
    if(!c)return;

    let nama=c.querySelector("h3").textContent;

    if(!confirm(`Hapus "${nama}"?`))return;

    c.remove();
    delete menuData[id];
    save();

    notif("Menu berhasil dihapus");
    filterMenu();
}


/* TUTUP */

function tutupMenu(e){
    if(!e||e.target.id==="menuOverlay")
        $("menuOverlay").classList.remove("show");
}


/* LOAD */

document.querySelectorAll(".menu-card").forEach(c=>{

    let id=c.dataset.id;

    if(menuData[id]){

        let d=menuData[id];

        c.dataset.category=d.kategori;
        c.querySelector("img").src=d.gambar;
        c.querySelector("h3").textContent=d.nama;
        c.querySelector("p").textContent=d.deskripsi;
        c.querySelector("h4").textContent=rupiah(d.harga);
        c.querySelector(".category-badge").textContent=d.kategori;
        c.querySelector(".stock-value").textContent=d.stok+" tersedia";

    }else{

        menuData[id]={
            nama:c.querySelector("h3").textContent,
            kategori:c.dataset.category,
            deskripsi:c.querySelector("p").textContent,
            harga:Number(c.querySelector("h4").textContent.replace(/[^\d]/g,"")),
            stok:Number(c.querySelector(".stock-value").textContent.replace(" tersedia","")),
            gambar:c.querySelector("img").getAttribute("src")
        };
    }
});

save();