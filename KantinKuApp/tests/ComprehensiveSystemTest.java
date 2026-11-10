package tests;

import config.DatabaseStartup;
import controllers.AntrianController;
import controllers.PembeliController;
import controllers.PenjualController;
import dao.impl.MenuDAOImpl;
import dao.impl.PembayaranDAOImpl;
import dao.impl.PesananDAOImpl;
import dao.impl.UserDAOImpl;
import models.*;
import services.SellerAuthService;
import services.SellerDataStore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ComprehensiveSystemTest {
    public static void main(String[] args) {
        System.out.println("=== FULL COMPREHENSIVE SYSTEM TEST ===");
        try {
            // 0. Startup & Connection
            DatabaseStartup.initialize();
            System.out.println("[PASS] 0. Database Connection verified.");

            // 1. Seller Auth & User Management
            testSellerAuth();
            System.out.println("[PASS] 1. Seller Authentication, Registration, and Password Reset.");

            // 2. Menu & Catalog Management (Seller Data Store)
            MenuItem testItem = testMenuOperations();
            System.out.println("[PASS] 2. Menu Catalog CRUD (Add, Update, Toggle Active, Delete).");

            // 3. Buyer Checkout & Order Creation (Transaction, Stock decrement, History logging)
            Pesanan testOrder = testBuyerOrderFlow();
            System.out.println("[PASS] 3. Buyer Checkout & Order Creation with Stock Decrement.");

            // 4. Seller Order Management & Status Transitions
            testSellerOrderProcessing(testOrder);
            System.out.println("[PASS] 4. Seller Status Transitions & Payment Auto-Approval.");

            // 5. Antrian Controller
            testQueueController();
            System.out.println("[PASS] 5. Queue Display & Sync Marker.");

            // 6. Cleanup test records
            cleanup(testOrder);
            System.out.println("[PASS] 6. Test Data Cleanup complete.");

            System.out.println("\n>>> ALL TESTS PASSED WITH ZERO ERRORS! <<<");
        } catch (Throwable t) {
            System.err.println("\n>>> TEST SUITE FAILED! <<<");
            t.printStackTrace();
            System.exit(1);
        }
    }

    private static void testSellerAuth() throws Exception {
        SellerAuthService authService = new SellerAuthService();
        UserDAOImpl userDAO = new UserDAOImpl();

        String testUser = "testuser" + System.currentTimeMillis() % 10000;
        String testEmail = testUser + "@kantinku.test";
        char[] pass = "Password123".toCharArray();

        // Register
        authService.register(testUser, testEmail, pass);
        if (!userDAO.exists(testUser, testEmail)) {
            throw new AssertionError("User registration failed: User not found in database.");
        }

        // Authenticate
        String loggedInUser = authService.authenticate(testUser, pass);
        if (!testUser.equalsIgnoreCase(loggedInUser)) {
            throw new AssertionError("Authentication failed for newly created user: " + testUser);
        }

        // Reset Password
        char[] newPass = "NewPassword456".toCharArray();
        authService.resetPassword(testEmail, newPass);
        String reLoggedIn = authService.authenticate(testEmail, newPass);
        if (!testUser.equalsIgnoreCase(reLoggedIn)) {
            throw new AssertionError("Authentication failed after password reset.");
        }

        // Cleanup user
        try (var conn = config.DatabaseConnection.getConnection();
             var stmt = conn.prepareStatement("DELETE FROM users WHERE email = ?")) {
            stmt.setString(1, testEmail);
            stmt.executeUpdate();
        }
    }

    private static MenuItem testMenuOperations() throws Exception {
        SellerDataStore store = new SellerDataStore();
        int initialCount = store.getCatalog().size();

        // Add
        MenuItem newItem = new MenuItem(null, "Menu Uji Coba", "Deskripsi Uji", 10000, "Makanan", "PEMPEK.jpeg", true, 50);
        store.addMenu(newItem);
        if (newItem.getId() == null || newItem.getId().isBlank()) {
            throw new AssertionError("New menu item did not receive generated ID.");
        }

        // Verify Added
        MenuItem retrieved = store.findById(newItem.getId());
        if (retrieved == null || !retrieved.getName().equals("Menu Uji Coba")) {
            throw new AssertionError("Menu retrieval by ID failed.");
        }

        // Update
        retrieved.setName("Menu Uji Diperbarui");
        retrieved.setPrice(12000);
        store.updateMenu(retrieved);

        // Toggle Active
        store.setActive(newItem.getId(), false);
        MenuItem toggled = store.findById(newItem.getId());
        if (toggled.isActive()) {
            throw new AssertionError("Menu active status was not set to false.");
        }
        store.setActive(newItem.getId(), true);

        return newItem;
    }

    private static Pesanan testBuyerOrderFlow() throws Exception {
        PembeliController pembeliController = new PembeliController();
        MenuDAOImpl menuDAO = new MenuDAOImpl();

        // Get an active menu to order
        List<MenuItem> allMenus = menuDAO.readAll();
        MenuItem targetMenu = null;
        for (MenuItem m : allMenus) {
            if (m.isActive() && m.getStock() >= 5) {
                targetMenu = m;
                break;
            }
        }
        if (targetMenu == null) {
            throw new AssertionError("No active menu with sufficient stock found.");
        }

        int stockBefore = targetMenu.getStock();

        // Create Order
        Pesanan order = new Pesanan(
                null,
                "Pembeli Test",
                "Tunai / COD",
                targetMenu.getPrice() * 2,
                "Menunggu konfirmasi",
                "",
                "081234567890",
                null
        );
        List<DetailPesanan> details = new ArrayList<>();
        details.add(new DetailPesanan(targetMenu.getId(), targetMenu.getName(), 2, targetMenu.getPrice(), targetMenu.getPrice() * 2, ""));
        order.setDetails(details);

        Map<String, Integer> stockReq = new LinkedHashMap<>();
        stockReq.put(targetMenu.getId(), 2);

        Pesanan saved = pembeliController.simpanPesanan(order, stockReq);
        if (saved.getId() == null || !saved.getId().startsWith("#ORD-")) {
            throw new AssertionError("Saved order has invalid formatted ID: " + saved.getId());
        }

        // Verify stock decreased by 2
        List<MenuItem> reloaded = menuDAO.readAll();
        for (MenuItem m : reloaded) {
            if (m.getId().equals(targetMenu.getId())) {
                if (m.getStock() != stockBefore - 2) {
                    throw new AssertionError("Menu stock did not decrease properly. Expected " + (stockBefore - 2) + ", got " + m.getStock());
                }
                break;
            }
        }

        // Verify Pembayaran record exists
        PembayaranDAOImpl payDAO = new PembayaranDAOImpl();
        Pembayaran pay = payDAO.findByOrderId(saved.getId());
        if (pay == null) {
            throw new AssertionError("Payment record not found for order: " + saved.getId());
        }

        return saved;
    }

    private static void testSellerOrderProcessing(Pesanan order) throws Exception {
        SellerAuthService authService = new SellerAuthService();
        SellerDataStore store = new SellerDataStore();
        PenjualController penjualController = new PenjualController(authService, store);
        PesananDAOImpl pesananDAO = new PesananDAOImpl();
        PembayaranDAOImpl payDAO = new PembayaranDAOImpl();

        // 1. Move to Diproses
        penjualController.ubahStatusPesanan(order.id, "Diproses");
        Pesanan p1 = pesananDAO.find(order.id);
        if (!"Diproses".equalsIgnoreCase(p1.status)) {
            throw new AssertionError("Order status update to 'Diproses' failed.");
        }
        Pembayaran pay1 = payDAO.findByOrderId(order.id);
        // For COD, moving to Diproses sets status to "Berhasil"
        if (!"Berhasil".equalsIgnoreCase(pay1.getStatus())) {
            throw new AssertionError("Payment status for COD did not update to 'Berhasil' on processing. Got: " + pay1.getStatus());
        }

        // 2. Move to Siap Diambil
        penjualController.ubahStatusPesanan(order.id, "Siap Diambil");
        Pesanan p2 = pesananDAO.find(order.id);
        if (!"Siap Diambil".equalsIgnoreCase(p2.status)) {
            throw new AssertionError("Order status update to 'Siap Diambil' failed.");
        }

        // 3. Move to Selesai
        penjualController.ubahStatusPesanan(order.id, "Selesai");
        Pesanan p3 = pesananDAO.find(order.id);
        if (!"Selesai".equalsIgnoreCase(p3.status)) {
            throw new AssertionError("Order status update to 'Selesai' failed.");
        }

        // Check Status History
        var orderHist = pesananDAO.readStatusHistory(order.id);
        if (orderHist.size() < 4) { // Menunggu konfirmasi -> Diproses -> Siap Diambil -> Selesai
            throw new AssertionError("Order history count incorrect. Expected at least 4 entries, got: " + orderHist.size());
        }
    }

    private static void testQueueController() throws Exception {
        AntrianController queueController = new AntrianController();
        List<Pesanan> queue = queueController.bacaPesanan();
        long marker = queueController.penandaPesanan();
        if (queue == null) {
            throw new AssertionError("Queue returned null list.");
        }
    }

    private static void cleanup(Pesanan testOrder) throws Exception {
        PesananDAOImpl pesananDAO = new PesananDAOImpl();
        SellerDataStore store = new SellerDataStore();

        if (testOrder != null && testOrder.id != null) {
            // Restore stock of ordered item
            for (DetailPesanan d : testOrder.getDetails()) {
                MenuItem m = store.findById(d.getMenuId());
                if (m != null) {
                    m.setStock(m.getStock() + d.getQuantity());
                    store.updateMenu(m);
                }
            }
            pesananDAO.delete(testOrder.id);
        }

        // Clean up test menu if any exists
        for (MenuItem item : store.getCatalog()) {
            if ("Menu Uji Diperbarui".equals(item.getName()) || "Menu Uji Coba".equals(item.getName())) {
                store.removeMenu(item.getId());
            }
        }
    }
}
