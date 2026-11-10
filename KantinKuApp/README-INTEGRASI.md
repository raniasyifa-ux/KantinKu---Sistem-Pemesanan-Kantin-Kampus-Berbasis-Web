# KantinKu Desktop — MySQL/MariaDB

## Database

The application connects directly to the existing `db_kantinkudesktop` database through
MySQL Connector/J. The default local connection is
`jdbc:mysql://localhost:3306/db_kantinkudesktop`; it uses `root` with an empty password
only when no local credentials are configured. Set `KANTINKU_MYSQL_USERNAME` and
`KANTINKU_MYSQL_PASSWORD` in the environment for other credentials. Do not put
database passwords in source files or commit them.
For local development, the JDBC URL and username may also be overridden with
the Java properties `kantinku.mysql.jdbcUrl` and `kantinku.mysql.username`.

The application uses the existing seven tables only: `users`, `menu`,
`detail_pemesanan`, `order_item`, `pembayaran`, `riwayat_status_pesanan`, and
`riwayat_status_pembayaran`. It does not create, drop, or migrate tables at
startup. The approved SQL in
[`sql/mysql_add_buyer_phone_and_menu_stock.sql`](sql/mysql_add_buyer_phone_and_menu_stock.sql)
adds `detail_pemesanan.no_hp_pembeli` and `menu.stok`; apply it once to the
existing database before running this application. No other schema changes are
required by the application.

Existing seller passwords remain in `users.password`; bcrypt hashes in the
`$2y$` format are verified without a bulk password change. Buyers still order
without signing in and provide their name and phone number during checkout.

## Compile and run on Windows

From the project directory in PowerShell:

```powershell
New-Item -ItemType Directory -Force -Path .\out | Out-Null
javac -encoding UTF-8 -cp ".\lib\*" -d .\out (Get-ChildItem -Recurse -Filter *.java -Path .\src | ForEach-Object { $_.FullName })
java -cp "out;src;lib/*" app.Main
```

Run the seller dashboard or the separate queue window with:

```powershell
java -cp "out;src;lib/*" app.MainPenjual
java -cp "out;src;lib/*" app.MainAntri
```

`src` stays on the runtime classpath so menu images can be loaded. The
`.vscode/settings.json` configuration includes all JARs in `lib`.

## Source package structure

```text
src/
  app/          application entry points
  config/       database connection and startup configuration
  controllers/  adapters between Swing views and application services
  dao/impl/     database and retained file-backed data access classes
  models/       application data objects
  services/     business operations
  util/         shared UI utilities
  views/        Swing windows, grouped under antri/, pembeli/, and penjual/
```

`config.DatabaseConnection` is the single JDBC connection provider used by
database access classes. No separate per-table connection classes are used.

## Data behavior

Catalog, orders, order items, payments, and both status histories are read and
written in MySQL. A checkout inserts the order, item rows, payment, and initial
status history in one JDBC transaction; stock validation/decrement participates
in the same transaction. Moving an order to `Diproses` also approves a waiting
QRIS/transfer payment or marks a waiting COD payment successful, recording that
change in payment history in the same transaction. Order and payment status
updates are recorded in their respective history tables. Existing database
records are not imported, overwritten, or deleted during startup.

The receipt feature may still save a user-requested text receipt under the
application data directory; it is separate from application transaction data.
