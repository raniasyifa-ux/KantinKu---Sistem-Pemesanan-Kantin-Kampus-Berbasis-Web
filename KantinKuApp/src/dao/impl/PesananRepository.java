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
import models.Pembeli;
import models.Pesanan;

public final class PesananRepository {
    private final Path folder = AppConfig.dataDirectory();
    private final Path file = folder.resolve("orders.tsv");
    private final Path lockFile = folder.resolve("orders.lock");
    private final Path buyerOrderIdsFile = folder.resolve("orders-pembeli.txt");
    private final Path buyersFile = folder.resolve("buyers.tsv");

    public synchronized List<Pembeli> readBuyers() throws IOException {
        List<Pembeli> buyers = new ArrayList<>();
        if (!Files.exists(buyersFile)) {
            return buyers;
        }
        for (String line : Files.readAllLines(buyersFile, StandardCharsets.UTF_8)) {
            if (line.isBlank()) {
                continue;
            }
            String[] fields = line.split("\\t", -1);
            if (fields.length < 2) {
                continue;
            }
            try {
                buyers.add(new Pembeli(decode(fields[0]), decode(fields[1])));
            } catch (RuntimeException ignored) {
                // Skip malformed legacy buyer entries.
            }
        }
        return buyers;
    }

    public synchronized void saveBuyer(String name, String phone) throws IOException {
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isEmpty()) {
            return;
        }
        withLock(() -> {
            List<Pembeli> buyers = readBuyers();
            buyers.removeIf(buyer -> buyer.nama.equalsIgnoreCase(cleanName));
            buyers.add(new Pembeli(cleanName, phone == null ? "" : phone.trim()));
            List<String> lines = new ArrayList<>();
            for (Pembeli buyer : buyers) {
                lines.add(encode(buyer.nama) + "\t" + encode(buyer.hp));
            }
            writeAtomically(buyersFile, lines, "buyers-");
            return null;
        });
    }

    public synchronized List<Pesanan> readOrders() throws IOException {
        return readOrdersUnlocked();
    }

    public synchronized List<Pesanan> readOrdersForBuyer(String name) throws IOException {
        List<Pesanan> orders = new ArrayList<>();
        if (name == null || name.isBlank()) {
            return orders;
        }
        for (Pesanan order : readOrdersUnlocked()) {
            if (order.nama.trim().equalsIgnoreCase(name.trim())) {
                orders.add(0, order);
            }
        }
        return orders;
    }

    public synchronized Pesanan saveNewOrder(Pesanan order) throws IOException {
        return withLock(() -> {
            List<Pesanan> orders = readOrdersUnlocked();
            boolean duplicateId = false;
            int maximumNumber = 0;
            for (Pesanan existing : orders) {
                maximumNumber = Math.max(maximumNumber, orderNumber(existing.id));
                if (existing.id.equals(order.id)) {
                    duplicateId = true;
                }
            }
            if (duplicateId) {
                order.id = formatId(maximumNumber + 1);
            }
            orders.add(order);
            writeOrders(orders);
            return order;
        });
    }

    public synchronized void saveOrder(Pesanan order) throws IOException {
        withLock(() -> {
            List<Pesanan> orders = readOrdersUnlocked();
            boolean replaced = false;
            for (int index = 0; index < orders.size(); index++) {
                if (orders.get(index).id.equals(order.id)) {
                    orders.set(index, order);
                    replaced = true;
                    break;
                }
            }
            if (!replaced) {
                orders.add(order);
            }
            writeOrders(orders);
            return null;
        });
    }

    public synchronized void updateOrderStatus(String id, String status) throws IOException {
        withLock(() -> {
            List<Pesanan> orders = readOrdersUnlocked();
            boolean found = false;
            for (Pesanan order : orders) {
                if (order.id.equals(id)) {
                    order.status = status;
                    found = true;
                }
            }
            if (!found) {
                throw new IOException("Pesanan " + id + " tidak ditemukan.");
            }
            writeOrders(orders);
            return null;
        });
    }

    public synchronized void deleteOrder(String id) throws IOException {
        withLock(() -> {
            List<Pesanan> orders = readOrdersUnlocked();
            boolean removed = orders.removeIf(order -> order.id.equals(id));
            if (!removed) {
                throw new IOException("Pesanan " + id + " tidak ditemukan.");
            }
            writeOrders(orders);
            return null;
        });
    }

    public synchronized Pesanan findOrder(String id) throws IOException {
        for (Pesanan order : readOrdersUnlocked()) {
            if (order.id.equals(id)) {
                return order;
            }
        }
        return null;
    }

    public synchronized int nextOrderNumber() throws IOException {
        int maximumNumber = 0;
        for (Pesanan order : readOrdersUnlocked()) {
            maximumNumber = Math.max(maximumNumber, orderNumber(order.id));
        }
        return maximumNumber + 1;
    }

    public synchronized List<String> readBuyerOrderIds() throws IOException {
        List<String> ids = new ArrayList<>();
        if (Files.exists(buyerOrderIdsFile)) {
            for (String line : Files.readAllLines(buyerOrderIdsFile, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) {
                    ids.add(line.trim());
                }
            }
        }
        return ids;
    }

    public synchronized void addBuyerOrderId(String id) throws IOException {
        withLock(() -> {
            List<String> ids = readBuyerOrderIds();
            if (!ids.contains(id)) {
                ids.add(id);
                writeAtomically(buyerOrderIdsFile, ids, "orders-pembeli-");
            }
            return null;
        });
    }

    public long changeMarker() {
        try {
            return Files.exists(file) ? Files.getLastModifiedTime(file).toMillis() * 31 + Files.size(file) : -1;
        } catch (IOException exception) {
            return -1;
        }
    }

    private List<Pesanan> readOrdersUnlocked() throws IOException {
        List<Pesanan> orders = new ArrayList<>();
        if (!Files.exists(file)) {
            return orders;
        }
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.isBlank()) {
                continue;
            }
            String[] fields = line.split("\t", -1);
            if (fields.length < 6 || fields.length > 8) {
                continue;
            }
            try {
                orders.add(new Pesanan(
                        decode(fields[0]),
                        decode(fields[1]),
                        decode(fields[2]),
                        Integer.parseInt(fields[3]),
                        decode(fields[4]),
                        decode(fields[5]),
                        fields.length >= 7 ? decode(fields[6]) : "",
                        fields.length >= 8 ? decode(fields[7]) : ""));
            } catch (RuntimeException ignored) {
                // Skip malformed legacy order entries.
            }
        }
        return orders;
    }

    private void writeOrders(List<Pesanan> orders) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Pesanan order : orders) {
            lines.add(String.join("\t",
                    encode(order.id),
                    encode(order.nama),
                    encode(order.metode),
                    Integer.toString(order.total),
                    encode(order.status),
                    encode(order.rincian),
                    encode(order.hp),
                    encode(order.waktu)));
        }
        writeAtomically(file, lines, "orders-");
    }

    private <T> T withLock(IoWork<T> work) throws IOException {
        Files.createDirectories(folder);
        try (RandomAccessFile raf = new RandomAccessFile(lockFile.toFile(), "rw");
                FileChannel channel = raf.getChannel();
                FileLock ignored = channel.lock()) {
            return work.run();
        }
    }

    private static void writeAtomically(Path destination, List<String> lines, String prefix) throws IOException {
        Files.createDirectories(destination.getParent());
        Path temporaryFile = Files.createTempFile(destination.getParent(), prefix, ".tmp");
        try {
            Files.write(temporaryFile, lines, StandardCharsets.UTF_8);
            try {
                Files.move(temporaryFile, destination, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private static String formatId(int number) {
        return "#ORD-" + String.format("%03d", number);
    }

    private static int orderNumber(String id) {
        String digits = id == null ? "" : id.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException exception) {
            return 0;
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
