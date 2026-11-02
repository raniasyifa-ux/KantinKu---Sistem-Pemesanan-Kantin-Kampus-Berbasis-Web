package id.kantinku.desktop;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.List;
import service.PesananBersama;
import service.PesananBersama.Pesanan;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

@SuppressWarnings("serial")
final class SellerDashboardFrame extends JFrame {
    private static final Color PRIMARY = new Color(0xC8553D);
    private static final Color PRIMARY_HOVER = new Color(0xA8432F);
    private static final Color PAGE_BACKGROUND = new Color(0xF3E5D2);
    private static final Color CARD_BACKGROUND = Color.WHITE;
    private static final Color TEXT = new Color(0x3B2A25);
    private static final Color MUTED = new Color(0x8A7B73);
    private static final Color BORDER = new Color(0xE8DDD0);
    private static final Color SAGE = new Color(0x3F5748);

    private final SellerDataStore dataStore;
    private long lastOrderMarker = Long.MIN_VALUE;
    private final String accountName;
    private final CardLayout panelLayout = new CardLayout();
    private final JPanel panelCards = new JPanel(panelLayout);
    private final JPanel orderResults = new JPanel();
    private final JPanel catalogResults = new JPanel();
    private final JTextField orderSearch = new PlaceholderTextField("Cari ID atau nama pembeli", 20);
    private final JComboBox<String> statusFilter = new JComboBox<>(
            new String[]{"Semua Status", "Menunggu Konfirmasi", "Diproses", "Siap Diambil", "Selesai"});
    private final JTextField catalogSearch = new PlaceholderTextField("Cari nama menu", 18);
    private final JComboBox<String> catalogCategory = new JComboBox<>(
            new String[]{"Semua Kategori", "Makanan", "Minuman"});
    private final JLabel catalogResultCount = new JLabel("Menu yang aktif akan muncul di halaman pembeli.");
    private final JLabel orderCount = new JLabel("0 pesanan");
    private final JLabel pendingOrdersValue = summaryValueLabel();
    private final JLabel totalOrdersValue = summaryValueLabel();
    private final JLabel processingOrdersValue = summaryValueLabel();
    private final JLabel completedOrdersValue = summaryValueLabel();
    private final JButton ordersTab = createTabButton("Pesanan", true);
    private final JButton catalogTab = createTabButton("Daftar menu", false);

