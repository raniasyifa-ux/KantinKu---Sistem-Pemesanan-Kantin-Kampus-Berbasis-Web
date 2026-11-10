package dao.impl;

import config.AppConfig;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import models.Menu;
import models.MenuItem;

public final class KatalogRepository {
    private final Path folder = AppConfig.dataDirectory();
    private final Path file = folder.resolve("menus.tsv");
    private final Path lockFile = folder.resolve("menus.lock");

    public Path file() {
        return file;
    }

    public long changeMarker() {
        try {
            return Files.exists(file) ? Files.getLastModifiedTime(file).toMillis() * 31 + Files.size(file) : -1;
        } catch (IOException exception) {
            return -1;
        }
    }

    public Optional<List<Menu>> readActiveBuyerCatalog() throws IOException {
        return withLock(() -> {
            if (!Files.exists(file)) {
                return Optional.empty();
            }
            List<Menu> result = new ArrayList<>();
            for (MenuItem item : readSellerCatalogUnlocked()) {
                if (item.isActive()) {
                    result.add(new Menu(item.getName(), item.getPrice(), item.getImageName(),
                            item.getCategory(), item.getDescription(), item.getStock()));
                }
            }
            return Optional.of(result);
        });
    }

    public List<MenuItem> readSellerCatalog() throws IOException {
        return withLock(this::readSellerCatalogUnlocked);
    }

    public void initializeFromBuyerCatalog(List<Menu> menus) throws IOException {
        withLock(() -> {
            if (!Files.exists(file)) {
                List<MenuItem> items = new ArrayList<>();
                for (Menu menu : menus) {
                    String id = menu.getNama().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
                    items.add(new MenuItem(id, menu.getNama(), menu.getDeskripsi(), menu.getHarga(),
                            menu.getKategori(), menu.getNamaGambar(), true, menu.getStok()));
                }
                writeSellerCatalog(items);
            }
            return null;
        });
    }

    public void initializeFromSellerCatalog(List<MenuItem> items) throws IOException {
        withLock(() -> {
            if (!Files.exists(file)) {
                writeSellerCatalog(items);
            }
            return null;
        });
    }

    public void add(MenuItem item) throws IOException {
        withLock(() -> {
            List<MenuItem> items = readSellerCatalogUnlocked();
            items.add(item);
            writeSellerCatalog(items);
            return null;
        });
    }

    public void update(MenuItem updated) throws IOException {
        withLock(() -> {
            List<MenuItem> items = readSellerCatalogUnlocked();
            for (int index = 0; index < items.size(); index++) {
                if (items.get(index).getId().equals(updated.getId())) {
                    items.set(index, updated);
                    writeSellerCatalog(items);
                    return null;
                }
            }
            throw new IOException("Menu yang akan diperbarui tidak ditemukan.");
        });
    }

    public void setActive(String id, boolean active) throws IOException {
        withLock(() -> {
            List<MenuItem> items = readSellerCatalogUnlocked();
            for (MenuItem item : items) {
                if (item.getId().equals(id)) {
                    item.setActive(active);
                    writeSellerCatalog(items);
                    return null;
                }
            }
            throw new IOException("Menu yang akan diperbarui tidak ditemukan.");
        });
    }

    public void remove(String id) throws IOException {
        withLock(() -> {
            List<MenuItem> items = readSellerCatalogUnlocked();
            boolean removed = items.removeIf(item -> item.getId().equals(id));
            if (!removed) {
                throw new IOException("Menu yang akan dihapus tidak ditemukan.");
            }
            writeSellerCatalog(items);
            return null;
        });
    }

    public String decreaseStock(Map<String, Integer> requestedItems) throws IOException {
        return withLock(() -> {
            if (!Files.exists(file)) {
                return "Katalog menu tidak ditemukan.";
            }
            List<String> lines = new ArrayList<>(Files.readAllLines(file, StandardCharsets.UTF_8));
            for (Map.Entry<String, Integer> request : requestedItems.entrySet()) {
                int index = findMenuLine(lines, request.getKey());
                if (index < 0) {
                    return "Menu \"" + request.getKey() + "\" sudah tidak tersedia.";
                }
                String[] fields = lines.get(index).split("\\t", -1);
                if (!isActive(fields)) {
                    return "Menu \"" + request.getKey() + "\" sedang tidak dijual.";
                }
                int stock = stockFrom(fields);
                if (stock < request.getValue()) {
                    return stock <= 0 ? "Stok \"" + request.getKey() + "\" sudah habis."
                            : "Stok \"" + request.getKey() + "\" tinggal " + stock + " porsi.";
                }
            }
            for (Map.Entry<String, Integer> request : requestedItems.entrySet()) {
                int index = findMenuLine(lines, request.getKey());
                String[] fields = lines.get(index).split("\\t", -1);
                lines.set(index, lineWithStock(fields, stockFrom(fields) - request.getValue()));
            }
            writeLines(lines);
            return null;
        });
    }

