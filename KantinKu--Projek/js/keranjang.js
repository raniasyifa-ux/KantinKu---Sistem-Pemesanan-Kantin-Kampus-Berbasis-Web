document.addEventListener('DOMContentLoaded', renderCart);

function renderCart() {
  const items = KantinKu.cart();
  const table = document.getElementById('tabel-keranjang');
  const totalElement = document.getElementById('total-keranjang');
  const safe = KantinKu.escapeHtml;

  if (!items.length) {
    table.innerHTML = '<tr><td colspan="6" class="empty-cell">Keranjang masih kosong. Pilih menu untuk mulai memesan.</td></tr>';
    totalElement.textContent = KantinKu.money(0);
    return;
  }

  table.innerHTML = items.map((item, index) => `
    <tr>
      <td>${index + 1}</td>
      <td>${safe(item.nama)}</td>
      <td>
        <div class="quantity-control">
          <button type="button" data-change="-1" data-index="${index}" aria-label="Kurangi ${safe(item.nama)}">-</button>
          <span>${item.jumlah}</span>
          <button type="button" data-change="1" data-index="${index}" aria-label="Tambah ${safe(item.nama)}">+</button>
        </div>
      </td>
      <td>${KantinKu.money(item.harga)}</td>
      <td>${KantinKu.money(item.harga * item.jumlah)}</td>
      <td><button type="button" class="btn-secondary" data-remove="${index}">Hapus</button></td>
    </tr>`).join('');

  totalElement.textContent = KantinKu.money(items.reduce((sum, item) => sum + item.harga * item.jumlah, 0));
}

document.addEventListener('click', event => {
  const remove = event.target.closest('[data-remove]');
  const change = event.target.closest('[data-change]');
  if (!remove && !change) return;

  const items = KantinKu.cart();
  const index = Number((remove || change).dataset.remove ?? (remove || change).dataset.index);
  if (remove) items.splice(index, 1);
  else items[index].jumlah = Math.max(1, items[index].jumlah + Number(change.dataset.change));
  KantinKu.saveCart(items);
  renderCart();
});