    SellerDashboardFrame(SellerDataStore dataStore, String accountName) {
        super("Dashboard Penjual - KantinKu");
        this.dataStore = dataStore;
        this.accountName = accountName;
        view.JendelaUtil.daftarkan(this);
        setMinimumSize(new Dimension(900, 650));
        setSize(1200, 820);
        setLocationRelativeTo(null);

        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(PAGE_BACKGROUND);
        page.add(createNavbar(), BorderLayout.NORTH);
        page.add(createScrollableContent(), BorderLayout.CENTER);
        page.add(createFooter(), BorderLayout.SOUTH);
        setContentPane(page);
        refreshCatalog();
        refreshOrders();
        addFilterListeners();

        // Sinkronkan daftar dan angka ringkasan secara berkala saat dashboard terbuka.
        Timer orderRefreshTimer = new Timer(1500, event -> {
            long penanda = service.PesananBersama.penandaPerubahan();
            if (penanda != lastOrderMarker) {
                lastOrderMarker = penanda;
                refreshOrders();
            }
        });
        orderRefreshTimer.start();
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent event) {
                orderRefreshTimer.stop();
            }
        });
    }

    private JPanel createNavbar() {
        JPanel navbar = new JPanel(new BorderLayout(24, 0));
        navbar.setBackground(Color.WHITE);
        navbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(15, 38, 15, 38)));

        JLabel logo = new JLabel("<html><span style='color:#3B2A25'>Kantin</span>"
                + "<span style='color:#C8553D'>Ku</span></html>");
        logo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 27));
        navbar.add(logo, BorderLayout.WEST);

        JPanel links = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        links.setOpaque(false);
        JButton home = createNavLink("Beranda Toko", false);
        home.addActionListener(event -> JOptionPane.showMessageDialog(
                this, "Portal Beranda Toko tidak termasuk dalam aplikasi desktop ini.",
                "KantinKu", JOptionPane.INFORMATION_MESSAGE));
        links.add(home);
        links.add(createNavLink("Dashboard Toko", true));
        navbar.add(links, BorderLayout.CENTER);

        JPanel account = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 1));
        account.setOpaque(false);
        JLabel greeting = new JLabel("Hai, " + accountName);
        greeting.setForeground(MUTED);
        account.add(greeting);
        JButton logout = new JButton("Keluar");
        logout.setForeground(PRIMARY);
        logout.setBackground(Color.WHITE);
        logout.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        logout.setFocusPainted(false);
        logout.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PRIMARY, 2),
                new EmptyBorder(7, 16, 7, 16)));
        logout.addActionListener(event -> {
            dispose();
            new SellerLoginFrame(new SellerAuthService(), dataStore).setVisible(true);
        });
        account.add(logout);
        navbar.add(account, BorderLayout.EAST);
        return navbar;
    }

    private static JButton createNavLink(String text, boolean active) {
        JButton link = new JButton(text);
        link.setUI(new BasicButtonUI());
        link.setOpaque(true);
        link.setContentAreaFilled(true);
        link.setBackground(active ? new Color(0xF8E9DD) : Color.WHITE);
        link.setForeground(active ? PRIMARY : TEXT);
        link.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        link.setContentAreaFilled(false);
        link.setBorderPainted(false);
        link.setFocusPainted(false);
        return link;
    }

    private JScrollPane createScrollableContent() {
        JPanel centered = new JPanel(new GridBagLayout());
        centered.setBackground(PAGE_BACKGROUND);
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(PAGE_BACKGROUND);
        content.setBorder(new EmptyBorder(30, 20, 30, 20));
        content.setMaximumSize(new Dimension(1100, Integer.MAX_VALUE));
        content.add(createPageHeading());
        content.add(Box.createVerticalStrut(22));
        content.add(createSummary());
        content.add(Box.createVerticalStrut(20));
        content.add(createTabs());
        content.add(Box.createVerticalStrut(2));
        content.add(createPanels());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.anchor = GridBagConstraints.NORTH;
        centered.add(content, constraints);

        JScrollPane scroll = new JScrollPane(centered);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.getViewport().setBackground(PAGE_BACKGROUND);
        return scroll;
    }

    private JPanel createPageHeading() {
        JPanel heading = new JPanel(new BorderLayout(18, 0));
        heading.setOpaque(false);
        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel eyebrow = new JLabel("TOKO KANTINKU");
        eyebrow.setForeground(new Color(0x85514C));
        eyebrow.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        copy.add(eyebrow);
        JLabel title = new JLabel("Kelola toko");
        title.setForeground(TEXT);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
        copy.add(title);
        JLabel subtitle = new JLabel("Atur menu, cek pembayaran, dan kabari pembeli tentang pesanannya.");
        subtitle.setForeground(MUTED);
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        copy.add(subtitle);
        heading.add(copy, BorderLayout.CENTER);

        JButton addMenu = new JButton("＋ Tambah menu");
        stylePrimaryButton(addMenu);
        addMenu.addActionListener(event -> showMenuDialog(null));
        heading.add(addMenu, BorderLayout.EAST);
        return heading;
    }

    private JPanel createSummary() {
        JPanel stats = new JPanel(new GridLayout(1, 4, 14, 0));
        stats.setOpaque(false);
        addSummaryCard(stats, "Menunggu konfirmasi", pendingOrdersValue);
        addSummaryCard(stats, "Total pesanan", totalOrdersValue);
        addSummaryCard(stats, "Sedang diproses", processingOrdersValue);
        addSummaryCard(stats, "Pesanan selesai", completedOrdersValue);
        return stats;
    }

    private static JLabel summaryValueLabel() {
        JLabel value = new JLabel("0");
        value.setForeground(TEXT);
        value.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 27));
        return value;
    }

    private static void addSummaryCard(JPanel stats, String caption, JLabel value) {
        JPanel card = new RoundedPanel(new BorderLayout(), CARD_BACKGROUND, BORDER, 7);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(17, 17, 17, 17));
        JLabel label = new JLabel(caption);
        label.setForeground(MUTED);
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        card.add(label);
        card.add(Box.createVerticalStrut(8));
        card.add(value);
        stats.add(card);
    }

    private JPanel createTabs() {
        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        tabs.setOpaque(false);
        tabs.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
        ordersTab.addActionListener(event -> { switchPanel("orders"); refreshOrders(); });
        catalogTab.addActionListener(event -> switchPanel("catalog"));
        tabs.add(ordersTab);
        tabs.add(catalogTab);
        return tabs;
    }

    private JButton createTabButton(String title, boolean active) {
        JButton button = new JButton(title + " 0");
        button.setUI(new BasicButtonUI());
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setBackground(active ? new Color(0xE8EFE8) : Color.WHITE);
        button.setForeground(active ? SAGE : MUTED);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, active ? 2 : 0, 0, active ? new Color(0x536A5B) : BORDER),
                new EmptyBorder(12, 3, 12, 3)));
        return button;
    }

    private JPanel createPanels() {
        panelCards.setOpaque(false);
        panelCards.add(createOrdersPanel(), "orders");
        panelCards.add(createCatalogPanel(), "catalog");
        return panelCards;
    }

    private JPanel createOrdersPanel() {
        JPanel panel = createSectionPanel();
        JPanel section = new JPanel(new BorderLayout(0, 14));
        section.setOpaque(false);
        JPanel top = new JPanel(new BorderLayout(16, 10));
        top.setOpaque(false);
        JPanel title = new JPanel();
        title.setOpaque(false);
        title.setLayout(new BoxLayout(title, BoxLayout.Y_AXIS));
        JLabel heading = new JLabel("Pesanan Pembeli");
        heading.setForeground(TEXT);
        heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        title.add(heading);
        orderCount.setForeground(MUTED);
        orderCount.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        title.add(orderCount);
        top.add(title, BorderLayout.WEST);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filters.setOpaque(false);
        styleFilterField(orderSearch);
        styleFilterCombo(statusFilter);
        filters.add(orderSearch);
        filters.add(statusFilter);
        top.add(filters, BorderLayout.EAST);
        section.add(top, BorderLayout.NORTH);

        orderResults.setLayout(new BoxLayout(orderResults, BoxLayout.Y_AXIS));
        orderResults.setOpaque(false);
        JLabel empty = new JLabel("Belum ada pesanan yang cocok. Pesanan pembeli akan muncul di sini.");
        empty.setForeground(MUTED);
        empty.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        orderResults.add(empty);
        section.add(orderResults, BorderLayout.CENTER);
        panel.add(section, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createCatalogPanel() {
        JPanel panel = createSectionPanel();
        JPanel section = new JPanel(new BorderLayout(0, 14));
        section.setOpaque(false);
        JPanel top = new JPanel(new BorderLayout(16, 10));
        top.setOpaque(false);
        JPanel title = new JPanel();
        title.setOpaque(false);
        title.setLayout(new BoxLayout(title, BoxLayout.Y_AXIS));
        JLabel heading = new JLabel("Menu Toko");
        heading.setForeground(TEXT);
        heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        title.add(heading);
        catalogResultCount.setForeground(MUTED);
        catalogResultCount.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        title.add(catalogResultCount);
        top.add(title, BorderLayout.WEST);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filters.setOpaque(false);
        styleFilterField(catalogSearch);
        styleFilterCombo(catalogCategory);
        filters.add(catalogSearch);
        filters.add(catalogCategory);
        top.add(filters, BorderLayout.EAST);
        section.add(top, BorderLayout.NORTH);

        catalogResults.setLayout(new BoxLayout(catalogResults, BoxLayout.Y_AXIS));
        catalogResults.setOpaque(false);
        JScrollPane menuScroll = new JScrollPane(catalogResults);
        menuScroll.setBorder(BorderFactory.createEmptyBorder());
        menuScroll.setPreferredSize(new Dimension(700, 440));
        menuScroll.getVerticalScrollBar().setUnitIncrement(16);
        section.add(menuScroll, BorderLayout.CENTER);
        panel.add(section, BorderLayout.CENTER);
        return panel;
    }

    private static JPanel createSectionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        return panel;
    }

    private static void styleFilterField(JTextField field) {
        field.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        field.setPreferredSize(new Dimension(230, 38));
        field.setBackground(Color.WHITE);
        field.setForeground(TEXT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(8, 11, 8, 11)));
    }

    private static void styleFilterCombo(JComboBox<String> combo) {
        combo.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        combo.setPreferredSize(new Dimension(170, 38));
        combo.setBackground(Color.WHITE);
        combo.setForeground(TEXT);
    }

    private void addFilterListeners() {
        orderSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent event) {
                refreshOrders();
            }
        });
        statusFilter.addActionListener(event -> refreshOrders());
        catalogSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent event) {
                refreshCatalog();
            }
        });
        catalogCategory.addActionListener(event -> refreshCatalog());
    }

    private void switchPanel(String panel) {
        panelLayout.show(panelCards, panel);
        setTabActive(ordersTab, panel.equals("orders"));
        setTabActive(catalogTab, panel.equals("catalog"));
    }

    private void setTabActive(JButton button, boolean active) {
        button.setForeground(active ? SAGE : MUTED);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, active ? 2 : 0, 0,
                        active ? new Color(0x536A5B) : BORDER),
                new EmptyBorder(12, 3, 12, 3)));
    }

    private void refreshOrders() {
        List<Pesanan> all;
        lastOrderMarker = PesananBersama.penandaPerubahan();
        try {
            all = PesananBersama.baca();
        } catch (IOException exception) {
            orderResults.removeAll();
            JLabel error = new JLabel("Pesanan tidak dapat dibaca: " + exception.getMessage());
            error.setForeground(MUTED);
            orderResults.add(error);
            orderResults.revalidate();
            orderResults.repaint();
            return;
        }
        updateOrderSummary(all);
        String query = orderSearch.getText().trim().toLowerCase(Locale.ROOT);
        String selectedStatus = (String) statusFilter.getSelectedItem();
        // Pesanan yang sudah selesai tidak lagi ditampilkan di daftar dashboard toko.
        List<Pesanan> filtered = all.stream()
                .filter(p -> !p.status.equalsIgnoreCase("Selesai"))
                .filter(p -> selectedStatus == null || selectedStatus.equals("Semua Status") || p.status.equalsIgnoreCase(selectedStatus))
                .filter(p -> (p.id + " " + p.nama).toLowerCase(Locale.ROOT).contains(query))
                .collect(Collectors.toList());
        java.util.Collections.reverse(filtered); // pesanan terbaru di atas
        orderResults.removeAll();
        if (filtered.isEmpty()) {
            JLabel empty = new JLabel(all.isEmpty() ? "Belum ada pesanan. Pesanan akan muncul setelah pembeli menyelesaikan konfirmasi pembayaran." : "Tidak ada pesanan aktif yang cocok. Pesanan berstatus Selesai disembunyikan dari dashboard toko.");
            empty.setForeground(MUTED);
            empty.setBorder(new EmptyBorder(20, 4, 20, 4));
            orderResults.add(empty);
        } else {
            for (Pesanan p : filtered) {
                JPanel card = new RoundedPanel(new BorderLayout(12, 8), Color.WHITE, BORDER, 7);
                card.setBorder(new EmptyBorder(14, 16, 14, 16));
                JPanel details = new JPanel();
                details.setOpaque(false);
                details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
                JLabel title = new JLabel(p.id + "  ·  " + p.nama);
                title.setForeground(TEXT);
                title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
                JLabel info = new JLabel("Pembayaran: " + p.metode + "  ·  Total: " + formatPrice(p.total));
                info.setForeground(MUTED);
                JLabel items = new JLabel("<html>" + p.rincian.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>") + "</html>");
                items.setForeground(TEXT);
                JLabel status = new JLabel("Status: " + p.status);
                status.setForeground(PRIMARY);
                details.add(title);
                details.add(Box.createVerticalStrut(5));
                details.add(info);
                details.add(Box.createVerticalStrut(5));
                details.add(items);
                details.add(Box.createVerticalStrut(5));
                details.add(status);
                JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
                actions.setOpaque(false);
                String next = p.status.equalsIgnoreCase("Menunggu konfirmasi") ? "Diproses"
                        : p.status.equalsIgnoreCase("Diproses") ? "Siap Diambil"
                        : p.status.equalsIgnoreCase("Siap Diambil") ? "Selesai" : null;
                if (next != null) {
                    String buttonText = p.status.equalsIgnoreCase("Menunggu konfirmasi")
                            ? "Setujui / Konfirmasi pesanan" : "Ubah ke " + next;
                    JButton update = createMenuAction(buttonText);
                    update.addActionListener(event -> {
                        String message = p.status.equalsIgnoreCase("Menunggu konfirmasi")
                                ? "Setujui pesanan " + p.id + " dari " + p.nama + "?"
                                : "Ubah status pesanan " + p.id + " menjadi " + next + "?";
                        int answer = JOptionPane.showConfirmDialog(this, message, "Konfirmasi pesanan",
                                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                        if (answer != JOptionPane.YES_OPTION) return;
                        try {
                            PesananBersama.ubahStatus(p.id, next);
                            refreshOrders();
                            JOptionPane.showMessageDialog(this,
                                    p.status.equalsIgnoreCase("Menunggu konfirmasi")
                                            ? "Pesanan berhasil dikonfirmasi dan sekarang sedang diproses."
                                            : "Status pesanan berhasil diperbarui.",
                                    "KantinKu", JOptionPane.INFORMATION_MESSAGE);
                        } catch (IOException ex) {
                            JOptionPane.showMessageDialog(this, "Status pesanan gagal diperbarui: " + ex.getMessage(),
                                    "KantinKu", JOptionPane.ERROR_MESSAGE);
                        }
                    });
                    actions.add(update);
                }
                card.add(details, BorderLayout.CENTER);
                card.add(actions, BorderLayout.SOUTH);
                orderResults.add(card);
                orderResults.add(Box.createVerticalStrut(10));
            }
        }
        orderCount.setText(filtered.size() + " pesanan");
        ordersTab.setText("Pesanan  " + all.size());
        orderResults.revalidate();
        orderResults.repaint();
    }

    private void updateOrderSummary(List<Pesanan> all) {
        long pending = all.stream().filter(p -> p.status.equalsIgnoreCase("Menunggu konfirmasi")).count();
        long processing = all.stream().filter(p -> p.status.equalsIgnoreCase("Diproses")
                || p.status.equalsIgnoreCase("Siap Diambil")).count();
        long completed = all.stream().filter(p -> p.status.equalsIgnoreCase("Selesai")).count();
        pendingOrdersValue.setText(Long.toString(pending));
        totalOrdersValue.setText(Integer.toString(all.size()));
        processingOrdersValue.setText(Long.toString(processing));
        completedOrdersValue.setText(Long.toString(completed));
    }

    private void refreshCatalog() {
        List<MenuItem> all = dataStore.getCatalog();
        String query = catalogSearch.getText().trim().toLowerCase(Locale.ROOT);
        String selectedCategory = (String) catalogCategory.getSelectedItem();
        List<MenuItem> filtered = all.stream()
                .filter(item -> (selectedCategory == null || selectedCategory.equals("Semua Kategori")
                        || item.getCategory().equals(selectedCategory)))
                .filter(item -> (item.getName() + " " + item.getDescription()).toLowerCase(Locale.ROOT)
                        .contains(query))
                .collect(Collectors.toList());

        catalogResults.removeAll();
        if (filtered.isEmpty()) {
            JLabel empty = new JLabel(all.isEmpty() ? "Katalog belum berisi menu. Gunakan tombol Tambah menu."
                    : "Tidak ada menu yang cocok dengan pencarian atau kategori.");
            empty.setForeground(MUTED);
            empty.setBorder(new EmptyBorder(20, 4, 20, 4));
            catalogResults.add(empty);
        } else {
            for (MenuItem item : filtered) {
                catalogResults.add(createMenuRow(item));
                catalogResults.add(Box.createVerticalStrut(10));
            }
        }
        catalogTab.setText("Daftar menu  " + all.size());
        catalogResultCount.setText(filtered.size() + " dari " + all.size() + " menu");
        catalogResults.revalidate();
        catalogResults.repaint();
    }

    private JPanel createMenuRow(MenuItem item) {
        JPanel row = new RoundedPanel(new BorderLayout(14, 0), Color.WHITE, BORDER, 7);
        row.setBorder(new EmptyBorder(12, 12, 12, 12));
        if (!item.isActive()) {
            row.setBackground(new Color(0xFAF8F4));
        }

        JLabel image = new JLabel(loadMenuIcon(item), SwingConstants.CENTER);
        image.setPreferredSize(new Dimension(72, 62));
        image.setOpaque(true);
        image.setBackground(new Color(0xEDF2ED));
        image.setBorder(BorderFactory.createLineBorder(BORDER));
        row.add(image, BorderLayout.WEST);

        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel name = new JLabel(item.getName());
        name.setForeground(TEXT);
        name.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        copy.add(name);
        JLabel categoryPrice = new JLabel(item.getCategory() + " · " + formatPrice(item.getPrice()));
        categoryPrice.setForeground(MUTED);
        categoryPrice.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        copy.add(categoryPrice);
        JLabel description = new JLabel(item.getDescription());
        description.setForeground(MUTED);
        description.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        copy.add(description);
        row.add(copy, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        actions.setOpaque(false);
        JLabel availability = new JLabel(item.isActive() ? "Tayang" : "Disembunyikan");
        availability.setForeground(SAGE);
        availability.setBackground(new Color(0xEDF2ED));
        availability.setOpaque(true);
        availability.setBorder(new EmptyBorder(5, 8, 5, 8));
        actions.add(availability);
        JButton edit = createMenuAction("Edit");
        edit.addActionListener(event -> showMenuDialog(item));
        JButton toggle = createMenuAction(item.isActive() ? "Sembunyikan" : "Tampilkan");
        toggle.addActionListener(event -> toggleMenu(item));
        JButton remove = createMenuAction("Hapus");
        remove.addActionListener(event -> removeMenu(item));
        actions.add(edit);
        actions.add(toggle);
        actions.add(remove);
        row.add(actions, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        return row;
    }

    private ImageIcon loadMenuIcon(MenuItem item) {
        String fileName = item.getImageName();
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        for (Path candidate : imageCandidates(fileName)) {
            if (Files.isRegularFile(candidate)) {
                try {
                    java.awt.Image image = ImageIO.read(candidate.toFile());
                    if (image != null) {
                        return new ImageIcon(image.getScaledInstance(72, 62, java.awt.Image.SCALE_SMOOTH));
                    }
                } catch (IOException exception) {
                    JOptionPane.showMessageDialog(this,
                            "Gambar menu \"" + fileName + "\" tidak dapat dibaca: " + exception.getMessage(),
                            "Gambar menu tidak tersedia", JOptionPane.ERROR_MESSAGE);
                    return null;
                }
            }
        }
        return null;
    }

    private static List<Path> imageCandidates(String fileName) {
        Path workingDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        return List.of(
                workingDirectory.resolve("images").resolve(fileName),
                workingDirectory.resolve("..").resolve("images").resolve(fileName).normalize(),
                workingDirectory.resolve("..").resolve("..").resolve("images").resolve(fileName).normalize(),
                workingDirectory.resolve("..").resolve("..").resolve("..").resolve("images").resolve(fileName).normalize());
    }

    private static String formatPrice(int price) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"));
        formatter.setMaximumFractionDigits(0);
        return formatter.format(price);
    }

    private static JButton createMenuAction(String label) {
        JButton button = new JButton(label);
        button.setUI(new BasicButtonUI());
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setForeground(TEXT);
        button.setBackground(Color.WHITE);
        button.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(6, 9, 6, 9)));
        return button;
    }

    private void toggleMenu(MenuItem item) {
        item.setActive(!item.isActive());
        saveMenu(item);
    }

    private void removeMenu(MenuItem item) {
        int answer = JOptionPane.showConfirmDialog(this,
                "Hapus menu \"" + item.getName() + "\"?", "Konfirmasi hapus",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer == JOptionPane.YES_OPTION) {
            try {
                dataStore.removeMenu(item.getId());
                refreshCatalog();
            } catch (IOException exception) {
                showStorageError(exception);
            }
        }
    }

    private void showMenuDialog(MenuItem current) {
        JTextField name = new JTextField(current == null ? "" : current.getName(), 22);
        JTextArea description = new JTextArea(current == null ? "" : current.getDescription(), 3, 22);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        JTextField price = new JTextField(current == null ? "" : Integer.toString(current.getPrice()), 22);
        JComboBox<String> category = new JComboBox<>(new String[]{"Makanan", "Minuman"});
        JTextField imageName = new JTextField(current == null ? "" : current.getImageName(), 22);
        if (current != null) {
            category.setSelectedItem(current.getCategory());
        }

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(5, 5, 5, 5);
        addFormField(form, constraints, 0, "Nama menu", name);
        addFormField(form, constraints, 2, "Deskripsi", new JScrollPane(description));
        JPanel priceCategory = new JPanel(new GridLayout(1, 2, 12, 0));
        priceCategory.add(fieldGroup("Harga (Rp)", price));
        priceCategory.add(fieldGroup("Kategori", category));
        addFormField(form, constraints, 4, "", priceCategory);
        addFormField(form, constraints, 6, "Nama file gambar", imageName);
        JLabel hint = new JLabel("Letakkan file gambar di folder proyek agar bisa ditampilkan.");
        hint.setForeground(MUTED);
        hint.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        addFormField(form, constraints, 8, "", hint);

        int answer = JOptionPane.showConfirmDialog(this, form,
                current == null ? "Tambah menu" : "Edit menu",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) {
            return;
        }
        int parsedPrice;
        try {
            parsedPrice = Integer.parseInt(price.getText().trim());
        } catch (NumberFormatException exception) {
            JOptionPane.showMessageDialog(this, "Harga harus berupa angka bulat.",
                    "Harga tidak valid", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String cleanName = name.getText().trim();
        String cleanDescription = description.getText().trim();
        String cleanImageName = imageName.getText().trim();
        if (cleanName.isEmpty() || cleanName.length() > 60
                || cleanDescription.isEmpty() || cleanDescription.length() > 160
                || parsedPrice < 500 || parsedPrice % 500 != 0) {
            JOptionPane.showMessageDialog(this,
                    "Nama (maksimal 60 karakter) dan deskripsi (maksimal 160 karakter) wajib diisi. "
                            + "Harga minimal Rp500 dan kelipatan Rp500.",
                    "Data menu belum valid", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (cleanImageName.length() > 120 || cleanImageName.contains("/") || cleanImageName.contains("\\")) {
            JOptionPane.showMessageDialog(this,
                    "Masukkan nama file gambar saja, tanpa folder atau alamat web.",
                    "Nama gambar tidak valid", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String selectedCategory = (String) category.getSelectedItem();
        if (cleanImageName.isEmpty()) {
            cleanImageName = current != null && !current.getImageName().isBlank()
                    ? current.getImageName()
                    : selectedCategory.equals("Makanan") ? "PEMPEK.jpeg" : "ES TEH.jpeg";
        }
        MenuItem updated = current == null
                ? SellerDataStore.newMenu(cleanName, cleanDescription, parsedPrice,
                        selectedCategory, cleanImageName)
                : current;
        updated.setName(cleanName);
        updated.setDescription(cleanDescription);
        updated.setPrice(parsedPrice);
        updated.setCategory(selectedCategory);
        updated.setImageName(cleanImageName);
        if (current == null) {
            try {
                dataStore.addMenu(updated);
                refreshCatalog();
            } catch (IOException exception) {
                showStorageError(exception);
            }
        } else {
            saveMenu(updated);
        }
    }

    private static JPanel fieldGroup(String label, Component field) {
        JPanel group = new JPanel(new BorderLayout(0, 5));
        group.setOpaque(false);
        group.add(new JLabel(label), BorderLayout.NORTH);
        group.add(field, BorderLayout.CENTER);
        return group;
    }

    private static void addFormField(JPanel form, GridBagConstraints constraints, int row, String label,
            Component field) {
        constraints.gridy = row;
        if (!label.isEmpty()) {
            form.add(new JLabel(label), constraints);
            constraints.gridy = row + 1;
        }
        form.add(field, constraints);
    }

    private void saveMenu(MenuItem item) {
        try {
            dataStore.saveMenu(item);
            refreshCatalog();
        } catch (IOException exception) {
            showStorageError(exception);
        }
    }

    private void showStorageError(IOException exception) {
        JOptionPane.showMessageDialog(this,
                "Perubahan tidak dapat disimpan: " + exception.getMessage(),
                "Kesalahan penyimpanan", JOptionPane.ERROR_MESSAGE);
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 12));
        footer.setBackground(new Color(0xEAD8C1));
        JLabel copyright = new JLabel("© 2026 KantinKu. Hak Cipta Dilindungi.");
        copyright.setForeground(Color.WHITE);
        footer.add(copyright);
        return footer;
    }

    private static final class PlaceholderTextField extends JTextField {
        private final String prompt;

        private PlaceholderTextField(String prompt, int columns) {
            super(columns);
            this.prompt = prompt;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (getText().isEmpty()) {
                graphics.setColor(MUTED);
                graphics.setFont(getFont());
                graphics.drawString(prompt, getInsets().left,
                        getHeight() / 2 + getFontMetrics(getFont()).getAscent() / 2 - 2);
            }
        }
    }

    private static void stylePrimaryButton(JButton button) {
        button.setBackground(PRIMARY);
        button.setForeground(Color.WHITE);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PRIMARY),
                new EmptyBorder(10, 20, 10, 20)));
        button.getModel().addChangeListener(event -> button.setBackground(
                button.getModel().isRollover() ? PRIMARY_HOVER : PRIMARY));
    }

    private static final class RoundedPanel extends JPanel {
        private final Color borderColor;
        private final int radius;

        private RoundedPanel(java.awt.LayoutManager layout, Color background, Color borderColor, int radius) {
            super(layout);
            this.borderColor = borderColor;
            this.radius = radius;
            setBackground(background);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(getBackground());
            g.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            if (borderColor != null) {
                g.setColor(borderColor);
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            }
            g.dispose();
            super.paintComponent(graphics);
        }
    }
}
