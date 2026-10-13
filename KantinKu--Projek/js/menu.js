
let selectedItem = null;
let selectedQuantity = 1;
let selectedCategory = 'Semua';

document.addEventListener('DOMContentLoaded', () => {
    renderMenu();

    document.getElementById('menu-search').addEventListener('input', renderMenu);

    document.querySelectorAll('[data-category]').forEach(button => {
        button.addEventListener('click', () => {
            selectedCategory = button.dataset.category;

            document.querySelectorAll('[data-category]').forEach(tab => {
                tab.classList.toggle('active', tab === button);
            });

            renderMenu();
        });
    });

    const dialog = document.getElementById('add-order-dialog');

    document.getElementById('dialog-quantity-minus').addEventListener('click', () => {
        selectedQuantity = Math.max(1, selectedQuantity - 1);
        renderDialogQuantity();
    });

    document.getElementById('dialog-quantity-plus').addEventListener('click', () => {
        selectedQuantity += 1;
        renderDialogQuantity();
    });

    document.getElementById('dialog-cancel').addEventListener('click', () => {
        dialog.close();
    });

    document.getElementById('dialog-confirm').addEventListener('click', confirmAddToCart);

    dialog.addEventListener('close', () => {
        selectedItem = null;
    });

    document.addEventListener('click', event => {
        const button = event.target.closest('[data-add-item]');

        if (button) {
            openAddDialog(button.dataset.addItem);
        }
    });

    window.addEventListener('storage', renderMenu);
});

function renderMenu() {
    const searchInput = document.getElementById('menu-search');
    const products = KantinKu.catalog().filter(item => item.aktif !== false);
    const query = searchInput.value.trim().toLocaleLowerCase('id-ID');

    const filtered = products.filter(item => {
        const matchesCategory =
            selectedCategory === 'Semua' || item.kategori === selectedCategory;

        const searchableText =
            `${item.nama} ${item.deskripsi || ''} ${item.kategori}`
                .toLocaleLowerCase('id-ID');

        return matchesCategory && searchableText.includes(query);
    });

    const groups = [
        ['Makanan', document.getElementById('makanan')],
        ['Minuman', document.getElementById('minuman')]
    ];

    groups.forEach(([category, section]) => {
        const items = filtered.filter(item => item.kategori === category);

        section.hidden = items.length === 0;
        section.querySelector('.menu-container').innerHTML =
            items.map(renderMenuCard).join('');
    });

    const bestSeller = document.querySelector('.best-seller');

    if (bestSeller) {
        bestSeller.hidden = true;
    }

    document.getElementById('menu-results').textContent = filtered.length
        ? `${filtered.length} menu tersedia`
        : 'Menu tidak ditemukan. Coba kata kunci lain.';
}

function renderMenuCard(item) {
    const safe = KantinKu.escapeHtml;
    const image = safe(encodeURI(item.gambar || ''));

    return `
        <article class="menu-card">
            <img src="${image}" alt="${safe(item.nama)}" loading="lazy">

            <div class="menu-card-content">
                <span class="menu-category-label">
                    ${safe(item.kategori)}
                </span>

                <h3>${safe(item.nama)}</h3>

                <p>${safe(item.deskripsi || 'Menu pilihan KantinKu.')}</p>

                <div class="menu-card-footer">
                    <strong>${KantinKu.money(item.harga)}</strong>

                    <button
                        class="btn-pesan"
                        type="button"
                        data-add-item="${safe(item.id)}"
                        aria-label="Tambah ${safe(item.nama)}">
                        ＋
                    </button>
                </div>
            </div>
        </article>
    `;
}

function openAddDialog(itemId) {
    // Pembeli tidak perlu login untuk memilih menu.
    selectedItem = KantinKu.catalog().find(
        item => item.id === itemId && item.aktif !== false
    );

    if (!selectedItem) {
        KantinKu.notify('Menu tidak ditemukan atau sedang tidak tersedia.', 'warning');
        return;
    }

    selectedQuantity = 1;

    const image = document.getElementById('dialog-product-image');
    image.src = encodeURI(selectedItem.gambar || '');
    image.alt = selectedItem.nama;

    document.getElementById('add-order-title').textContent = selectedItem.nama;

    document.getElementById('dialog-product-price').textContent =
        `${KantinKu.money(selectedItem.harga)} per porsi`;

    renderDialogQuantity();

    document.getElementById('add-order-dialog').showModal();
}

function renderDialogQuantity() {
    document.getElementById('dialog-quantity').textContent = selectedQuantity;

    document.getElementById('dialog-subtotal').textContent =
        KantinKu.money((Number(selectedItem?.harga) || 0) * selectedQuantity);
}

function confirmAddToCart() {
    if (!selectedItem) {
        return;
    }

    const items = KantinKu.cart();

    const existing = items.find(item => item.id === selectedItem.id);

    const cartItem = {
        id: selectedItem.id,
        nama: selectedItem.nama,
        harga: Number(selectedItem.harga),
        jumlah: selectedQuantity,
        gambar: selectedItem.gambar || '',
        kategori: selectedItem.kategori || '',
        sellerId: selectedItem.sellerId || KantinKu.storeId,
        toko: selectedItem.toko || 'KantinKu'
    };

    if (existing) {
        existing.jumlah = Number(existing.jumlah || 0) + selectedQuantity;
    } else {
        items.push(cartItem);
    }

    KantinKu.saveCart(items);

    document.getElementById('add-order-dialog').close();

    KantinKu.notify(
        `${selectedItem.nama} berhasil ditambahkan ke keranjang.`,
        'success',
        {
            href: '../pages/keranjang.html',
            label: 'Lihat keranjang'
        }
    );
}
