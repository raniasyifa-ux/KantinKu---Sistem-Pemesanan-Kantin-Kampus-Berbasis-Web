package tests;

import config.DatabaseConfig;
import config.DatabaseConnection;
import config.DatabaseStartup;
import dao.impl.*;
import models.*;
import services.*;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public class TestRunner {
    public static void main(String[] args) {
        System.out.println("=== TESTING KANTINKU DATABASE & FUNCTIONS ===");
        try {
            // 1. Test Database Connection
            System.out.print("[1] Testing DatabaseStartup.initialize()... ");
            DatabaseStartup.initialize();
            System.out.println("OK (Connected to db_kantinkudesktop)");

            // 2. Test MenuDAOImpl
            System.out.print("[2] Testing MenuDAOImpl.readAll()... ");
            MenuDAOImpl menuDAO = new MenuDAOImpl();
            List<MenuItem> allMenus = menuDAO.readAll();
            System.out.println("OK (Found " + allMenus.size() + " menus)");

            System.out.print("[3] Testing MenuDAOImpl.readActiveBuyerCatalog()... ");
            Optional<List<Menu>> activeMenus = menuDAO.readActiveBuyerCatalog();
            System.out.println("OK (Found " + activeMenus.map(List::size).orElse(0) + " active menus)");

            System.out.print("[4] Testing MenuDAOImpl.changeMarker()... ");
            long menuMarker = menuDAO.changeMarker();
            System.out.println("OK (Marker: " + menuMarker + ")");

            // 3. Test UserDAOImpl
            System.out.print("[5] Testing UserDAOImpl.loadSellers()... ");
            UserDAOImpl userDAO = new UserDAOImpl();
            List<Account> sellers = userDAO.loadSellers();
            System.out.println("OK (Found " + sellers.size() + " sellers)");

            // 4. Test PesananDAOImpl
            System.out.print("[6] Testing PesananDAOImpl.readAll()... ");
            PesananDAOImpl pesananDAO = new PesananDAOImpl();
            List<Pesanan> allOrders = pesananDAO.readAll();
            System.out.println("OK (Found " + allOrders.size() + " orders)");

            System.out.print("[7] Testing PesananDAOImpl.changeMarker()... ");
            long orderMarker = pesananDAO.changeMarker();
            System.out.println("OK (Marker: " + orderMarker + ")");

            System.out.print("[8] Testing PesananDAOImpl.readBuyers()... ");
            List<Pembeli> buyers = pesananDAO.readBuyers();
            System.out.println("OK (Found " + buyers.size() + " buyers)");

            // 5. Test Services
            System.out.print("[9] Testing KatalogBersama.bacaMenuAktif()... ");
            Optional<List<Menu>> catalogMenus = KatalogBersama.bacaMenuAktif();
            System.out.println("OK (Found " + catalogMenus.map(List::size).orElse(0) + " menus)");

            System.out.print("[10] Testing PesananBersama.baca()... ");
            List<Pesanan> ordersFromService = PesananBersama.baca();
            System.out.println("OK (Found " + ordersFromService.size() + " orders)");

            System.out.println("ALL BASIC FUNCTIONS PASSED SUCCESSFULLY!");
        } catch (Exception e) {
            System.err.println("FAILED WITH EXCEPTION:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
