package view;

import model.Menu;
import model.ItemKeranjang;
import service.Keranjang;
import service.PesananBersama;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KantinKuPembeliView extends JFrame {

    private final Color COLOR_PRIMARY_DARK = new Color(50, 40, 35);
    private final Color COLOR_BG_CREAM = new Color(245, 235, 224);
    private final Color COLOR_CARD_BG = Color.WHITE;
    private final Color COLOR_TERRACOTTA = new Color(194, 65, 12);
    private final Color COLOR_TEXT_MUTED = new Color(120, 110, 100);
    private final Color COLOR_BADGE_BG = new Color(253, 238, 227);
    private final Color COLOR_EMPTY_BOX = new Color(245, 236, 223);

    private JTabbedPane tabbedPane;
    private DefaultTableModel keranjangModel;
    private JLabel lblTotalHargaKeranjang;
    private JLabel lblTotalHargaPembayaran;
    private JLabel lblBadgeKeranjang;
    private JPanel panelRingkasanPesanan;
    private JPanel panelKeranjangContainer;
    private JPanel panelPesananSayaContainer;

    private JTextField txtNamaPembeli;
    private ButtonGroup bgMetodeBayar;
    private JRadioButton rbQRIS, rbTunai, rbTransfer;
    private JComboBox<String> cbBankVA;
    private JPanel panelPilihBankContainer;

    private Keranjang keranjang = new Keranjang();
    private int nomorPesananCount = 1;

    // data pesanan yang sedang dibayar (baru disimpan ke penjual setelah pembayaran dikonfirmasi)
    private String IDPesananTerakhir = "";
    private String namaPembeliTerakhir = "";
    private String metodePesananTerakhir = "";
    private int totalPesananTerakhir = 0;
    private String rincianPesananTerakhir = "";

    // sinkronisasi dengan penjual
    private javax.swing.Timer timerSinkron;
    private long penandaPesananTerlihat = Long.MIN_VALUE;
    private long penandaKatalogTerlihat = Long.MIN_VALUE;

    private JTextField txtCariMenu;
    private JPanel gridMenuContainer;
    private JLabel lblJumlahTersedia;
    private JButton btnFilterSemua, btnFilterMakanan, btnFilterMinuman, btnFilterFavorit;
    private String filterAktif = "Semua";
    private final Set<String> daftarFavorit = new HashSet<>();

    private final List<Menu> dataMasterMenu = new ArrayList<>();

    public KantinKuPembeliView() {
        muatKatalogBersama();

        setTitle("KantinKu - Pemesanan Kantin Kampus");
        setSize(1100, 800);
        JendelaUtil.daftarkan(this);
        setLocationRelativeTo(null);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 220, 210)),
                new EmptyBorder(12, 30, 12, 30)
        ));

        JLabel lblLogo = new JLabel("<html><font color='#C2410C'><b>Kantin</b></font><font color='#322823'><b>Ku</b></font></html>");
        lblLogo.setFont(new Font("SansSerif", Font.BOLD, 22));
        headerPanel.add(lblLogo, BorderLayout.WEST);

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("SansSerif", Font.PLAIN, 14));
        tabbedPane.setBackground(COLOR_BG_CREAM);

        tabbedPane.addTab("Beranda", createBerandaPanel());
        tabbedPane.addTab("Menu", createMenuPanel());
        tabbedPane.addTab("Keranjang", createKeranjangPanel());
        tabbedPane.addTab("Pembayaran", createPembayaranPanel());
        tabbedPane.addTab("Pesanan Saya", createPesananSayaPanel());
        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedIndex() == 4) updatePesananSayaTampilan();
        });

        lblBadgeKeranjang = new JLabel(" 0 ");
        lblBadgeKeranjang.setOpaque(true);
        lblBadgeKeranjang.setBackground(COLOR_TERRACOTTA);
        lblBadgeKeranjang.setForeground(Color.WHITE);
        lblBadgeKeranjang.setFont(new Font("SansSerif", Font.BOLD, 11));

        setLayout(new BorderLayout());
        add(headerPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);

        mulaiSinkronisasi();
    }

    /**
     * Memeriksa perubahan dari penjual setiap 1,5 detik: menu baru/disembunyikan
     * dan perubahan status pesanan (misalnya pesanan sudah dikonfirmasi penjual).
     */
    private void mulaiSinkronisasi() {
        penandaPesananTerlihat = PesananBersama.penandaPerubahan();
        timerSinkron = new javax.swing.Timer(1500, e -> {
            long katalog = penandaKatalogBersama();
            if (katalog != penandaKatalogTerlihat) {
                muatKatalogBersama();
                updateGridMenu();
            }
            long pesanan = PesananBersama.penandaPerubahan();
            if (pesanan != penandaPesananTerlihat) {
                penandaPesananTerlihat = pesanan;
                updatePesananSayaTampilan();
            }
        });
        timerSinkron.start();
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                timerSinkron.stop();
            }
        });
    }

    private void isiMenuBawaan() {
        dataMasterMenu.clear();
        dataMasterMenu.add(new Menu("Nasi Goreng", 15000, "NASI GORENG.jpeg", "Makanan", "Nasi goreng gurih dengan telur dan ayam."));
        dataMasterMenu.add(new Menu("Mie Ayam", 12000, "MIE AYAM.jpeg", "Makanan", "Mie lembut dengan ayam gurih dan kuah."));
        dataMasterMenu.add(new Menu("Pempek", 15000, "PEMPEK.jpeg", "Makanan", "Pempek ikan dengan kuah cuko khas."));
        dataMasterMenu.add(new Menu("Siomay", 13000, "SIOMAY.jpeg", "Makanan", "Siomay lengkap dengan saus kacang."));
        dataMasterMenu.add(new Menu("Ayam Bakar", 18000, "AYAM BAKAR.jpeg", "Makanan", "Ayam bakar bumbu meresap manis gurih."));
        dataMasterMenu.add(new Menu("Batagor", 10000, "BATAGOR.jpeg", "Makanan", "Bakso tahu goreng renyah saus kacang."));
        dataMasterMenu.add(new Menu("Soto", 14000, "SOTO.jpeg", "Makanan", "Soto kuah hangat dengan koya renyah."));
        dataMasterMenu.add(new Menu("Risol Mayo", 8000, "RISOL MAYO.jpeg", "Makanan", "Risol isian mayones dan daging gurih."));
        dataMasterMenu.add(new Menu("Kentang Goreng", 10000, "KENTANG GORENG.jpeg", "Makanan", "Kentang goreng stik renyah gurih."));
        dataMasterMenu.add(new Menu("Matcha", 8000, "MATCHA.jpeg", "Minuman", "Minuman matcha susu manis dingin."));
        dataMasterMenu.add(new Menu("Cappucino", 8000, "CAPPUCINO.jpeg", "Minuman", "Kopi cappucino racikan lembut bersusu."));
        dataMasterMenu.add(new Menu("Coklat", 8000, "COKLAT.jpeg", "Minuman", "Coklat pekat manis dingin segar."));
        dataMasterMenu.add(new Menu("Kopi Susu", 8000, "KOPI SUSU.jpeg", "Minuman", "Kopi susu gula aren khas KantinKu."));
        dataMasterMenu.add(new Menu("Thai Tea", 7000, "THAI TEA.jpeg", "Minuman", "Teh Thailand autentik manis segar."));
        dataMasterMenu.add(new Menu("Es Jeruk", 5000, "ES JERUK.jpeg", "Minuman", "Perasan jeruk peras alami manis dingin."));
        dataMasterMenu.add(new Menu("Es Teh", 4000, "ES TEH.jpeg", "Minuman", "Es teh manis penyegar dahaga."));
    }

    private static Path berkasKatalogBersama() {
        return Path.of(System.getProperty("user.home"), ".kantinku-desktop", "menus.tsv");
    }

    private long penandaKatalogBersama() {
        try {
            Path f = berkasKatalogBersama();
            return Files.exists(f) ? Files.getLastModifiedTime(f).toMillis() * 31 + Files.size(f) : -1;
        } catch (IOException e) {
            return -1;
        }
    }

    /**
     * Menu pembeli = katalog yang dikelola penjual (hanya menu yang tayang).
     * Bila penjual belum pernah membuka katalog, dipakai menu bawaan.
     */
    private void muatKatalogBersama() {
        penandaKatalogTerlihat = penandaKatalogBersama();
        Path sharedCatalog = berkasKatalogBersama();
        if (!Files.exists(sharedCatalog)) {
            isiMenuBawaan();
            return;
        }
        try {
            List<Menu> sharedMenus = new ArrayList<>();
            for (String line : Files.readAllLines(sharedCatalog, StandardCharsets.UTF_8)) {
                if (line.isBlank()) continue;
                String[] fields = line.split("\\t", -1);
                if (fields.length < 7) continue;
                String nama = decodeShared(fields[1]);
                String deskripsi = decodeShared(fields[2]);
                int harga = Integer.parseInt(fields[3]);
                String kategori = decodeShared(fields[4]);
                String gambar = decodeShared(fields[5]);
                boolean aktif = Boolean.parseBoolean(fields[6]);
                if (aktif) sharedMenus.add(new Menu(nama, harga, gambar, kategori, deskripsi));
            }
            dataMasterMenu.clear();
            dataMasterMenu.addAll(sharedMenus);
        } catch (Exception ex) {
            System.err.println("Katalog bersama tidak dapat dibaca: " + ex.getMessage());
            if (dataMasterMenu.isEmpty()) isiMenuBawaan();
        }
    }

    private String decodeShared(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private JPanel createBerandaPanel() {
        JPanel outerPanel = new JPanel(new BorderLayout());
        outerPanel.setBackground(COLOR_BG_CREAM);

        JPanel contentContainer = new JPanel();
        contentContainer.setLayout(new BoxLayout(contentContainer, BoxLayout.Y_AXIS));
        contentContainer.setBackground(COLOR_BG_CREAM);

        JPanel heroPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                ImageIcon bgIcon = loadMenuImage("NASI GORENG.jpeg");
                if (bgIcon != null) {
                    g.drawImage(bgIcon.getImage(), 0, 0, getWidth(), getHeight(), this);
                    g.setColor(new Color(0, 0, 0, 140));
                    g.fillRect(0, 0, getWidth(), getHeight());
                } else {
                    g.setColor(COLOR_PRIMARY_DARK);
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        heroPanel.setPreferredSize(new Dimension(1000, 400));
        heroPanel.setBorder(new EmptyBorder(50, 50, 50, 50));

        JPanel heroTextPanel = new JPanel();
        heroTextPanel.setLayout(new BoxLayout(heroTextPanel, BoxLayout.Y_AXIS));
        heroTextPanel.setOpaque(false);

        JLabel lblHeroTag = new JLabel("KANTIN KAMPUS - PRAKTIS & CEPAT");
        lblHeroTag.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblHeroTag.setForeground(new Color(220, 220, 220));

        JLabel lblHeroTitle = new JLabel("<html><font size='7'><b>Menu favoritmu,<br>siap dipesan.</b></font></html>");
        lblHeroTitle.setForeground(Color.WHITE);

        JLabel lblHeroDesc = new JLabel("<html><font size='4'>Nikmati kemudahan memesan makanan dan minuman favoritmu di KantinKu.<br>Pilih menu, selesaikan pembayaran, dan tinggal ambil!</font></html>");
        lblHeroDesc.setForeground(new Color(230, 230, 230));

        JPanel btnHeroGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        btnHeroGroup.setOpaque(false);

        JButton btnJelajahi = new JButton("Jelajahi menu");
        styleButton(btnJelajahi, COLOR_TERRACOTTA, Color.WHITE);
        btnJelajahi.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnJelajahi.addActionListener(e -> tabbedPane.setSelectedIndex(1));

        JButton btnLihatPesanan = new JButton("Lihat pesanan saya");
        styleButton(btnLihatPesanan, new Color(255, 255, 255, 220), COLOR_PRIMARY_DARK);
        btnLihatPesanan.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnLihatPesanan.addActionListener(e -> tabbedPane.setSelectedIndex(4));

        btnHeroGroup.add(btnJelajahi);
        btnHeroGroup.add(btnLihatPesanan);

        heroTextPanel.add(lblHeroTag);
        heroTextPanel.add(Box.createVerticalStrut(12));
        heroTextPanel.add(lblHeroTitle);
        heroTextPanel.add(Box.createVerticalStrut(12));
        heroTextPanel.add(lblHeroDesc);
        heroTextPanel.add(Box.createVerticalStrut(20));
        heroTextPanel.add(btnHeroGroup);

        heroPanel.add(heroTextPanel, BorderLayout.WEST);

        JPanel promoCard = new JPanel(new BorderLayout());
        promoCard.setBackground(new Color(245, 238, 227));
        promoCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 220, 210), 1),
                new EmptyBorder(25, 35, 25, 35)
        ));

        JPanel promoLeft = new JPanel();
        promoLeft.setLayout(new BoxLayout(promoLeft, BoxLayout.Y_AXIS));
        promoLeft.setOpaque(false);

        JLabel lblPromoTag = new JLabel("PEMESANAN SELALU TERPANTAU");
        lblPromoTag.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblPromoTag.setForeground(COLOR_TERRACOTTA);

        JLabel lblPromoTitle = new JLabel("Pesan dulu, cek progresnya kapan saja.");
        lblPromoTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblPromoTitle.setForeground(COLOR_PRIMARY_DARK);

        JLabel lblPromoSub = new JLabel("<html>Lihat progres pesananmu menggunakan nomor pesanan. Kamu bisa<br>memantau tanpa membuat akun.</html>");
        lblPromoSub.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblPromoSub.setForeground(COLOR_TEXT_MUTED);

        JButton btnBukaPesanan = new JButton("Buka pesanan");
        styleButton(btnBukaPesanan, COLOR_TERRACOTTA, Color.WHITE);
        btnBukaPesanan.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnBukaPesanan.addActionListener(e -> tabbedPane.setSelectedIndex(4));

        promoLeft.add(lblPromoTag);
        promoLeft.add(Box.createVerticalStrut(6));
        promoLeft.add(lblPromoTitle);
        promoLeft.add(Box.createVerticalStrut(6));
        promoLeft.add(lblPromoSub);
        promoLeft.add(Box.createVerticalStrut(15));
        promoLeft.add(btnBukaPesanan);

        JLabel lblPromoImg = new JLabel();
        ImageIcon imgPromo = loadMenuImage("NASI GORENG.jpeg");
        if (imgPromo != null) {
            Image scaled = imgPromo.getImage().getScaledInstance(180, 115, Image.SCALE_SMOOTH);
            lblPromoImg.setIcon(new ImageIcon(scaled));
        }

        promoCard.add(promoLeft, BorderLayout.WEST);
        promoCard.add(lblPromoImg, BorderLayout.EAST);

        JPanel featurePanel = new JPanel(new GridLayout(1, 3, 20, 0));
        featurePanel.setOpaque(false);

        featurePanel.add(createFeatureCard("01", "Cepat & Praktis", "Pilih menu dan siapkan pesanan tanpa perlu mengantre panjang."));
        featurePanel.add(createFeatureCard("02", "Pilihan Menu Lengkap", "Temukan hidangan dan minuman favorit dari pilihan kantin."));
        featurePanel.add(createFeatureCard("03", "Pesanan Terpantau", "Cek perkembangan pesanan dengan nomor pesananmu."));

        contentContainer.add(heroPanel);
        contentContainer.add(Box.createVerticalStrut(30));

        JPanel padPromo = new JPanel(new BorderLayout());
        padPromo.setOpaque(false);
        padPromo.setBorder(new EmptyBorder(0, 40, 0, 40));
        padPromo.add(promoCard, BorderLayout.CENTER);
        contentContainer.add(padPromo);

        contentContainer.add(Box.createVerticalStrut(30));

        JPanel padFeature = new JPanel(new BorderLayout());
        padFeature.setOpaque(false);
        padFeature.setBorder(new EmptyBorder(0, 40, 30, 40));
        padFeature.add(featurePanel, BorderLayout.CENTER);
        contentContainer.add(padFeature);

        JPanel scrollHolder = new JPanel(new BorderLayout());
        scrollHolder.setBackground(COLOR_BG_CREAM);
        scrollHolder.add(contentContainer, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(scrollHolder);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.getVerticalScrollBar().setUnitIncrement(22);
        scrollPane.setBorder(null);

        outerPanel.add(scrollPane, BorderLayout.CENTER);
        return outerPanel;
    }

    private JPanel createFeatureCard(String num, String title, String desc) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(COLOR_CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(235, 225, 215), 1),
                new EmptyBorder(20, 18, 20, 18)
        ));

        JLabel lblNum = new JLabel(num);
        lblNum.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblNum.setForeground(COLOR_TERRACOTTA);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblTitle.setForeground(COLOR_PRIMARY_DARK);

        JLabel lblDesc = new JLabel("<html>" + desc + "</html>");
        lblDesc.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblDesc.setForeground(COLOR_TEXT_MUTED);

        card.add(lblNum);
        card.add(Box.createVerticalStrut(6));
        card.add(lblTitle);
        card.add(Box.createVerticalStrut(6));
        card.add(lblDesc);

        return card;
    }

    private JPanel createMenuPanel() {
        JPanel outerPanel = new JPanel(new BorderLayout());
        outerPanel.setBackground(COLOR_BG_CREAM);

        JPanel topHeaderSection = new JPanel(new BorderLayout());
        topHeaderSection.setOpaque(false);
        topHeaderSection.setBorder(new EmptyBorder(25, 40, 15, 40));

        JPanel titleGroup = new JPanel();
        titleGroup.setLayout(new BoxLayout(titleGroup, BoxLayout.Y_AXIS));
        titleGroup.setOpaque(false);

        JLabel lblTag = new JLabel("MENU HARI INI");
        lblTag.setFont(new Font("SansSerif", Font.BOLD, 10));
        lblTag.setForeground(COLOR_TEXT_MUTED);

        JLabel lblMainTitle = new JLabel("Temukan yang kamu suka");
        lblMainTitle.setFont(new Font("SansSerif", Font.BOLD, 22));
        lblMainTitle.setForeground(COLOR_PRIMARY_DARK);

        JLabel lblSub = new JLabel("Dimasak dan disajikan oleh KantinKu.");
        lblSub.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblSub.setForeground(COLOR_TEXT_MUTED);

        titleGroup.add(lblTag);
        titleGroup.add(Box.createVerticalStrut(4));
        titleGroup.add(lblMainTitle);
        titleGroup.add(Box.createVerticalStrut(4));
        titleGroup.add(lblSub);

        JPanel searchGroup = new JPanel();
        searchGroup.setLayout(new BoxLayout(searchGroup, BoxLayout.Y_AXIS));
        searchGroup.setOpaque(false);

        JLabel lblCariTitle = new JLabel("Cari menu");
        lblCariTitle.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblCariTitle.setForeground(COLOR_TEXT_MUTED);
        lblCariTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtCariMenu = new JTextField();
        txtCariMenu.setFont(new Font("SansSerif", Font.PLAIN, 13));
        txtCariMenu.setPreferredSize(new Dimension(240, 36));
        txtCariMenu.setMaximumSize(new Dimension(240, 36));
        txtCariMenu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 210, 200), 1, true),
                new EmptyBorder(5, 12, 5, 12)
        ));

        txtCariMenu.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updateGridMenu(); }
            @Override public void removeUpdate(DocumentEvent e) { updateGridMenu(); }
            @Override public void changedUpdate(DocumentEvent e) { updateGridMenu(); }
        });

        searchGroup.add(lblCariTitle);
        searchGroup.add(Box.createVerticalStrut(4));
        searchGroup.add(txtCariMenu);

        topHeaderSection.add(titleGroup, BorderLayout.WEST);
        topHeaderSection.add(searchGroup, BorderLayout.EAST);

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterRow.setOpaque(false);
        filterRow.setBorder(new EmptyBorder(0, 40, 10, 40));

        btnFilterSemua = createFilterButton("Semua", true);
        btnFilterMakanan = createFilterButton("Makanan", false);
        btnFilterMinuman = createFilterButton("Minuman", false);
        btnFilterFavorit = createFilterButton("Favorit 0", false);

        btnFilterSemua.addActionListener(e -> setFilterAktif("Semua"));
        btnFilterMakanan.addActionListener(e -> setFilterAktif("Makanan"));
        btnFilterMinuman.addActionListener(e -> setFilterAktif("Minuman"));
        btnFilterFavorit.addActionListener(e -> setFilterAktif("Favorit"));

        filterRow.add(btnFilterSemua);
        filterRow.add(btnFilterMakanan);
        filterRow.add(btnFilterMinuman);
        filterRow.add(btnFilterFavorit);

        lblJumlahTersedia = new JLabel(dataMasterMenu.size() + " menu tersedia");
        lblJumlahTersedia.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblJumlahTersedia.setForeground(COLOR_TEXT_MUTED);
        lblJumlahTersedia.setBorder(new EmptyBorder(10, 40, 15, 40));

        gridMenuContainer = new JPanel();
        gridMenuContainer.setBackground(COLOR_BG_CREAM);

        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);
        topContainer.add(topHeaderSection);
        topContainer.add(filterRow);
        topContainer.add(lblJumlahTersedia);

        JPanel mainContent = new JPanel(new BorderLayout());
        mainContent.setBackground(COLOR_BG_CREAM);
        mainContent.add(topContainer, BorderLayout.NORTH);
        mainContent.add(gridMenuContainer, BorderLayout.CENTER);

        updateGridMenu();

        JScrollPane scrollPane = new JScrollPane(mainContent);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.getVerticalScrollBar().setUnitIncrement(22);
        scrollPane.setBorder(null);

        outerPanel.add(scrollPane, BorderLayout.CENTER);
        return outerPanel;
    }

    private JButton createFilterButton(String text, boolean active) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 18, 8, 18));
        updateFilterButtonStyle(btn, active);
        return btn;
    }

    private void updateFilterButtonStyle(JButton btn, boolean active) {
        if (active) {
            btn.setBackground(COLOR_TERRACOTTA);
            btn.setForeground(Color.WHITE);
        } else {
            btn.setBackground(Color.WHITE);
            btn.setForeground(COLOR_PRIMARY_DARK);
        }
    }

    private void setFilterAktif(String filter) {
        this.filterAktif = filter;
        updateFilterButtonStyle(btnFilterSemua, filter.equals("Semua"));
        updateFilterButtonStyle(btnFilterMakanan, filter.equals("Makanan"));
        updateFilterButtonStyle(btnFilterMinuman, filter.equals("Minuman"));
        updateFilterButtonStyle(btnFilterFavorit, filter.equals("Favorit"));
        updateGridMenu();
    }

    private void updateGridMenu() {
        if (gridMenuContainer == null) return;
        gridMenuContainer.removeAll();

        String kataKunci = txtCariMenu != null ? txtCariMenu.getText().toLowerCase().trim() : "";

        List<Menu> listFiltered = new ArrayList<>();
        for (Menu m : dataMasterMenu) {
            boolean matchesSearch = m.getNama().toLowerCase().contains(kataKunci);
            boolean matchesCategory = filterAktif.equals("Semua") ||
                    (filterAktif.equals("Makanan") && m.getKategori().equalsIgnoreCase("Makanan")) ||
                    (filterAktif.equals("Minuman") && m.getKategori().equalsIgnoreCase("Minuman")) ||
                    (filterAktif.equals("Favorit") && daftarFavorit.contains(m.getNama()));

            if (matchesSearch && matchesCategory) {
                listFiltered.add(m);
            }
        }

        lblJumlahTersedia.setText(listFiltered.size() + " menu tersedia");
        btnFilterFavorit.setText("Favorit " + daftarFavorit.size());

        int columns = 4;
        int rows = (int) Math.ceil((double) listFiltered.size() / columns);
        int cardHeight = 310;
        int gap = 16;
        int totalHeight = Math.max(400, (rows * cardHeight) + ((rows + 1) * gap));

        JPanel grid = new JPanel(new GridLayout(0, columns, gap, gap));
        grid.setBackground(COLOR_BG_CREAM);
        grid.setBorder(new EmptyBorder(0, 40, 30, 40));
        grid.setPreferredSize(new Dimension(980, totalHeight));

        for (Menu menu : listFiltered) {
            JPanel card = new JPanel(new BorderLayout());
            card.setBackground(COLOR_CARD_BG);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(230, 220, 210), 1),
                    new EmptyBorder(0, 0, 12, 0)
            ));

            JLayeredPane imageContainer = new JLayeredPane();
            imageContainer.setPreferredSize(new Dimension(200, 130));

            JLabel lblImage = new JLabel();
            lblImage.setBounds(0, 0, 230, 130);
            lblImage.setHorizontalAlignment(SwingConstants.CENTER);
            ImageIcon imageIcon = loadMenuImage(menu.getNamaGambar());

            if (imageIcon != null) {
                Image imgScaled = imageIcon.getImage().getScaledInstance(230, 130, Image.SCALE_SMOOTH);
                lblImage.setIcon(new ImageIcon(imgScaled));
            } else {
                lblImage.setText("<html><center>[ Gambar<br>" + menu.getNama() + " ]</center></html>");
                lblImage.setForeground(Color.GRAY);
            }

            JButton btnFav = new JButton(daftarFavorit.contains(menu.getNama()) ? "★" : "☆");
            btnFav.setBounds(180, 8, 32, 32);
            btnFav.setFont(new Font("SansSerif", Font.BOLD, 14));
            btnFav.setForeground(daftarFavorit.contains(menu.getNama()) ? new Color(234, 179, 8) : Color.GRAY);
            btnFav.setBackground(Color.WHITE);
            btnFav.setFocusPainted(false);
            btnFav.setBorder(BorderFactory.createLineBorder(new Color(230, 220, 210), 1));
            btnFav.setCursor(new Cursor(Cursor.HAND_CURSOR));

            btnFav.addActionListener(e -> {
                if (daftarFavorit.contains(menu.getNama())) {
                    daftarFavorit.remove(menu.getNama());
                } else {
                    daftarFavorit.add(menu.getNama());
                }
                updateGridMenu();
            });

            imageContainer.add(lblImage, JLayeredPane.DEFAULT_LAYER);
            imageContainer.add(btnFav, JLayeredPane.PALETTE_LAYER);

            JPanel infoPanel = new JPanel();
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
            infoPanel.setBackground(COLOR_CARD_BG);
            infoPanel.setBorder(new EmptyBorder(10, 12, 0, 12));

            JLabel lblBadgeKategori = new JLabel(" " + menu.getKategori() + " ");
            lblBadgeKategori.setOpaque(true);
            lblBadgeKategori.setBackground(COLOR_BADGE_BG);
            lblBadgeKategori.setForeground(COLOR_TERRACOTTA);
            lblBadgeKategori.setFont(new Font("SansSerif", Font.BOLD, 10));
            lblBadgeKategori.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel lblNama = new JLabel(menu.getNama());
            lblNama.setFont(new Font("SansSerif", Font.BOLD, 14));
            lblNama.setForeground(COLOR_PRIMARY_DARK);
            lblNama.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel lblDesc = new JLabel("<html>" + menu.getDeskripsi() + "</html>");
            lblDesc.setFont(new Font("SansSerif", Font.PLAIN, 11));
            lblDesc.setForeground(COLOR_TEXT_MUTED);
            lblDesc.setPreferredSize(new Dimension(190, 32));
            lblDesc.setAlignmentX(Component.LEFT_ALIGNMENT);

            infoPanel.add(lblBadgeKategori);
            infoPanel.add(Box.createVerticalStrut(6));
            infoPanel.add(lblNama);
            infoPanel.add(Box.createVerticalStrut(4));
            infoPanel.add(lblDesc);

            JPanel priceRow = new JPanel(new BorderLayout());
            priceRow.setBackground(COLOR_CARD_BG);
            priceRow.setBorder(new EmptyBorder(8, 12, 0, 12));

            JLabel lblHarga = new JLabel("Rp " + String.format("%,d", menu.getHarga()));
            lblHarga.setFont(new Font("SansSerif", Font.BOLD, 13));
            lblHarga.setForeground(COLOR_PRIMARY_DARK);

            JButton btnPlus = new JButton("+");
            btnPlus.setBackground(COLOR_TERRACOTTA);
            btnPlus.setForeground(Color.WHITE);
            btnPlus.setFont(new Font("SansSerif", Font.BOLD, 18));
            btnPlus.setMargin(new Insets(0, 0, 0, 0));
            btnPlus.setFocusPainted(false);
            btnPlus.setBorder(null);
            btnPlus.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnPlus.setPreferredSize(new Dimension(34, 34));

            btnPlus.addActionListener(e -> showModalPesan(menu, imageIcon));

            priceRow.add(lblHarga, BorderLayout.WEST);
            priceRow.add(btnPlus, BorderLayout.EAST);

            card.add(imageContainer, BorderLayout.NORTH);
            card.add(infoPanel, BorderLayout.CENTER);
            card.add(priceRow, BorderLayout.SOUTH);

            grid.add(card);
        }

        gridMenuContainer.add(grid);
        gridMenuContainer.revalidate();
        gridMenuContainer.repaint();
    }

    private void showModalPesan(Menu menu, ImageIcon iconGambar) {
        JDialog dialog = new JDialog(this, "Tambah ke Keranjang", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(480, 360);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(new EmptyBorder(20, 25, 20, 25));

        JPanel headerProduct = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        headerProduct.setBackground(Color.WHITE);

        JLabel lblImage = new JLabel();
        if (iconGambar != null) {
            Image imgScaled = iconGambar.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
            lblImage.setIcon(new ImageIcon(imgScaled));
        } else {
            lblImage.setPreferredSize(new Dimension(80, 80));
            lblImage.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        }

        JPanel details = new JPanel();
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.setBackground(Color.WHITE);

        JLabel lblSubtitle = new JLabel("PESAN DARI KANTINKU");
        lblSubtitle.setFont(new Font("SansSerif", Font.BOLD, 10));
        lblSubtitle.setForeground(new Color(101, 117, 100));

        JLabel lblTitle = new JLabel(menu.getNama());
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblTitle.setForeground(COLOR_PRIMARY_DARK);

        JLabel lblPriceDesc = new JLabel("Rp " + String.format("%,d", menu.getHarga()) + " per porsi");
        lblPriceDesc.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblPriceDesc.setForeground(Color.GRAY);

        details.add(lblSubtitle);
        details.add(Box.createVerticalStrut(3));
        details.add(lblTitle);
        details.add(Box.createVerticalStrut(3));
        details.add(lblPriceDesc);

        headerProduct.add(lblImage);
        headerProduct.add(details);

        JPanel porsiPanel = new JPanel(new BorderLayout());
        porsiPanel.setBackground(Color.WHITE);
        porsiPanel.setBorder(new EmptyBorder(15, 0, 15, 0));

        JLabel lblJumlahText = new JLabel("Jumlah porsi");
        lblJumlahText.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JPanel counterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        counterPanel.setBackground(Color.WHITE);

        JButton btnMinus = new JButton("-");
        JButton btnPlus = new JButton("+");
        JLabel lblQty = new JLabel("1", SwingConstants.CENTER);
        lblQty.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblQty.setPreferredSize(new Dimension(30, 25));

        styleCounterButton(btnMinus);
        styleCounterButton(btnPlus);

        int[] porsiCount = {1};

        counterPanel.add(btnMinus);
        counterPanel.add(lblQty);
        counterPanel.add(btnPlus);

        porsiPanel.add(lblJumlahText, BorderLayout.WEST);
        porsiPanel.add(counterPanel, BorderLayout.EAST);

        JPanel subtotalPanel = new JPanel(new BorderLayout());
        subtotalPanel.setBackground(Color.WHITE);
        subtotalPanel.setBorder(new EmptyBorder(10, 0, 15, 0));

        JLabel lblSubtotalText = new JLabel("Subtotal");
        lblSubtotalText.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JLabel lblSubtotalVal = new JLabel("Rp " + String.format("%,d", menu.getHarga()));
        lblSubtotalVal.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblSubtotalVal.setForeground(COLOR_PRIMARY_DARK);

        subtotalPanel.add(lblSubtotalText, BorderLayout.WEST);
        subtotalPanel.add(lblSubtotalVal, BorderLayout.EAST);

        btnMinus.addActionListener(e -> {
            if (porsiCount[0] > 1) {
                porsiCount[0]--;
                lblQty.setText(String.valueOf(porsiCount[0]));
                lblSubtotalVal.setText("Rp " + String.format("%,d", menu.getHarga() * porsiCount[0]));
            }
        });

        btnPlus.addActionListener(e -> {
            porsiCount[0]++;
            lblQty.setText(String.valueOf(porsiCount[0]));
            lblSubtotalVal.setText("Rp " + String.format("%,d", menu.getHarga() * porsiCount[0]));
        });

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionPanel.setBackground(Color.WHITE);

        JButton btnBatal = new JButton("Batal");
        styleButton(btnBatal, new Color(140, 130, 125), Color.WHITE);

        JButton btnSubmit = new JButton("Tambah ke keranjang");
        styleButton(btnSubmit, COLOR_TERRACOTTA, Color.WHITE);

        btnBatal.addActionListener(e -> dialog.dispose());

        btnSubmit.addActionListener(e -> {
            keranjang.tambahItem(menu, porsiCount[0]);
            lblBadgeKeranjang.setText(" " + keranjang.getTotalQty() + " ");
            updateKeranjangTampilan();
            updateRingkasanTampilan();
            dialog.dispose();
            JOptionPane.showMessageDialog(this, porsiCount[0] + " x " + menu.getNama() + " berhasil dimasukkan ke keranjang!",
                    "Berhasil Disimpan", JOptionPane.INFORMATION_MESSAGE);
        });

        actionPanel.add(btnBatal);
        actionPanel.add(btnSubmit);

        contentPanel.add(headerProduct);
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(new JSeparator(JSeparator.HORIZONTAL));
        contentPanel.add(porsiPanel);
        contentPanel.add(new JSeparator(JSeparator.HORIZONTAL));
        contentPanel.add(subtotalPanel);
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(actionPanel);

        dialog.add(contentPanel);
        dialog.setVisible(true);
    }

    private JPanel createKeranjangPanel() {
        JPanel outerPanel = new JPanel(new BorderLayout());
        outerPanel.setBackground(COLOR_BG_CREAM);

        panelKeranjangContainer = new JPanel();
        panelKeranjangContainer.setLayout(new BoxLayout(panelKeranjangContainer, BoxLayout.Y_AXIS));
        panelKeranjangContainer.setBackground(COLOR_BG_CREAM);

        String[] columns = {"NO", "NAMA MENU", "JUMLAH", "HARGA SATUAN", "SUBTOTAL", "AKSI"};
        keranjangModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5;
            }
        };

        updateKeranjangTampilan();

        JScrollPane scrollPane = new JScrollPane(panelKeranjangContainer);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.getVerticalScrollBar().setUnitIncrement(22);
        scrollPane.setBorder(null);

        outerPanel.add(scrollPane, BorderLayout.CENTER);
        return outerPanel;
    }

    private void syncModelKeranjangWithService() {
        keranjangModel.setRowCount(0);
        int no = 1;
        for (ItemKeranjang item : keranjang.getListItems()) {
            keranjangModel.addRow(new Object[]{
                no++,
                item.getMenu().getNama(),
                item.getJumlah(),
                "Rp " + String.format("%,d", item.getMenu().getHarga()),
                "Rp " + String.format("%,d", item.getSubtotal()),
                "Hapus"
            });
        }
    }

    private void updateKeranjangTampilan() {
        if (panelKeranjangContainer == null) return;
        panelKeranjangContainer.removeAll();
        panelKeranjangContainer.setBorder(new EmptyBorder(30, 60, 40, 60));

        syncModelKeranjangWithService();

        JPanel headerTextGroup = new JPanel();
        headerTextGroup.setLayout(new BoxLayout(headerTextGroup, BoxLayout.Y_AXIS));
        headerTextGroup.setOpaque(false);

        JLabel lblTag = new JLabel("PESANANMU");
        lblTag.setFont(new Font("SansSerif", Font.BOLD, 10));
        lblTag.setForeground(COLOR_TERRACOTTA);
        lblTag.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblTitle = new JLabel("Keranjang belanja");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 24));
        lblTitle.setForeground(COLOR_PRIMARY_DARK);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSub = new JLabel("Periksa kembali pilihan menu sebelum melanjutkan pembayaran.");
        lblSub.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblSub.setForeground(COLOR_TEXT_MUTED);
        lblSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        headerTextGroup.add(lblTag);
        headerTextGroup.add(Box.createVerticalStrut(4));
        headerTextGroup.add(lblTitle);
        headerTextGroup.add(Box.createVerticalStrut(4));
        headerTextGroup.add(lblSub);

        panelKeranjangContainer.add(headerTextGroup);
        panelKeranjangContainer.add(Box.createVerticalStrut(20));

        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(Color.WHITE);
        tableCard.setBorder(BorderFactory.createLineBorder(new Color(230, 220, 210), 1));
        tableCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));

        if (keranjang.getListItems().isEmpty()) {
            JPanel headerTable = new JPanel(new GridLayout(1, 6));
            headerTable.setBackground(COLOR_TERRACOTTA);
            headerTable.setPreferredSize(new Dimension(0, 40));

            String[] titles = {"NO", "NAMA MENU", "JUMLAH", "HARGA SATUAN", "SUBTOTAL", "AKSI"};
            for (String t : titles) {
                JLabel l = new JLabel(t, SwingConstants.CENTER);
                l.setFont(new Font("SansSerif", Font.BOLD, 11));
                l.setForeground(Color.WHITE);
                headerTable.add(l);
            }

            JPanel bodyEmpty = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 20));
            bodyEmpty.setBackground(Color.WHITE);
            JLabel lblEmpty = new JLabel("Keranjang masih kosong. Pilih menu untuk mulai memesan.");
            lblEmpty.setFont(new Font("SansSerif", Font.BOLD, 13));
            lblEmpty.setForeground(COLOR_TEXT_MUTED);
            bodyEmpty.add(lblEmpty);

            tableCard.add(headerTable, BorderLayout.NORTH);
            tableCard.add(bodyEmpty, BorderLayout.CENTER);
        } else {
            JTable tableKeranjang = createStyledTable(keranjangModel);
            tableKeranjang.getTableHeader().setBackground(COLOR_TERRACOTTA);
            tableKeranjang.getTableHeader().setForeground(Color.WHITE);

            tableKeranjang.getColumnModel().getColumn(5).setCellRenderer(new ButtonRenderer());
            tableKeranjang.getColumnModel().getColumn(5).setCellEditor(new ButtonEditor(new JCheckBox()));

            JScrollPane tableScroll = new JScrollPane(tableKeranjang);
            tableScroll.setBorder(null);
            tableScroll.getViewport().setBackground(Color.WHITE);
            tableScroll.setPreferredSize(new Dimension(0, Math.min(250, (keranjangModel.getRowCount() + 1) * 35)));

            tableCard.add(tableScroll, BorderLayout.CENTER);
        }

        panelKeranjangContainer.add(tableCard);
        panelKeranjangContainer.add(Box.createVerticalStrut(20));

        JPanel totalCard = new JPanel(new BorderLayout());
        totalCard.setBackground(Color.WHITE);
        totalCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 220, 210), 1),
                new EmptyBorder(16, 25, 16, 25)
        ));
        totalCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 75));
        totalCard.setPreferredSize(new Dimension(0, 75));

        JPanel totalLeft = new JPanel();
        totalLeft.setLayout(new BoxLayout(totalLeft, BoxLayout.Y_AXIS));
        totalLeft.setOpaque(false);

        JLabel lblTotalTag = new JLabel("Total pesanan");
        lblTotalTag.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblTotalTag.setForeground(COLOR_TEXT_MUTED);

        lblTotalHargaKeranjang = new JLabel("Rp " + String.format("%,d", keranjang.getTotalHarga()));
        lblTotalHargaKeranjang.setFont(new Font("SansSerif", Font.BOLD, 20));
        lblTotalHargaKeranjang.setForeground(COLOR_PRIMARY_DARK);

        totalLeft.add(lblTotalTag);
        totalLeft.add(Box.createVerticalStrut(2));
        totalLeft.add(lblTotalHargaKeranjang);

        JButton btnLanjutBayar = new JButton("Lanjutkan pembayaran");
        styleButton(btnLanjutBayar, COLOR_TERRACOTTA, Color.WHITE);
        btnLanjutBayar.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnLanjutBayar.setPreferredSize(new Dimension(190, 38));

        btnLanjutBayar.addActionListener(e -> {
            if (keranjang.getListItems().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Keranjang kamu masih kosong!");
            } else {
                tabbedPane.setSelectedIndex(3);
            }
        });

        JPanel btnWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnWrapper.setOpaque(false);
        btnWrapper.add(btnLanjutBayar);

        totalCard.add(totalLeft, BorderLayout.WEST);
        totalCard.add(btnWrapper, BorderLayout.EAST);

        panelKeranjangContainer.add(totalCard);

        panelKeranjangContainer.revalidate();
        panelKeranjangContainer.repaint();
    }

    private JPanel createPembayaranPanel() {
        JPanel outerPanel = new JPanel(new BorderLayout());
        outerPanel.setBackground(COLOR_BG_CREAM);

        JPanel contentContainer = new JPanel();
        contentContainer.setLayout(new BoxLayout(contentContainer, BoxLayout.Y_AXIS));
        contentContainer.setBackground(COLOR_BG_CREAM);
        contentContainer.setBorder(new EmptyBorder(30, 60, 40, 60));

        JPanel headerTextGroup = new JPanel();
        headerTextGroup.setLayout(new BoxLayout(headerTextGroup, BoxLayout.Y_AXIS));
        headerTextGroup.setOpaque(false);

        JLabel lblTag = new JLabel("SELANGKAH LAGI");
        lblTag.setFont(new Font("SansSerif", Font.BOLD, 10));
        lblTag.setForeground(COLOR_TERRACOTTA);
        lblTag.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblTitle = new JLabel("Pembayaran KantinKu");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 24));
        lblTitle.setForeground(COLOR_PRIMARY_DARK);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSub = new JLabel("Periksa ringkasan pesanan dan pilih cara pembayaran.");
        lblSub.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblSub.setForeground(COLOR_TEXT_MUTED);
        lblSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        headerTextGroup.add(lblTag);
        headerTextGroup.add(Box.createVerticalStrut(4));
        headerTextGroup.add(lblTitle);
        headerTextGroup.add(Box.createVerticalStrut(4));
        headerTextGroup.add(lblSub);

        contentContainer.add(headerTextGroup);
        contentContainer.add(Box.createVerticalStrut(20));

        JPanel cardForm = new JPanel();
        cardForm.setLayout(new BoxLayout(cardForm, BoxLayout.Y_AXIS));
        cardForm.setBackground(Color.WHITE);
        cardForm.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 220, 210), 1),
                new EmptyBorder(25, 30, 25, 30)
        ));

        JLabel lblNamaTitle = new JLabel("Nama pembeli");
        lblNamaTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblNamaTitle.setForeground(COLOR_PRIMARY_DARK);
        lblNamaTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtNamaPembeli = new JTextField();
        txtNamaPembeli.setFont(new Font("SansSerif", Font.PLAIN, 13));
        txtNamaPembeli.setPreferredSize(new Dimension(0, 38));
        txtNamaPembeli.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        txtNamaPembeli.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtNamaPembeli.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 215, 205), 1),
                new EmptyBorder(5, 12, 5, 12)
        ));

        JLabel lblNamaHint = new JLabel("Nama ini akan dicantumkan pada detail pesanan.");
        lblNamaHint.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblNamaHint.setForeground(COLOR_TEXT_MUTED);
        lblNamaHint.setAlignmentX(Component.LEFT_ALIGNMENT);

        cardForm.add(lblNamaTitle);
        cardForm.add(Box.createVerticalStrut(8));
        cardForm.add(txtNamaPembeli);
        cardForm.add(Box.createVerticalStrut(4));
        cardForm.add(lblNamaHint);
        cardForm.add(Box.createVerticalStrut(20));

        JLabel lblSectionTitle1 = new JLabel("Ringkasan Pesanan");
        lblSectionTitle1.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblSectionTitle1.setForeground(COLOR_PRIMARY_DARK);
        lblSectionTitle1.setAlignmentX(Component.LEFT_ALIGNMENT);

        panelRingkasanPesanan = new JPanel();
        panelRingkasanPesanan.setLayout(new BoxLayout(panelRingkasanPesanan, BoxLayout.Y_AXIS));
        panelRingkasanPesanan.setBackground(COLOR_CARD_BG);
        panelRingkasanPesanan.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelRingkasanPesanan.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(235, 225, 215), 1),
                new EmptyBorder(15, 20, 15, 20)
        ));

        cardForm.add(lblSectionTitle1);
        cardForm.add(Box.createVerticalStrut(8));
        cardForm.add(panelRingkasanPesanan);
        cardForm.add(Box.createVerticalStrut(20));

        JLabel lblSectionTitle2 = new JLabel("Metode Pembayaran");
        lblSectionTitle2.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblSectionTitle2.setForeground(COLOR_PRIMARY_DARK);
        lblSectionTitle2.setAlignmentX(Component.LEFT_ALIGNMENT);

        bgMetodeBayar = new ButtonGroup();
        rbQRIS = createRadioCard("QRIS");
        rbTunai = createRadioCard("Tunai / COD");
        rbTransfer = createRadioCard("Transfer Bank (Virtual Account)");

        bgMetodeBayar.add(rbQRIS);
        bgMetodeBayar.add(rbTunai);
        bgMetodeBayar.add(rbTransfer);
        rbQRIS.setSelected(true);


        panelPilihBankContainer = new JPanel();
        panelPilihBankContainer.setLayout(new BoxLayout(panelPilihBankContainer, BoxLayout.Y_AXIS));
        panelPilihBankContainer.setOpaque(false);
        panelPilihBankContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelPilihBankContainer.setVisible(false);

        JLabel lblPilihBank = new JLabel("Pilih bank untuk Virtual Account");
        lblPilihBank.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblPilihBank.setForeground(COLOR_PRIMARY_DARK);
        lblPilihBank.setAlignmentX(Component.LEFT_ALIGNMENT);

        String[] bankOptions = {"BCA", "BNI", "BRI", "Mandiri", "Permata"};
        cbBankVA = new JComboBox<>(bankOptions);
        cbBankVA.setFont(new Font("SansSerif", Font.PLAIN, 13));
        cbBankVA.setBackground(new Color(254, 249, 243));
        cbBankVA.setPreferredSize(new Dimension(0, 38));
        cbBankVA.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        cbBankVA.setAlignmentX(Component.LEFT_ALIGNMENT);

        panelPilihBankContainer.add(Box.createVerticalStrut(10));
        panelPilihBankContainer.add(lblPilihBank);
        panelPilihBankContainer.add(Box.createVerticalStrut(6));
        panelPilihBankContainer.add(cbBankVA);

        rbQRIS.addActionListener(e -> panelPilihBankContainer.setVisible(false));
        rbTunai.addActionListener(e -> panelPilihBankContainer.setVisible(false));
        rbTransfer.addActionListener(e -> panelPilihBankContainer.setVisible(true));

        cardForm.add(lblSectionTitle2);
        cardForm.add(Box.createVerticalStrut(8));
        cardForm.add(rbQRIS.getParent());
        cardForm.add(Box.createVerticalStrut(8));
        cardForm.add(rbTunai.getParent());
        cardForm.add(Box.createVerticalStrut(8));
        cardForm.add(rbTransfer.getParent());
        cardForm.add(panelPilihBankContainer);
        cardForm.add(Box.createVerticalStrut(20));

        JPanel panelTotal = new JPanel(new BorderLayout());
        panelTotal.setBackground(new Color(254, 243, 199));
        panelTotal.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelTotal.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(243, 220, 150), 1),
                new EmptyBorder(12, 18, 12, 18)
        ));

        lblTotalHargaPembayaran = new JLabel("Total Pembayaran: Rp 0");
        lblTotalHargaPembayaran.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblTotalHargaPembayaran.setForeground(COLOR_TERRACOTTA);
        panelTotal.add(lblTotalHargaPembayaran, BorderLayout.WEST);

        JPanel actionGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actionGroup.setOpaque(false);
        actionGroup.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton btnKonfirmasiBayar = new JButton("Lanjutkan pembayaran");
        styleButton(btnKonfirmasiBayar, COLOR_TERRACOTTA, Color.WHITE);
        btnKonfirmasiBayar.setFont(new Font("SansSerif", Font.BOLD, 13));

        JButton btnKembali = new JButton("Kembali");
        styleButton(btnKembali, new Color(140, 130, 125), Color.WHITE);
        btnKembali.setFont(new Font("SansSerif", Font.BOLD, 13));

        btnKembali.addActionListener(e -> tabbedPane.setSelectedIndex(2));

        btnKonfirmasiBayar.addActionListener(e -> {
            String namaInput = txtNamaPembeli.getText().trim();

            if (namaInput.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Silakan masukkan nama pemesan terlebih dahulu!", "Nama Kosong", JOptionPane.WARNING_MESSAGE);
                txtNamaPembeli.requestFocus();
                return;
            }

            if (keranjang.getListItems().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Pesanan kosong! Silakan pilih menu terlebih dahulu.");
                return;
            }

            int nomorBaru;
            try {
                nomorBaru = PesananBersama.nomorBerikutnya();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Penyimpanan pesanan tidak dapat dibaca: " + ex.getMessage(),
                        "Kesalahan penyimpanan", JOptionPane.ERROR_MESSAGE);
                return;
            }
            nomorPesananCount = nomorBaru + 1;
            IDPesananTerakhir = PesananBersama.formatId(nomorBaru);
            namaPembeliTerakhir = namaInput;
            totalPesananTerakhir = keranjang.getTotalHarga();
            metodePesananTerakhir = rbQRIS.isSelected() ? "QRIS"
                    : rbTransfer.isSelected() ? "Transfer Bank (" + cbBankVA.getSelectedItem() + ")"
                    : "Tunai / COD";

            StringBuilder rincian = new StringBuilder();
            for (ItemKeranjang item : keranjang.getListItems()) {
                if (rincian.length() > 0) rincian.append("\n");
                rincian.append(item.getMenu().getNama()).append(" x").append(item.getJumlah())
                        .append(" (Rp ").append(String.format("%,d", item.getSubtotal())).append(")");
            }
            rincianPesananTerakhir = rincian.toString();

            if (rbQRIS.isSelected()) {
                tampilkanModalQRIS(IDPesananTerakhir, namaInput, keranjang.getTotalHarga());
            } else if (rbTransfer.isSelected()) {
                String selectedBank = (String) cbBankVA.getSelectedItem();
                tampilkanModalVA(IDPesananTerakhir, namaInput, selectedBank, keranjang.getTotalHarga());
            } else {
                JOptionPane.showMessageDialog(this,
                    "Pembayaran Tunai / COD Berhasil!\n" +
                    "Silakan selesaikan pembayaran langsung di kasir kantin.",
                    "Pembayaran Sukses", JOptionPane.INFORMATION_MESSAGE);
                selesaikanPesanan();
            }
        });

        actionGroup.add(btnKonfirmasiBayar);
        actionGroup.add(btnKembali);

        cardForm.add(panelTotal);
        cardForm.add(Box.createVerticalStrut(20));
        cardForm.add(actionGroup);

        contentContainer.add(cardForm);

        updateRingkasanTampilan();

        JScrollPane scrollPane = new JScrollPane(contentContainer);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.getVerticalScrollBar().setUnitIncrement(22);
        scrollPane.setBorder(null);

        outerPanel.add(scrollPane, BorderLayout.CENTER);
        return outerPanel;
    }

    private void tampilkanModalVA(String kodePesanan, String namaPemesan, String bank, int totalHarga) {
        JDialog dialog = new JDialog(this, "Virtual Account " + bank, true);
        dialog.setSize(440, 520);
        dialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        mainPanel.setBackground(Color.WHITE);

        JLabel lblKode = new JLabel("PESANAN " + kodePesanan, SwingConstants.CENTER);
        lblKode.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblKode.setForeground(COLOR_TERRACOTTA);
        lblKode.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblJudul = new JLabel("Virtual Account " + bank, SwingConstants.CENTER);
        lblJudul.setFont(new Font("Serif", Font.BOLD, 22));
        lblJudul.setForeground(COLOR_PRIMARY_DARK);
        lblJudul.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblAtasNama = new JLabel("Atas nama " + namaPemesan, SwingConstants.CENTER);
        lblAtasNama.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lblAtasNama.setForeground(Color.GRAY);
        lblAtasNama.setAlignmentX(Component.CENTER_ALIGNMENT);


        String prefix = "8800";
        if (bank.equalsIgnoreCase("BCA")) prefix = "8800";
        else if (bank.equalsIgnoreCase("BNI")) prefix = "9880";
        else if (bank.equalsIgnoreCase("BRI")) prefix = "8870";
        else if (bank.equalsIgnoreCase("Mandiri")) prefix = "7001";
        else if (bank.equalsIgnoreCase("Permata")) prefix = "8400";

        String nomorVA = prefix + "0000000" + String.format("%02d", nomorPesananCount - 1);

        JPanel cardVA = new JPanel();
        cardVA.setLayout(new BoxLayout(cardVA, BoxLayout.Y_AXIS));
        cardVA.setBackground(new Color(248, 250, 248));
        cardVA.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 240, 230), 1),
                new EmptyBorder(15, 15, 15, 15)
        ));
        cardVA.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardVA.setMaximumSize(new Dimension(380, 210));

        JLabel lblBankTujuan = new JLabel("Bank tujuan: " + bank);
        lblBankTujuan.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblBankTujuan.setForeground(COLOR_TEXT_MUTED);
        lblBankTujuan.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel boxVANumber = new JPanel(new BorderLayout());
        boxVANumber.setBackground(Color.WHITE);
        boxVANumber.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createDashedBorder(COLOR_TERRACOTTA, 1, 4, 4, true),
                new EmptyBorder(12, 10, 12, 10)
        ));
        boxVANumber.setAlignmentX(Component.CENTER_ALIGNMENT);
        boxVANumber.setMaximumSize(new Dimension(340, 65));

        JLabel lblVALabel = new JLabel("Nomor Virtual Account", SwingConstants.CENTER);
        lblVALabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblVALabel.setForeground(COLOR_TEXT_MUTED);

        JLabel lblVANumber = new JLabel(nomorVA, SwingConstants.CENTER);
        lblVANumber.setFont(new Font("SansSerif", Font.BOLD, 20));
        lblVANumber.setForeground(COLOR_TERRACOTTA);

        boxVANumber.add(lblVALabel, BorderLayout.NORTH);
        boxVANumber.add(lblVANumber, BorderLayout.CENTER);

        JPanel panelTotal = new JPanel(new BorderLayout());
        panelTotal.setOpaque(false);
        panelTotal.setMaximumSize(new Dimension(340, 30));

        JLabel lblTotalText = new JLabel("Total pesanan");
        lblTotalText.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblTotalText.setForeground(COLOR_PRIMARY_DARK);

        JLabel lblTotalVal = new JLabel(String.format("Rp %,d", totalHarga));
        lblTotalVal.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblTotalVal.setForeground(COLOR_TERRACOTTA);

        panelTotal.add(lblTotalText, BorderLayout.WEST);
        panelTotal.add(lblTotalVal, BorderLayout.EAST);

        JButton btnSalinVA = new JButton("Salin nomor VA");
        btnSalinVA.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnSalinVA.setBackground(new Color(130, 115, 105));
        btnSalinVA.setForeground(Color.WHITE);
        btnSalinVA.setFocusPainted(false);
        btnSalinVA.setBorder(new EmptyBorder(8, 15, 8, 15));
        btnSalinVA.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSalinVA.setAlignmentX(Component.CENTER_ALIGNMENT);

        btnSalinVA.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(nomorVA), null);
            JOptionPane.showMessageDialog(dialog, "Nomor Virtual Account " + nomorVA + " berhasil disalin!", "Berhasil Disalin", JOptionPane.INFORMATION_MESSAGE);
        });

        cardVA.add(lblBankTujuan);
        cardVA.add(Box.createRigidArea(new Dimension(0, 10)));
        cardVA.add(boxVANumber);
        cardVA.add(Box.createRigidArea(new Dimension(0, 12)));
        cardVA.add(panelTotal);
        cardVA.add(Box.createRigidArea(new Dimension(0, 10)));
        cardVA.add(btnSalinVA);

        JButton btnKonfirmasi = new JButton("✓ Konfirmasi pesanan");
        styleButton(btnKonfirmasi, COLOR_TERRACOTTA, Color.WHITE);
        btnKonfirmasi.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnKonfirmasi.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnKonfirmasi.setMaximumSize(new Dimension(380, 42));

        btnKonfirmasi.addActionListener(e -> {
            JOptionPane.showMessageDialog(dialog, "Pesanan " + kodePesanan + " berhasil dikonfirmasi!", "Sukses", JOptionPane.INFORMATION_MESSAGE);
            dialog.dispose();
            selesaikanPesanan();
        });

        mainPanel.add(lblKode);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        mainPanel.add(lblJudul);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        mainPanel.add(lblAtasNama);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        mainPanel.add(cardVA);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        mainPanel.add(btnKonfirmasi);

        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    private void tampilkanModalQRIS(String kodePesanan, String namaPemesan, int totalHarga) {
        JDialog dialog = new JDialog(this, "Pembayaran QRIS", true);
        dialog.setSize(420, 560);
        dialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(Color.WHITE);

        JLabel lblKode = new JLabel("PESANAN " + kodePesanan, SwingConstants.CENTER);
        lblKode.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblKode.setForeground(COLOR_TERRACOTTA);
        lblKode.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblJudul = new JLabel("Pembayaran QRIS", SwingConstants.CENTER);
        lblJudul.setFont(new Font("Serif", Font.BOLD, 22));
        lblJudul.setForeground(COLOR_PRIMARY_DARK);
        lblJudul.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblAtasNama = new JLabel("Atas nama " + namaPemesan, SwingConstants.CENTER);
        lblAtasNama.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lblAtasNama.setForeground(Color.GRAY);
        lblAtasNama.setAlignmentX(Component.CENTER_ALIGNMENT);

        String[] qrisNames = {"qris.jpeg", "qris.png", "QRIS.jpeg", "QRIS.png"};
        ImageIcon iconQris = null;

        for (String name : qrisNames) {
            URL imgURL = getClass().getClassLoader().getResource("images/" + name);
            if (imgURL != null) {
                iconQris = new ImageIcon(imgURL);
                break;
            }
            File file = new File("src/images/" + name);
            if (file.exists()) {
                iconQris = new ImageIcon(file.getAbsolutePath());
                break;
            }
            File rootFile = new File("images/" + name);
            if (rootFile.exists()) {
                iconQris = new ImageIcon(rootFile.getAbsolutePath());
                break;
            }
        }

        JLabel lblGambar;
        if (iconQris != null) {
            Image img = iconQris.getImage().getScaledInstance(220, 220, Image.SCALE_SMOOTH);
            lblGambar = new JLabel(new ImageIcon(img));
        } else {
            lblGambar = new JLabel("<html><center>[ Gambar QRIS ]</center></html>", SwingConstants.CENTER);
            lblGambar.setPreferredSize(new Dimension(220, 220));
            lblGambar.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        }
        lblGambar.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel panelTotal = new JPanel(new BorderLayout());
        panelTotal.setBackground(new Color(245, 247, 245));
        panelTotal.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        panelTotal.setMaximumSize(new Dimension(360, 45));

        JLabel lblTotalText = new JLabel("Total pesanan");
        lblTotalText.setFont(new Font("SansSerif", Font.BOLD, 14));

        JLabel lblTotalVal = new JLabel(String.format("Rp %,d", totalHarga));
        lblTotalVal.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblTotalVal.setForeground(COLOR_TERRACOTTA);

        panelTotal.add(lblTotalText, BorderLayout.WEST);
        panelTotal.add(lblTotalVal, BorderLayout.EAST);

        JButton btnKonfirmasi = new JButton("✓ Konfirmasi pesanan");
        styleButton(btnKonfirmasi, COLOR_TERRACOTTA, Color.WHITE);
        btnKonfirmasi.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnKonfirmasi.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnKonfirmasi.setMaximumSize(new Dimension(360, 42));

        btnKonfirmasi.addActionListener(e -> {
            JOptionPane.showMessageDialog(dialog, "Pesanan berhasil dikonfirmasi!", "Sukses", JOptionPane.INFORMATION_MESSAGE);
            dialog.dispose();
            selesaikanPesanan();
        });

        mainPanel.add(lblKode);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        mainPanel.add(lblJudul);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        mainPanel.add(lblAtasNama);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        mainPanel.add(lblGambar);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        mainPanel.add(panelTotal);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        mainPanel.add(btnKonfirmasi);

        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    private void selesaikanPesanan() {
        try {
            PesananBersama.Pesanan tersimpan = PesananBersama.simpanBaru(new PesananBersama.Pesanan(
                    IDPesananTerakhir, namaPembeliTerakhir, metodePesananTerakhir,
                    totalPesananTerakhir, "Menunggu konfirmasi", rincianPesananTerakhir));
            PesananBersama.catatIdPembeli(tersimpan.id);
            IDPesananTerakhir = tersimpan.id;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Pesanan belum dapat disimpan: " + ex.getMessage(),
                    "Kesalahan penyimpanan", JOptionPane.ERROR_MESSAGE);
            return;
        }
        penandaPesananTerlihat = PesananBersama.penandaPerubahan();
        txtNamaPembeli.setText("");
        keranjang.kosongkan();
        lblBadgeKeranjang.setText(" 0 ");

        updateKeranjangTampilan();
        updateRingkasanTampilan();
        updatePesananSayaTampilan();

        tabbedPane.setSelectedIndex(4);
    }

    private JPanel createPesananSayaPanel() {
        JPanel outerPanel = new JPanel(new BorderLayout());
        outerPanel.setBackground(COLOR_BG_CREAM);

        panelPesananSayaContainer = new JPanel();
        panelPesananSayaContainer.setLayout(new BoxLayout(panelPesananSayaContainer, BoxLayout.Y_AXIS));
        panelPesananSayaContainer.setBackground(COLOR_BG_CREAM);

        updatePesananSayaTampilan();

        JScrollPane scrollPane = new JScrollPane(panelPesananSayaContainer);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.getVerticalScrollBar().setUnitIncrement(22);
        scrollPane.setBorder(null);

        outerPanel.add(scrollPane, BorderLayout.CENTER);
        return outerPanel;
    }

    /** Pesanan milik pembeli ini, terbaru lebih dulu, dibaca langsung dari penyimpanan penjual. */
    private List<PesananBersama.Pesanan> pesananSaya() {
        List<PesananBersama.Pesanan> hasil = new ArrayList<>();
        try {
            Map<String, PesananBersama.Pesanan> semua = new LinkedHashMap<>();
            for (PesananBersama.Pesanan p : PesananBersama.baca()) semua.put(p.id, p);
            List<String> ids = PesananBersama.bacaIdPembeli();
            for (int i = ids.size() - 1; i >= 0; i--) {
                PesananBersama.Pesanan p = semua.get(ids.get(i));
                if (p != null) hasil.add(p);
            }
        } catch (IOException ex) {
            System.err.println("Status pesanan belum dapat disinkronkan: " + ex.getMessage());
        }
        return hasil;
    }

    private JPanel buatBarisPesanan(PesananBersama.Pesanan p) {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setOpaque(false);

        JPanel inner = new JPanel(new BorderLayout());
        inner.setOpaque(false);
        inner.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblID = new JLabel(p.id);
        lblID.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblID.setForeground(COLOR_PRIMARY_DARK);

        boolean selesai = p.status.equalsIgnoreCase("Selesai");
        JLabel lblBadge = new JLabel(" " + labelStatusPembeli(p.status) + " ");
        lblBadge.setOpaque(true);
        lblBadge.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblBadge.setBackground(selesai ? new Color(225, 242, 226) : COLOR_BADGE_BG);
        lblBadge.setForeground(selesai ? new Color(35, 110, 55) : COLOR_TERRACOTTA);

        inner.add(lblID, BorderLayout.WEST);
        inner.add(lblBadge, BorderLayout.EAST);
        box.add(inner);

        JLabel lblIsi = new JLabel("<html>" + p.rincian.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\n", "<br>")
                + "<br>Total: Rp " + String.format("%,d", p.total) + " · " + p.metode + "</html>");
        lblIsi.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblIsi.setForeground(COLOR_TEXT_MUTED);
        lblIsi.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.add(Box.createVerticalStrut(4));
        box.add(lblIsi);
        return box;
    }

    private JPanel buatDaftarPesanan(List<PesananBersama.Pesanan> list) {
        JPanel daftar = new JPanel();
        daftar.setLayout(new BoxLayout(daftar, BoxLayout.Y_AXIS));
        daftar.setOpaque(false);
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                daftar.add(Box.createVerticalStrut(10));
                daftar.add(new JSeparator(JSeparator.HORIZONTAL));
                daftar.add(Box.createVerticalStrut(10));
            }
            daftar.add(buatBarisPesanan(list.get(i)));
        }
        return daftar;
    }

    private String labelStatusPembeli(String status) {
        if (status == null || status.isBlank()) return "Menunggu konfirmasi dari penjual";
        if (status.equalsIgnoreCase("Menunggu konfirmasi")) return "Menunggu konfirmasi dari penjual";
        if (status.equalsIgnoreCase("Diproses")) return "Telah dikonfirmasi · Sedang diproses";
        if (status.equalsIgnoreCase("Siap Diambil")) return "Siap diambil";
        if (status.equalsIgnoreCase("Selesai")) return "Pesanan selesai";
        return status;
    }

    private void updatePesananSayaTampilan() {
        if (panelPesananSayaContainer == null) return;
        List<PesananBersama.Pesanan> daftarPesanan = pesananSaya();
        PesananBersama.Pesanan terbaru = daftarPesanan.isEmpty() ? null : daftarPesanan.get(0);
        List<PesananBersama.Pesanan> pesananAktif = new ArrayList<>();
        List<PesananBersama.Pesanan> pesananSelesai = new ArrayList<>();
        for (PesananBersama.Pesanan ps : daftarPesanan) {
            if (ps.status.equalsIgnoreCase("Selesai")) pesananSelesai.add(ps);
            else pesananAktif.add(ps);
        }
        panelPesananSayaContainer.removeAll();
        panelPesananSayaContainer.setBorder(new EmptyBorder(30, 60, 40, 60));

        JPanel headerTextGroup = new JPanel();
        headerTextGroup.setLayout(new BoxLayout(headerTextGroup, BoxLayout.Y_AXIS));
        headerTextGroup.setOpaque(false);

        JLabel lblTitle = new JLabel("Status Pesanan KantinKu");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 24));
        lblTitle.setForeground(COLOR_PRIMARY_DARK);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSub = new JLabel("Pantau status dan riwayat pesananmu di sini.");
        lblSub.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblSub.setForeground(COLOR_TEXT_MUTED);
        lblSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        headerTextGroup.add(lblTitle);
        headerTextGroup.add(Box.createVerticalStrut(4));
        headerTextGroup.add(lblSub);

        panelPesananSayaContainer.add(headerTextGroup);
        panelPesananSayaContainer.add(Box.createVerticalStrut(20));

        JPanel cardMain = new JPanel();
        cardMain.setLayout(new BoxLayout(cardMain, BoxLayout.Y_AXIS));
        cardMain.setBackground(Color.WHITE);
        cardMain.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 220, 210), 1),
                new EmptyBorder(25, 30, 25, 30)
        ));

        JLabel lblSec1 = new JLabel("Detail Pesanan");
        lblSec1.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblSec1.setForeground(COLOR_PRIMARY_DARK);
        lblSec1.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel boxDetail = new JPanel(new BorderLayout());
        boxDetail.setBackground(COLOR_EMPTY_BOX);
        boxDetail.setAlignmentX(Component.LEFT_ALIGNMENT);
        boxDetail.setBorder(new EmptyBorder(15, 20, 15, 20));

        if (terbaru == null) {
            JLabel lblEmpty1 = new JLabel("Belum ada pesanan. Pilih menu untuk memulai.");
            lblEmpty1.setFont(new Font("SansSerif", Font.PLAIN, 12));
            lblEmpty1.setForeground(COLOR_TEXT_MUTED);
            boxDetail.add(lblEmpty1, BorderLayout.WEST);
        } else {
            JPanel detailContent = new JPanel();
            detailContent.setLayout(new BoxLayout(detailContent, BoxLayout.Y_AXIS));
            detailContent.setOpaque(false);

            JLabel lblInfoOrder = new JLabel("Nomor pesanan: " + terbaru.id + " · " + terbaru.status);
            lblInfoOrder.setFont(new Font("SansSerif", Font.PLAIN, 13));
            lblInfoOrder.setForeground(COLOR_PRIMARY_DARK);

            detailContent.add(lblInfoOrder);
            detailContent.add(Box.createVerticalStrut(6));

            for (String baris : terbaru.rincian.split("\n")) {
                if (baris.isBlank()) continue;
                JLabel lblItem = new JLabel("• " + baris);
                lblItem.setFont(new Font("SansSerif", Font.PLAIN, 12));
                lblItem.setForeground(COLOR_TEXT_MUTED);
                detailContent.add(lblItem);
            }
            boxDetail.add(detailContent, BorderLayout.CENTER);
        }

        cardMain.add(lblSec1);
        cardMain.add(Box.createVerticalStrut(10));
        cardMain.add(boxDetail);
        cardMain.add(Box.createVerticalStrut(20));
        cardMain.add(new JSeparator(JSeparator.HORIZONTAL));
        cardMain.add(Box.createVerticalStrut(20));

        JLabel lblSec2 = new JLabel("Status Pesanan");
        lblSec2.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblSec2.setForeground(COLOR_PRIMARY_DARK);
        lblSec2.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        statusRow.setOpaque(false);
        statusRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblBadgeStatus = new JLabel();
        lblBadgeStatus.setOpaque(true);
        lblBadgeStatus.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblBadgeStatus.setBorder(new EmptyBorder(8, 16, 8, 16));

        if (terbaru == null) {
            lblBadgeStatus.setText("Belum ada pesanan");
            lblBadgeStatus.setBackground(COLOR_BADGE_BG);
            lblBadgeStatus.setForeground(COLOR_TERRACOTTA);
        } else {
            boolean terbaruSelesai = terbaru.status.equalsIgnoreCase("Selesai");
            lblBadgeStatus.setText(labelStatusPembeli(terbaru.status));
            lblBadgeStatus.setBackground(terbaruSelesai ? new Color(225, 242, 226) : COLOR_BADGE_BG);
            lblBadgeStatus.setForeground(terbaruSelesai ? new Color(35, 110, 55) : COLOR_TERRACOTTA);
            lblBadgeStatus.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(COLOR_TERRACOTTA, 1, true),
                    new EmptyBorder(8, 16, 8, 16)
            ));
        }

        JButton btnPesanLagi = new JButton("Pesan Lagi");
        styleButton(btnPesanLagi, new Color(140, 130, 125), Color.WHITE);
        btnPesanLagi.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnPesanLagi.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnPesanLagi.addActionListener(e -> tabbedPane.setSelectedIndex(1));

        statusRow.add(lblBadgeStatus);

        cardMain.add(lblSec2);
        cardMain.add(Box.createVerticalStrut(10));
        cardMain.add(statusRow);
        cardMain.add(Box.createVerticalStrut(12));
        cardMain.add(btnPesanLagi);
        cardMain.add(Box.createVerticalStrut(20));
        cardMain.add(new JSeparator(JSeparator.HORIZONTAL));
        cardMain.add(Box.createVerticalStrut(20));

        JLabel lblSec3 = new JLabel("Pesanan Aktif");
        lblSec3.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblSec3.setForeground(COLOR_PRIMARY_DARK);
        lblSec3.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel boxAktif = new JPanel(new BorderLayout());
        boxAktif.setBackground(COLOR_EMPTY_BOX);
        boxAktif.setAlignmentX(Component.LEFT_ALIGNMENT);
        boxAktif.setBorder(new EmptyBorder(15, 20, 15, 20));

        if (pesananAktif.isEmpty()) {
            JLabel lblEmpty2 = new JLabel(terbaru != null
                    ? "Semua pesananmu sudah selesai." : "Belum ada pesanan yang sedang berjalan.");
            lblEmpty2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            lblEmpty2.setForeground(COLOR_TEXT_MUTED);
            boxAktif.add(lblEmpty2, BorderLayout.WEST);
        } else {
            boxAktif.add(buatDaftarPesanan(pesananAktif), BorderLayout.CENTER);
        }

        cardMain.add(lblSec3);
        cardMain.add(Box.createVerticalStrut(10));
        cardMain.add(boxAktif);
        cardMain.add(Box.createVerticalStrut(20));
        cardMain.add(new JSeparator(JSeparator.HORIZONTAL));
        cardMain.add(Box.createVerticalStrut(20));

        JLabel lblSec4 = new JLabel("Riwayat Pesanan");
        lblSec4.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblSec4.setForeground(COLOR_PRIMARY_DARK);
        lblSec4.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel boxRiwayat = new JPanel(new BorderLayout());
        boxRiwayat.setBackground(COLOR_EMPTY_BOX);
        boxRiwayat.setAlignmentX(Component.LEFT_ALIGNMENT);
        boxRiwayat.setBorder(new EmptyBorder(15, 20, 15, 20));

        if (pesananSelesai.isEmpty()) {
            JLabel lblEmpty3 = new JLabel("Riwayat pesanan selesai akan muncul di sini.");
            lblEmpty3.setFont(new Font("SansSerif", Font.PLAIN, 12));
            lblEmpty3.setForeground(COLOR_TEXT_MUTED);
            boxRiwayat.add(lblEmpty3, BorderLayout.WEST);
        } else {
            boxRiwayat.add(buatDaftarPesanan(pesananSelesai), BorderLayout.CENTER);
        }

        cardMain.add(lblSec4);
        cardMain.add(Box.createVerticalStrut(10));
        cardMain.add(boxRiwayat);

        panelPesananSayaContainer.add(cardMain);

        panelPesananSayaContainer.revalidate();
        panelPesananSayaContainer.repaint();
    }

    private JRadioButton createRadioCard(String text) {
        JRadioButton rb = new JRadioButton(text);
        rb.setFont(new Font("SansSerif", Font.PLAIN, 13));
        rb.setBackground(COLOR_CARD_BG);
        rb.setFocusPainted(false);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(COLOR_CARD_BG);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 215, 205), 1),
                new EmptyBorder(10, 15, 10, 15)
        ));
        wrapper.add(rb, BorderLayout.WEST);

        return rb;
    }

    private void updateRingkasanTampilan() {
        if (panelRingkasanPesanan == null) return;

        panelRingkasanPesanan.removeAll();

        if (keranjang.getListItems().isEmpty()) {
            JPanel emptyRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            emptyRow.setOpaque(false);

            JLabel lblKosong = new JLabel("Keranjang kosong. Tambahkan menu sebelum membayar. ");
            lblKosong.setForeground(COLOR_TEXT_MUTED);
            lblKosong.setFont(new Font("SansSerif", Font.PLAIN, 12));

            JButton btnPilihMenu = new JButton("Pilih menu");
            btnPilihMenu.setBorder(null);
            btnPilihMenu.setContentAreaFilled(false);
            btnPilihMenu.setForeground(COLOR_TERRACOTTA);
            btnPilihMenu.setFont(new Font("SansSerif", Font.BOLD, 12));
            btnPilihMenu.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnPilihMenu.addActionListener(e -> tabbedPane.setSelectedIndex(1));

            emptyRow.add(lblKosong);
            emptyRow.add(btnPilihMenu);

            panelRingkasanPesanan.add(emptyRow);
        } else {
            List<ItemKeranjang> items = keranjang.getListItems();
            for (int i = 0; i < items.size(); i++) {
                ItemKeranjang item = items.get(i);

                JPanel row = new JPanel(new BorderLayout());
                row.setBackground(COLOR_CARD_BG);
                row.setBorder(new EmptyBorder(8, 0, 8, 0));

                JPanel left = new JPanel();
                left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
                left.setBackground(COLOR_CARD_BG);

                JLabel lblName = new JLabel(item.getMenu().getNama());
                lblName.setFont(new Font("SansSerif", Font.BOLD, 13));
                lblName.setForeground(COLOR_PRIMARY_DARK);

                JLabel lblDesc = new JLabel(item.getJumlah() + " × Rp " + String.format("%,d", item.getMenu().getHarga()));
                lblDesc.setFont(new Font("SansSerif", Font.PLAIN, 11));
                lblDesc.setForeground(COLOR_TEXT_MUTED);

                left.add(lblName);
                left.add(lblDesc);

                JLabel lblPrice = new JLabel("Rp " + String.format("%,d", item.getSubtotal()));
                lblPrice.setFont(new Font("SansSerif", Font.BOLD, 13));
                lblPrice.setForeground(COLOR_PRIMARY_DARK);

                row.add(left, BorderLayout.WEST);
                row.add(lblPrice, BorderLayout.EAST);

                panelRingkasanPesanan.add(row);
                if (i < items.size() - 1) {
                    panelRingkasanPesanan.add(new JSeparator(JSeparator.HORIZONTAL));
                }
            }
        }

        if (lblTotalHargaPembayaran != null) {
            lblTotalHargaPembayaran.setText("Total Pembayaran: Rp " + String.format("%,d", keranjang.getTotalHarga()));
        }

        panelRingkasanPesanan.revalidate();
        panelRingkasanPesanan.repaint();
    }

    private void styleCounterButton(JButton btn) {
        btn.setPreferredSize(new Dimension(32, 32));
        btn.setBackground(new Color(245, 235, 225));
        btn.setForeground(Color.BLACK);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(new Color(230, 220, 210), 1));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void styleButton(JButton btn, Color bg, Color fg) {
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(35);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        table.getTableHeader().setBackground(COLOR_TERRACOTTA);
        table.getTableHeader().setForeground(Color.WHITE);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.setDefaultRenderer(Object.class, centerRenderer);

        return table;
    }

    private ImageIcon loadMenuImage(String fileName) {
        URL imgURL = getClass().getClassLoader().getResource("images/" + fileName);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        }

        String[] paths = {
            "src/images/" + fileName,
            "images/" + fileName,
            "KantinKuApp/src/images/" + fileName,
            fileName
        };

        for (String path : paths) {
            File file = new File(path);
            if (file.exists()) {
                return new ImageIcon(file.getAbsolutePath());
            }
        }
        return null;
    }

    private void hapusItemKeranjang(int row) {
        if (row >= 0 && row < keranjang.getListItems().size()) {
            keranjang.hapusItem(row);
            lblBadgeKeranjang.setText(" " + keranjang.getTotalQty() + " ");
            updateKeranjangTampilan();
            updateRingkasanTampilan();
        }
    }

    private class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setFont(new Font("SansSerif", Font.BOLD, 12));
            setForeground(COLOR_TERRACOTTA);
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "Hapus" : value.toString());
            return this;
        }
    }

    private class ButtonEditor extends DefaultCellEditor {
        private JButton button;
        private String label;
        private boolean isPushed;
        private int selectedRow;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.setFont(new Font("SansSerif", Font.BOLD, 12));
            button.setForeground(COLOR_TERRACOTTA);
            button.setBackground(Color.WHITE);
            button.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));

            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                      boolean isSelected, int row, int column) {
            label = (value == null) ? "Hapus" : value.toString();
            button.setText(label);
            isPushed = true;
            selectedRow = row;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                SwingUtilities.invokeLater(() -> hapusItemKeranjang(selectedRow));
            }
            isPushed = false;
            return label;
        }

        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }
}
