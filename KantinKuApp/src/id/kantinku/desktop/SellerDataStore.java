package id.kantinku.desktop;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

final class SellerDataStore {
    private final Path catalogFile = Path.of(
            System.getProperty("user.home"), ".kantinku-desktop", "menus.tsv");
    private final List<MenuItem> catalog;

    SellerDataStore() throws IOException {
        if (Files.exists(catalogFile)) {
            catalog = loadCatalog();
        } else {
            catalog = createInitialCatalog();
            saveCatalog();
        }
    }

    List<MenuItem> getCatalog() {
        return new ArrayList<>(catalog);
    }

    int getActiveMenuCount() {
        return (int) catalog.stream().filter(MenuItem::isActive).count();
    }

    void addMenu(MenuItem item) throws IOException {
        catalog.add(item);
        saveCatalog();
    }

    void updateMenu(MenuItem updated) throws IOException {
        for (int index = 0; index < catalog.size(); index++) {
            if (catalog.get(index).getId().equals(updated.getId())) {
                catalog.set(index, updated);
                saveCatalog();
                return;
            }
        }
        throw new IOException("Menu yang akan diperbarui tidak ditemukan.");
    }

    void removeMenu(String id) throws IOException {
        boolean removed = catalog.removeIf(item -> item.getId().equals(id));
        if (!removed) {
            throw new IOException("Menu yang akan dihapus tidak ditemukan.");
        }
        saveCatalog();
    }

    void saveMenu(MenuItem item) throws IOException {
        updateMenu(item);
    }

    static MenuItem newMenu(String name, String description, int price, String category) {
        String imageName = category.equals("Makanan") ? "PEMPEK.jpeg" : "ES TEH.jpeg";
        return newMenu(name, description, price, category, imageName);
    }

    static MenuItem newMenu(String name, String description, int price, String category, String imageName) {
        return new MenuItem(UUID.randomUUID().toString(), name, description, price, category, imageName, true);
    }

    private List<MenuItem> loadCatalog() throws IOException {
        List<MenuItem> loaded = new ArrayList<>();
        int lineNumber = 0;
        for (String line : Files.readAllLines(catalogFile, StandardCharsets.UTF_8)) {
            lineNumber++;
            if (line.isBlank()) {
                continue;
            }
            String[] fields = line.split("\\t", -1);
            if (fields.length != 6 && fields.length != 7) {
                throw new IOException("Format katalog tidak valid pada baris " + lineNumber + ".");
            }
            try {
                loaded.add(new MenuItem(
                        decode(fields[0]),
                        decode(fields[1]),
                        decode(fields[2]),
                        Integer.parseInt(fields[3]),
                        decode(fields[4]),
                        fields.length == 7 ? decode(fields[5]) : imageNameForId(decode(fields[0])),
                        Boolean.parseBoolean(fields[fields.length - 1])));
            } catch (IllegalArgumentException exception) {
                throw new IOException("Data katalog tidak valid pada baris " + lineNumber + ".", exception);
            }
        }
        return loaded;
    }

