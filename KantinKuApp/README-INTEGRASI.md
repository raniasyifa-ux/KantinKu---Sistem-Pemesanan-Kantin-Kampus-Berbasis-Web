# KantinKuApp — Pembeli dan Penjual (terhubung)

## Menjalankan aplikasi
Butuh JDK 11 atau lebih baru. Jalankan dari folder `KantinKuApp`.

**Windows (PowerShell):**
```powershell
New-Item -ItemType Directory -Force -Path out | Out-Null
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse -Filter *.java -Path .\src | ForEach-Object { $_.FullName })
java -cp "out;src" Main
```

**Linux / macOS:**
```bash
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name '*.java')
java -cp "out:src" Main
```

`src` ikut di classpath supaya gambar menu di `src/images` terbaca.
Saat dibuka, pilih **Pembeli**, **Penjual**, atau **Keduanya** (membuka dua jendela sekaligus untuk uji coba).

## Alur pesanan (pembeli → penjual → pembeli)
1. **Pembeli** memilih menu → keranjang → pembayaran (QRIS / Transfer VA / Tunai) → konfirmasi.
   Pesanan langsung tersimpan dengan status `Menunggu konfirmasi` dan nomor unik (`#ORD-001`, `#ORD-002`, ...).
2. **Penjual** (login ke dashboard) melihat pesanan masuk otomatis, paling baru di atas.
   Tekan **Setujui / Konfirmasi pesanan** → status `Diproses`, lalu **Ubah ke Siap Diambil**, lalu **Ubah ke Selesai**.
3. **Pembeli** membuka tab **Pesanan Saya**: status berubah sendiri (tanpa pindah tab) menjadi
   `Telah dikonfirmasi · Sedang diproses` → `Siap diambil` → `Pesanan selesai`.
   Pesanan yang belum selesai ada di **Pesanan Aktif**, yang selesai pindah ke **Riwayat Pesanan**.

## Alur menu (penjual → pembeli)
Menu yang ditambah / diedit / disembunyikan / dihapus penjual langsung berubah di halaman **Menu** pembeli
(dicek setiap 1,5 detik). Katalog awal penjual sama dengan menu bawaan pembeli.

## Penyimpanan data
Folder `~/.kantinku-desktop` (Windows: `%USERPROFILE%\.kantinku-desktop`):
- `orders.tsv` pesanan (dipakai bersama pembeli & penjual, dikunci saat ditulis)
- `orders-pembeli.txt` daftar nomor pesanan milik pembeli di komputer ini
- `menus.tsv` katalog menu penjual
- `accounts.tsv` akun penjual (password di-hash)

Pembeli dan penjual boleh dijalankan sebagai dua program terpisah di komputer yang sama; datanya tetap sinkron.
Untuk mulai dari nol, tutup aplikasi lalu hapus folder tersebut.

## Yang diperbaiki dari versi sebelumnya
- Nomor pesanan tidak lagi mulai dari `#ORD-001` setiap aplikasi dibuka (dulu pesanan lama tertimpa).
- Status pesanan di pembeli diperbarui otomatis, bukan hanya saat pindah tab.
- "Pesanan Saya" hanya menampilkan pesanan yang benar-benar sudah tersimpan (dulu bisa muncul pesanan hantu bila pembayaran dibatalkan) dan menampilkan semua pesanan, bukan hanya yang terakhir.
- Menu pembeli ikut berubah saat penjual mengubah katalog, termasuk bila semua menu disembunyikan.
- Menutup satu jendela tidak lagi menutup seluruh aplikasi; aplikasi berhenti setelah semua jendela tertutup.
- Penulisan pesanan aman dari tabrakan antar-program (file lock) dan nomor pesanan ganda otomatis dibetulkan.
- Metode transfer sekarang mencatat banknya (mis. `Transfer Bank (BCA)`).