    public void restoreStock(Map<String, Integer> requestedItems) throws IOException {
        withLock(() -> {
            if (!Files.exists(file)) {
                return null;
            }
            List<String> lines = new ArrayList<>(Files.readAllLines(file, StandardCharsets.UTF_8));
            for (Map.Entry<String, Integer> request : requestedItems.entrySet()) {
                int index = findMenuLine(lines, request.getKey());
                if (index < 0) {
                    continue;
                }
                String[] fields = lines.get(index).split("\\t", -1);
                lines.set(index, lineWithStock(fields, stockFrom(fields) + request.getValue()));
            }
            writeLines(lines);
            return null;
        });
    }

    private List<MenuItem> readSellerCatalogUnlocked() throws IOException {
        List<MenuItem> items = new ArrayList<>();
        if (!Files.exists(file)) {
            return items;
        }
        int lineNumber = 0;
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            lineNumber++;
            if (line.isBlank()) {
                continue;
            }
            String[] fields = line.split("\\t", -1);
            if (fields.length < 6 || fields.length > 8) {
                throw new IOException("Format katalog tidak valid pada baris " + lineNumber + ".");
            }
            try {
                String id = decode(fields[0]);
                items.add(new MenuItem(
                        id,
                        decode(fields[1]),
                        decode(fields[2]),
                        Integer.parseInt(fields[3]),
                        decode(fields[4]),
                        fields.length >= 7 ? decode(fields[5]) : Menu.gambarBawaanUntukId(id),
                        Boolean.parseBoolean(fields.length >= 7 ? fields[6] : fields[5]),
                        fields.length == 8 ? Math.max(0, Integer.parseInt(fields[7].trim())) : Menu.STOK_AWAL));
            } catch (IllegalArgumentException exception) {
                throw new IOException("Data katalog tidak valid pada baris " + lineNumber + ".", exception);
            }
        }
        return items;
    }

    private void writeSellerCatalog(List<MenuItem> items) throws IOException {
        List<String> lines = new ArrayList<>();
        for (MenuItem item : items) {
            lines.add(String.join("\t",
                    encode(item.getId()),
                    encode(item.getName()),
                    encode(item.getDescription()),
                    Integer.toString(item.getPrice()),
                    encode(item.getCategory()),
                    encode(item.getImageName()),
                    Boolean.toString(item.isActive()),
                    Integer.toString(item.getStock())));
        }
        writeLines(lines);
    }

    private int findMenuLine(List<String> lines, String menuName) {
        for (int index = 0; index < lines.size(); index++) {
            String[] fields = lines.get(index).split("\\t", -1);
            if (fields.length < 6) {
                continue;
            }
            try {
                if (decode(fields[1]).equalsIgnoreCase(menuName)) {
                    return index;
                }
            } catch (IllegalArgumentException ignored) {
                // Skip malformed legacy catalog entries.
            }
        }
        return -1;
    }

    private static int stockFrom(String[] fields) {
        if (fields.length >= 8) {
            try {
                return Integer.parseInt(fields[7].trim());
            } catch (NumberFormatException ignored) {
                // Older catalog rows use the initial stock value.
            }
        }
        return Menu.STOK_AWAL;
    }

    private static String lineWithStock(String[] fields, int stock) {
        String[] result = new String[8];
        if (fields.length == 6) {
            System.arraycopy(fields, 0, result, 0, 5);
            result[5] = encode(Menu.gambarBawaanUntukId(decode(fields[0])));
            result[6] = fields[5];
        } else {
            System.arraycopy(fields, 0, result, 0, 7);
        }
        result[7] = Integer.toString(Math.max(0, stock));
        return String.join("\t", result);
    }

    private static boolean isActive(String[] fields) {
        return Boolean.parseBoolean(fields[fields.length >= 7 ? 6 : 5]);
    }

    private <T> T withLock(IoWork<T> work) throws IOException {
        Files.createDirectories(folder);
        try (RandomAccessFile raf = new RandomAccessFile(lockFile.toFile(), "rw");
                FileChannel channel = raf.getChannel();
                FileLock ignored = channel.lock()) {
            return work.run();
        }
    }

    private void writeLines(List<String> lines) throws IOException {
        Files.createDirectories(folder);
        Path temporaryFile = Files.createTempFile(folder, "menus-", ".tmp");
        try {
            Files.write(temporaryFile, lines, StandardCharsets.UTF_8);
            try {
                Files.move(temporaryFile, file, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private interface IoWork<T> {
        T run() throws IOException;
    }
}