    private void saveCatalog() throws IOException {
        Files.createDirectories(catalogFile.getParent());
        Path temporaryFile = Files.createTempFile(catalogFile.getParent(), "menus-", ".tmp");
        try {
            List<String> lines = new ArrayList<>();
            for (MenuItem item : catalog) {
                lines.add(String.join("\t",
                        encode(item.getId()),
                        encode(item.getName()),
                        encode(item.getDescription()),
                        Integer.toString(item.getPrice()),
                        encode(item.getCategory()),
                        encode(item.getImageName()),
                        Boolean.toString(item.isActive())));
            }
            Files.write(temporaryFile, lines, StandardCharsets.UTF_8);
            try {
                Files.move(temporaryFile, catalogFile, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, catalogFile, StandardCopyOption.REPLACE_EXISTING);
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

    /** Katalog awal sama persis dengan menu bawaan aplikasi pembeli, agar menu keduanya selalu sinkron. */
    private static List<MenuItem> createInitialCatalog() {
        List<MenuItem> items = new ArrayList<>();
        addMenu(items, "Nasi Goreng", "Nasi goreng gurih dengan telur dan ayam.", 15000, "Makanan", "NASI GORENG.jpeg");
        addMenu(items, "Mie Ayam", "Mie lembut dengan ayam gurih dan kuah.", 12000, "Makanan", "MIE AYAM.jpeg");
        addMenu(items, "Pempek", "Pempek ikan dengan kuah cuko khas.", 15000, "Makanan", "PEMPEK.jpeg");
        addMenu(items, "Siomay", "Siomay lengkap dengan saus kacang.", 13000, "Makanan", "SIOMAY.jpeg");
        addMenu(items, "Ayam Bakar", "Ayam bakar bumbu meresap manis gurih.", 18000, "Makanan", "AYAM BAKAR.jpeg");
        addMenu(items, "Batagor", "Bakso tahu goreng renyah saus kacang.", 10000, "Makanan", "BATAGOR.jpeg");
        addMenu(items, "Soto", "Soto kuah hangat dengan koya renyah.", 14000, "Makanan", "SOTO.jpeg");
        addMenu(items, "Risol Mayo", "Risol isian mayones dan daging gurih.", 8000, "Makanan", "RISOL MAYO.jpeg");
        addMenu(items, "Kentang Goreng", "Kentang goreng stik renyah gurih.", 10000, "Makanan", "KENTANG GORENG.jpeg");
        addMenu(items, "Matcha", "Minuman matcha susu manis dingin.", 8000, "Minuman", "MATCHA.jpeg");
        addMenu(items, "Cappucino", "Kopi cappucino racikan lembut bersusu.", 8000, "Minuman", "CAPPUCINO.jpeg");
        addMenu(items, "Coklat", "Coklat pekat manis dingin segar.", 8000, "Minuman", "COKLAT.jpeg");
        addMenu(items, "Kopi Susu", "Kopi susu gula aren khas KantinKu.", 8000, "Minuman", "KOPI SUSU.jpeg");
        addMenu(items, "Thai Tea", "Teh Thailand autentik manis segar.", 7000, "Minuman", "THAI TEA.jpeg");
        addMenu(items, "Es Jeruk", "Perasan jeruk peras alami manis dingin.", 5000, "Minuman", "ES JERUK.jpeg");
        addMenu(items, "Es Teh", "Es teh manis penyegar dahaga.", 4000, "Minuman", "ES TEH.jpeg");
        return items;
    }

    private static void addMenu(List<MenuItem> items, String name, String description, int price,
            String category, String imageName) {
        String id = name.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        items.add(new MenuItem(id, name, description, price, category, imageName, true));
    }

    private static void add(List<MenuItem> items, String id, String name, String description, int price,
            String category) {
        items.add(new MenuItem(id, name, description, price, category, imageNameForId(id), true));
    }

    private static String imageNameForId(String id) {
        switch (id) {
            case "nasi-goreng": return "NASI GORENG.jpeg";
            case "mie-ayam": return "MIE AYAM.jpeg";
            case "pempek": return "PEMPEK.jpeg";
            case "siomay": return "SIOMAY.jpeg";
            case "batagor": return "BATAGOR.jpeg";
            case "risol-mayo": return "RISOL MAYO.jpeg";
            case "ayam-geprek": return "GEPREK.jpeg";
            case "soto-ayam": return "SOTO.jpeg";
            case "nasi-ayam-bakar": return "AYAM BAKAR.jpeg";
            case "kentang-goreng": return "KENTANG GORENG.jpeg";
            case "es-teh": return "ES TEH.jpeg";
            case "es-jeruk": return "ES JERUK.jpeg";
            case "es-milo": return "milo.jpeg";
            case "milky-regal": return "REGAL.jpeg";
            case "es-kopi-susu": return "KOPI SUSU.jpeg";
            case "thai-tea": return "THAI TEA.jpeg";
            case "matcha-latte": return "MATCHA.jpeg";
            case "cappuccino": return "CAPPUCINO.jpeg";
            case "chocolate": return "COKLAT.jpeg";
            case "jus-alpukat": return "ALPUKAT.jpeg";
            default: return "";
        }
    }
}
