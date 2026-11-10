package views.pembeli;

import models.Menu;
import models.ItemKeranjang;
import models.Pesanan;
import controllers.PembeliController;
import controllers.AntrianController;
import services.Keranjang;
import util.JendelaUtil;
import views.antri.AntrianFrame;
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
import java.nio.file.Path;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class KantinKuPembeliView extends JFrame {

    private final Color COLOR_PRIMARY_DARK = new Color(50, 40, 35);
    private final Color COLOR_BG_CREAM = new Color(245, 235, 224);
    private final Color COLOR_CARD_BG = Color.WHITE;
    private final Color COLOR_TERRACOTTA = new Color(194, 65, 12);
    private final Color COLOR_TEXT_MUTED = new Color(120, 110, 100);
    private final Color COLOR_BADGE_BG = new Color(253, 238, 227);
    private final Color COLOR_EMPTY_BOX = new Color(245, 236, 223);

    private JTabbedPane tabbedPane;
    private JLabel lblTotalHargaKeranjang;
    private JLabel lblTotalHargaPembayaran;
    private JLabel lblBadgeKeranjang;
    private JPanel panelRingkasanPesanan;
    private JPanel panelKeranjangContainer;
    private JPanel panelPesananSayaContainer;

    private JTextField txtNamaPembeli;
    private JTextField txtHpPembeli;
    private ButtonGroup bgMetodeBayar;
    private JRadioButton rbQRIS, rbTunai, rbTransfer;
    private JComboBox<String> cbBankVA;
    private JPanel panelPilihBankContainer;

    private Keranjang keranjang = new Keranjang();
    private int nomorPesananCount = 1;

    // data pesanan yang sedang dibayar (baru disimpan ke penjual setelah pembayaran dikonfirmasi)
    private String IDPesananTerakhir = "";
    private String namaPembeliTerakhir = "";
    private String hpPesananTerakhir = "";

    private String pembeliAktif = "";
    private String hpPembeliAktif = "";
    private String metodePesananTerakhir = "";
    private int totalPesananTerakhir = 0;
    private String rincianPesananTerakhir = "";
    private boolean penyelesaianBerjalan;
    private Pesanan pesananAktifPembeli;

    // sinkronisasi dengan penjual
    private javax.swing.Timer timerSinkron;
    private long penandaPesananTerlihat = Long.MIN_VALUE;
    private long penandaKatalogTerlihat = Long.MIN_VALUE;

    private JTextField txtCariMenu;
    private JPanel gridMenuContainer;
    private JLabel lblJumlahTersedia;
    private JButton btnFilterSemua, btnFilterMakanan, btnFilterMinuman;
    private String filterAktif = "Semua";

    private final List<Menu> dataMasterMenu = new ArrayList<>();
    private final PembeliController controller;

    public KantinKuPembeliView() {
        this(new PembeliController());
    }

    public KantinKuPembeliView(PembeliController controller) {
        this.controller = controller;
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
        JButton btnAntrian = new JButton("Lihat Antrian");
        btnAntrian.setForeground(COLOR_TERRACOTTA);
        btnAntrian.setBackground(Color.WHITE);
        btnAntrian.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnAntrian.setFocusPainted(false);
        btnAntrian.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_TERRACOTTA, 1),
                new EmptyBorder(7, 12, 7, 12)));
        btnAntrian.addActionListener(e -> new AntrianFrame(new AntrianController()).setVisible(true));
        headerPanel.add(btnAntrian, BorderLayout.EAST);

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

    /** Semua komponen langsung dalam panel dirapatkan ke kiri (BoxLayout memusatkan komponen sempit bila tidak). */
    private static void ratakiri(Container c) {
        for (Component k : c.getComponents()) {
            if (k instanceof JComponent) ((JComponent) k).setAlignmentX(Component.LEFT_ALIGNMENT);
        }
    }

    private int stokTersedia(String namaMenu) {
        for (Menu m : dataMasterMenu) {
            if (m.getNama().equalsIgnoreCase(namaMenu)) return m.getStok();
        }
        return 0;
    }

    /**
     * Memeriksa perubahan dari penjual setiap 1,5 detik: menu baru/disembunyikan
     * dan perubahan status pesanan (misalnya pesanan sudah dikonfirmasi penjual).
     */
    private void mulaiSinkronisasi() {
        penandaPesananTerlihat = controller.penandaPesanan();
        timerSinkron = new javax.swing.Timer(1500, e -> {
            long katalog = penandaKatalogBersama();
            if (katalog != penandaKatalogTerlihat) {
                muatKatalogBersama();
                updateGridMenu();
            }
            long pesanan = controller.penandaPesanan();
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

    private long penandaKatalogBersama() {
        return controller.penandaKatalog();
    }

    /**
     * Menu pembeli berasal dari katalog aktif di database.
     */
    private void muatKatalogBersama() {
        penandaKatalogTerlihat = penandaKatalogBersama();
        try {
            Optional<List<Menu>> katalog = controller.bacaMenuAktif();
            if (katalog.isEmpty()) {
                dataMasterMenu.clear();
                return;
            }
            dataMasterMenu.clear();
            dataMasterMenu.addAll(katalog.get());
        } catch (IOException ex) {
            System.err.println("Katalog bersama tidak dapat dibaca: " + ex.getMessage());
            dataMasterMenu.clear();
        }
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

        btnFilterSemua.addActionListener(e -> setFilterAktif("Semua"));
        btnFilterMakanan.addActionListener(e -> setFilterAktif("Makanan"));
        btnFilterMinuman.addActionListener(e -> setFilterAktif("Minuman"));

        filterRow.add(btnFilterSemua);
        filterRow.add(btnFilterMakanan);
        filterRow.add(btnFilterMinuman);

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
        isiWarna(btn);
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
                    (filterAktif.equals("Minuman") && m.getKategori().equalsIgnoreCase("Minuman"));

            if (matchesSearch && matchesCategory) {
                listFiltered.add(m);
            }
        }

        lblJumlahTersedia.setText(listFiltered.size() + " menu tersedia");
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

            imageContainer.add(lblImage, JLayeredPane.DEFAULT_LAYER);

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

            JPanel badgeRow = new JPanel(new BorderLayout());
            badgeRow.setOpaque(false);
            badgeRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            badgeRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
            boolean stokHabis = menu.getStok() <= 0;
            JLabel lblStok = new JLabel(stokHabis ? "Stok habis" : "Stok: " + menu.getStok());
            lblStok.setFont(new Font("SansSerif", Font.BOLD, 11));
            lblStok.setForeground(stokHabis ? new Color(180, 35, 24) : COLOR_TEXT_MUTED);
            badgeRow.add(lblBadgeKategori, BorderLayout.WEST);
            badgeRow.add(lblStok, BorderLayout.EAST);

            infoPanel.add(badgeRow);
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
            isiWarna(btnPlus);
            btnPlus.setBackground(COLOR_TERRACOTTA);
            btnPlus.setForeground(Color.WHITE);
            btnPlus.setFont(new Font("SansSerif", Font.BOLD, 18));
            btnPlus.setMargin(new Insets(0, 0, 0, 0));
            btnPlus.setFocusPainted(false);
            btnPlus.setBorder(null);
            btnPlus.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnPlus.setPreferredSize(new Dimension(34, 34));

            if (stokHabis) {
                btnPlus.setEnabled(false);
                btnPlus.setBackground(new Color(200, 192, 185));
                btnPlus.setToolTipText("Stok habis");
            }
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
        final int sisaStok = menu.getStok() - keranjang.jumlahDi(menu.getNama());
        if (menu.getStok() <= 0) {
            JOptionPane.showMessageDialog(this, "Maaf, stok " + menu.getNama() + " sedang habis.",
                    "Stok habis", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (sisaStok <= 0) {
            JOptionPane.showMessageDialog(this, "Seluruh stok " + menu.getNama() + " sudah ada di keranjangmu.",
                    "Stok terbatas", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
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

        JLabel lblPriceDesc = new JLabel("Rp " + String.format("%,d", menu.getHarga()) + " per porsi · Sisa stok " + sisaStok);
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
            if (porsiCount[0] >= sisaStok) {
                JOptionPane.showMessageDialog(dialog, "Stok " + menu.getNama() + " hanya tersisa " + sisaStok + " porsi.",
                        "Stok terbatas", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
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

        updateKeranjangTampilan();

        JScrollPane scrollPane = new JScrollPane(panelKeranjangContainer);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.getVerticalScrollBar().setUnitIncrement(22);
        scrollPane.setBorder(null);

        outerPanel.add(scrollPane, BorderLayout.CENTER);
        return outerPanel;
    }

    private void updateKeranjangTampilan() {
        if (panelKeranjangContainer == null) return;
        panelKeranjangContainer.removeAll();
        panelKeranjangContainer.setBorder(new EmptyBorder(30, 60, 40, 60));

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

        if (keranjang.getListItems().isEmpty()) {
            tableCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
            tableCard.add(buatTabelKeranjang(), BorderLayout.CENTER);
        } else {
            int tinggi = Math.min(300, 40 + keranjang.getListItems().size() * 46);
            tableCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, tinggi + 4));
            JScrollPane tableScroll = new JScrollPane(buatTabelKeranjang());
            tableScroll.setBorder(null);
            tableScroll.getViewport().setBackground(Color.WHITE);
            tableScroll.getVerticalScrollBar().setUnitIncrement(16);
            tableScroll.setPreferredSize(new Dimension(0, tinggi));
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

        ratakiri(panelKeranjangContainer);
        panelKeranjangContainer.revalidate();
        panelKeranjangContainer.repaint();
    }

    /** Tabel keranjang: tiap baris punya tombol Edit (ubah jumlah) dan Hapus. */
    private JPanel buatTabelKeranjang() {
        JPanel tabel = new JPanel(new GridBagLayout());
        tabel.setBackground(Color.WHITE);
        double[] bobot = {0.5, 3, 1, 1.6, 1.6, 2.2};
        String[] judul = {"NO", "NAMA MENU", "JUMLAH", "HARGA SATUAN", "SUBTOTAL", "AKSI"};

        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.gridy = 0;
        for (int c = 0; c < judul.length; c++) {
            JLabel l = new JLabel(judul[c], SwingConstants.CENTER);
            l.setOpaque(true);
            l.setBackground(COLOR_TERRACOTTA);
            l.setForeground(Color.WHITE);
            l.setFont(new Font("SansSerif", Font.BOLD, 11));
            l.setPreferredSize(new Dimension(10, 40));
            gc.gridx = c;
            gc.weightx = bobot[c];
            tabel.add(l, gc);
        }

        List<ItemKeranjang> items = keranjang.getListItems();
        if (items.isEmpty()) {
            JLabel lblEmpty = new JLabel("Keranjang masih kosong. Pilih menu untuk mulai memesan.");
            lblEmpty.setFont(new Font("SansSerif", Font.BOLD, 13));
            lblEmpty.setForeground(COLOR_TEXT_MUTED);
            lblEmpty.setBorder(new EmptyBorder(20, 20, 20, 20));
            gc.gridy = 1;
            gc.gridx = 0;
            gc.gridwidth = judul.length;
            gc.weightx = 1;
            tabel.add(lblEmpty, gc);
            return tabel;
        }

        Color garis = new Color(238, 230, 222);
        for (int i = 0; i < items.size(); i++) {
            ItemKeranjang item = items.get(i);
            final int index = i;
            gc.gridy = i + 1;
            String[] teks = {
                String.valueOf(i + 1),
                item.getMenu().getNama(),
                String.valueOf(item.getJumlah()),
                "Rp " + String.format("%,d", item.getMenu().getHarga()),
                "Rp " + String.format("%,d", item.getSubtotal())
            };
            for (int c = 0; c < teks.length; c++) {
                JLabel l = new JLabel(teks[c], c == 1 ? SwingConstants.LEFT : SwingConstants.CENTER);
                l.setFont(new Font("SansSerif", c == 1 ? Font.BOLD : Font.PLAIN, 12));
                l.setForeground(COLOR_PRIMARY_DARK);
                l.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, garis), new EmptyBorder(0, c == 1 ? 14 : 4, 0, 4)));
                l.setPreferredSize(new Dimension(10, 46));
                gc.gridx = c;
                gc.weightx = bobot[c];
                tabel.add(l, gc);
            }

            JPanel aksi = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 8));
            aksi.setOpaque(false);
            aksi.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, garis));
            JButton btnEdit = new JButton("Edit");
            styleButton(btnEdit, new Color(245, 235, 225), COLOR_PRIMARY_DARK);
            btnEdit.setBorder(new EmptyBorder(5, 12, 5, 12));
            btnEdit.addActionListener(e -> editItemKeranjang(index));
            JButton btnHapus = new JButton("Hapus");
            styleButton(btnHapus, new Color(253, 236, 234), new Color(180, 35, 24));
            btnHapus.setBorder(new EmptyBorder(5, 12, 5, 12));
            btnHapus.addActionListener(e -> hapusItemKeranjang(index));
            aksi.add(btnEdit);
            aksi.add(btnHapus);
            gc.gridx = 5;
            gc.weightx = bobot[5];
            tabel.add(aksi, gc);
        }
        return tabel;
    }

    /** Dialog ubah jumlah porsi untuk satu baris keranjang (tidak boleh melebihi stok). */
    private void editItemKeranjang(int index) {
        if (index < 0 || index >= keranjang.getListItems().size()) return;
        ItemKeranjang item = keranjang.getListItems().get(index);
        Menu menu = item.getMenu();
        int stok = stokTersedia(menu.getNama());
        if (stok <= 0) {
            JOptionPane.showMessageDialog(this, "Stok " + menu.getNama() + " sudah habis. Hapus dari keranjang.",
                    "Stok habis", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog(this, "Edit Pesanan", true);
        dialog.setSize(420, 260);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(20, 25, 20, 25));

        JLabel lblNama = new JLabel(menu.getNama());
        lblNama.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblNama.setForeground(COLOR_PRIMARY_DARK);
        JLabel lblInfo = new JLabel("Rp " + String.format("%,d", menu.getHarga()) + " per porsi · Stok " + stok);
        lblInfo.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblInfo.setForeground(COLOR_TEXT_MUTED);

        int[] qty = {Math.min(item.getJumlah(), stok)};
        JButton btnMinus = new JButton("-");
        JButton btnPlus = new JButton("+");
        styleCounterButton(btnMinus);
        styleCounterButton(btnPlus);
        JLabel lblQty = new JLabel(String.valueOf(qty[0]), SwingConstants.CENTER);
        lblQty.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblQty.setPreferredSize(new Dimension(34, 25));
        JLabel lblSubtotal = new JLabel("Rp " + String.format("%,d", menu.getHarga() * qty[0]));
        lblSubtotal.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblSubtotal.setForeground(COLOR_PRIMARY_DARK);

        btnMinus.addActionListener(e -> {
            if (qty[0] > 1) {
                qty[0]--;
                lblQty.setText(String.valueOf(qty[0]));
                lblSubtotal.setText("Rp " + String.format("%,d", menu.getHarga() * qty[0]));
            }
        });
        btnPlus.addActionListener(e -> {
            if (qty[0] >= stok) {
                JOptionPane.showMessageDialog(dialog, "Stok " + menu.getNama() + " hanya tersisa " + stok + " porsi.",
                        "Stok terbatas", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            qty[0]++;
            lblQty.setText(String.valueOf(qty[0]));
            lblSubtotal.setText("Rp " + String.format("%,d", menu.getHarga() * qty[0]));
        });

        JPanel barisJumlah = new JPanel(new BorderLayout());
        barisJumlah.setOpaque(false);
        JLabel lblJumlah = new JLabel("Jumlah porsi");
        lblJumlah.setFont(new Font("SansSerif", Font.PLAIN, 13));
        JPanel counter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        counter.setOpaque(false);
        counter.add(btnMinus);
        counter.add(lblQty);
        counter.add(btnPlus);
        barisJumlah.add(lblJumlah, BorderLayout.WEST);
        barisJumlah.add(counter, BorderLayout.EAST);

        JPanel barisSubtotal = new JPanel(new BorderLayout());
        barisSubtotal.setOpaque(false);
        JLabel lblSub = new JLabel("Subtotal");
        lblSub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        barisSubtotal.add(lblSub, BorderLayout.WEST);
        barisSubtotal.add(lblSubtotal, BorderLayout.EAST);

        JPanel aksi = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        aksi.setOpaque(false);
        JButton btnBatal = new JButton("Batal");
        styleButton(btnBatal, new Color(140, 130, 125), Color.WHITE);
        JButton btnSimpan = new JButton("Simpan perubahan");
        styleButton(btnSimpan, COLOR_TERRACOTTA, Color.WHITE);
        btnBatal.addActionListener(e -> dialog.dispose());
        btnSimpan.addActionListener(e -> {
            keranjang.ubahJumlah(index, qty[0]);
            lblBadgeKeranjang.setText(" " + keranjang.getTotalQty() + " ");
            dialog.dispose();
            updateKeranjangTampilan();
            updateRingkasanTampilan();
        });
        aksi.add(btnBatal);
        aksi.add(btnSimpan);

        panel.add(lblNama);
        panel.add(Box.createVerticalStrut(3));
        panel.add(lblInfo);
        panel.add(Box.createVerticalStrut(14));
        panel.add(barisJumlah);
        panel.add(Box.createVerticalStrut(12));
        panel.add(barisSubtotal);
        panel.add(Box.createVerticalStrut(18));
        panel.add(aksi);
        ratakiri(panel);

        dialog.add(panel);
        dialog.setVisible(true);
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

        JLabel lblNamaTitle = new JLabel("<html>Nama Pembeli <font color='#D32F2F'>*</font></html>");
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


        JLabel lblHpTitle = new JLabel("<html>Nomor Telepon <font color='#D32F2F'>*</font></html>");
        lblHpTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblHpTitle.setForeground(COLOR_PRIMARY_DARK);
        lblHpTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtHpPembeli = new JTextField();
        txtHpPembeli.setFont(new Font("SansSerif", Font.PLAIN, 13));
        txtHpPembeli.setPreferredSize(new Dimension(0, 38));
        txtHpPembeli.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        txtHpPembeli.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtHpPembeli.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 215, 205), 1),
                new EmptyBorder(5, 12, 5, 12)
        ));
        

        JLabel lblNamaHint = new JLabel("<html><font color='#D32F2F'>*</font> Wajib diisi.</html>");
        lblNamaHint.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblNamaHint.setForeground(COLOR_TEXT_MUTED);
        lblNamaHint.setAlignmentX(Component.LEFT_ALIGNMENT);

        cardForm.add(lblNamaTitle);
        cardForm.add(Box.createVerticalStrut(8));
        cardForm.add(txtNamaPembeli);
        cardForm.add(Box.createVerticalStrut(14));
        cardForm.add(lblHpTitle);
        cardForm.add(Box.createVerticalStrut(8));
        cardForm.add(txtHpPembeli);
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

            String hpInput = txtHpPembeli.getText().trim();
            if (hpInput.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nomor telepon wajib diisi.",
                        "Nomor telepon belum diisi", JOptionPane.WARNING_MESSAGE);
                txtHpPembeli.requestFocus();
                return;
            }
            if (!hpInput.matches("\\+?[0-9 .-]{8,18}")) {
                JOptionPane.showMessageDialog(this, "Nomor telepon tidak valid. Contoh: 081234567890",
                        "Nomor telepon", JOptionPane.WARNING_MESSAGE);
                txtHpPembeli.requestFocus();
                return;
            }

            if (keranjang.getListItems().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Pesanan kosong! Silakan pilih menu terlebih dahulu.");
                return;
            }

            int nomorBaru;
            try {
                nomorBaru = controller.nomorBerikutnya();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Penyimpanan pesanan tidak dapat dibaca: " + ex.getMessage(),
                        "Kesalahan penyimpanan", JOptionPane.ERROR_MESSAGE);
                return;
            }
            nomorPesananCount = nomorBaru + 1;
            IDPesananTerakhir = controller.formatId(nomorBaru);
            namaPembeliTerakhir = namaInput;
            hpPesananTerakhir = hpInput;
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
                Object[] pilihan = {"Oke", "Ubah Metode Pembayaran"};
                int jawaban = JOptionPane.showOptionDialog(this,
                        "Pembayaran dilakukan secara tunai di kasir kantin.",
                        "Konfirmasi Pembayaran Tunai", JOptionPane.DEFAULT_OPTION,
                        JOptionPane.INFORMATION_MESSAGE, null, pilihan, pilihan[0]);
                if (jawaban == 0) {
                    selesaikanPesanan();
                } else if (jawaban == 1) {
                    rbQRIS.setSelected(true);
                    panelPilihBankContainer.setVisible(false);
                }
            }
        });

        actionGroup.add(btnKonfirmasiBayar);
        actionGroup.add(btnKembali);

        cardForm.add(panelTotal);
        cardForm.add(Box.createVerticalStrut(20));
        cardForm.add(actionGroup);

        contentContainer.add(cardForm);
        ratakiri(contentContainer);

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

        JLabel lblKode = new JLabel("PESANAN " + kodePesanan, SwingConstants.LEFT);
        lblKode.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblKode.setForeground(COLOR_TERRACOTTA);

        JLabel lblJudul = new JLabel("Virtual Account " + bank, SwingConstants.LEFT);
        lblJudul.setFont(new Font("Serif", Font.BOLD, 22));
        lblJudul.setForeground(COLOR_PRIMARY_DARK);

        JLabel lblAtasNama = new JLabel("Atas nama " + namaPemesan, SwingConstants.LEFT);
        lblAtasNama.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lblAtasNama.setForeground(Color.GRAY);

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
        isiWarna(btnSalinVA);
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

        JButton btnKonfirmasi = new JButton("Setuju");
        styleButton(btnKonfirmasi, COLOR_TERRACOTTA, Color.WHITE);
        btnKonfirmasi.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnKonfirmasi.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnKonfirmasi.setMaximumSize(new Dimension(380, 42));

        btnKonfirmasi.addActionListener(e -> {
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
        ratakiri(mainPanel);

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

        JLabel lblKode = new JLabel("PESANAN " + kodePesanan, SwingConstants.LEFT);
        lblKode.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblKode.setForeground(COLOR_TERRACOTTA);

        JLabel lblJudul = new JLabel("Pembayaran QRIS", SwingConstants.LEFT);
        lblJudul.setFont(new Font("Serif", Font.BOLD, 22));
        lblJudul.setForeground(COLOR_PRIMARY_DARK);

        JLabel lblAtasNama = new JLabel("Atas nama " + namaPemesan, SwingConstants.LEFT);
        lblAtasNama.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lblAtasNama.setForeground(Color.GRAY);

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
            File kAppFile = new File("KantinKuApp/src/images/" + name);
            if (kAppFile.exists()) {
                iconQris = new ImageIcon(kAppFile.getAbsolutePath());
                break;
            }
            File kAppRootFile = new File("KantinKuApp/images/" + name);
            if (kAppRootFile.exists()) {
                iconQris = new ImageIcon(kAppRootFile.getAbsolutePath());
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

        JButton btnKonfirmasi = new JButton("Setuju");
        styleButton(btnKonfirmasi, COLOR_TERRACOTTA, Color.WHITE);
        btnKonfirmasi.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnKonfirmasi.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnKonfirmasi.setMaximumSize(new Dimension(360, 42));

        btnKonfirmasi.addActionListener(e -> {
            dialog.dispose();
            selesaikanPesanan();
        });

        mainPanel.add(lblKode);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        mainPanel.add(lblJudul);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        mainPanel.add(lblAtasNama);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        JPanel qrWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        qrWrap.setOpaque(false);
        qrWrap.add(lblGambar);
        mainPanel.add(qrWrap);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        mainPanel.add(panelTotal);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        mainPanel.add(btnKonfirmasi);
        ratakiri(mainPanel);

        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    private void selesaikanPesanan() {
        if (penyelesaianBerjalan || IDPesananTerakhir.isBlank()) return;
        if (keranjang.getListItems().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Keranjang kosong. Pesanan tidak dapat disimpan.",
                    "Pesanan kosong", JOptionPane.WARNING_MESSAGE);
            return;
        }
        penyelesaianBerjalan = true;
        // Stock and transaction data are committed together by the repository.
        Map<String, Integer> permintaan = new LinkedHashMap<>();
        for (ItemKeranjang item : keranjang.getListItems()) {
            permintaan.merge(item.getMenu().getId(), item.getJumlah(), Integer::sum);
        }
        Pesanan tersimpan;
        try {
            Pesanan pesanan = new Pesanan(
                    IDPesananTerakhir, namaPembeliTerakhir, metodePesananTerakhir,
                    totalPesananTerakhir, "Menunggu konfirmasi", rincianPesananTerakhir,
                    hpPesananTerakhir, controller.sekarang());
            List<models.DetailPesanan> details = new ArrayList<>();
            for (ItemKeranjang item : keranjang.getListItems()) {
                int subtotal = item.getMenu().getHarga() * item.getJumlah();
                details.add(new models.DetailPesanan(item.getMenu().getId(), item.getMenu().getNama(),
                        item.getJumlah(), item.getMenu().getHarga(), subtotal, ""));
            }
            pesanan.setDetails(details);
            tersimpan = controller.simpanPesanan(pesanan, permintaan);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Pesanan belum dapat disimpan: " + ex.getMessage(),
                    "Kesalahan penyimpanan", JOptionPane.ERROR_MESSAGE);
            muatKatalogBersama();
            updateGridMenu();
            tabbedPane.setSelectedIndex(2);
            penyelesaianBerjalan = false;
            return;
        }
        IDPesananTerakhir = tersimpan.id;
        pesananAktifPembeli = tersimpan;

        penandaPesananTerlihat = controller.penandaPesanan();
        keranjang.kosongkan();
        lblBadgeKeranjang.setText(" 0 ");
        muatKatalogBersama();
        updateGridMenu();

        updateKeranjangTampilan();
        updateRingkasanTampilan();
        updatePesananSayaTampilan();

        tabbedPane.setSelectedIndex(4);
        IDPesananTerakhir = "";
        penyelesaianBerjalan = false;
    }

    private void tampilkanStruk(Pesanan p) {
        JDialog dialog = new JDialog(this, "Struk Pembelian", true);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.setSize(470, 640);
        dialog.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(18, 22, 18, 22));

        JPanel atas = new JPanel();
        atas.setLayout(new BoxLayout(atas, BoxLayout.Y_AXIS));
        atas.setOpaque(false);
        JLabel lblJudul = new JLabel("Struk pesanan " + p.id);
        lblJudul.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblJudul.setForeground(COLOR_PRIMARY_DARK);
        JLabel lblSub = new JLabel("Cetak struk ini sebagai bukti pemesanan.");
        lblSub.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblSub.setForeground(COLOR_TEXT_MUTED);
        atas.add(lblJudul);
        atas.add(Box.createVerticalStrut(3));
        atas.add(lblSub);
        ratakiri(atas);

        JTextArea area = new JTextArea(controller.buatStruk(p));
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setBackground(new Color(252, 250, 246));
        area.setBorder(new EmptyBorder(14, 14, 14, 14));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(230, 220, 210), 1));

        JPanel tombol = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        tombol.setOpaque(false);
        JButton btnCetak = new JButton("Cetak");
        styleButton(btnCetak, COLOR_TERRACOTTA, Color.WHITE);
        btnCetak.addActionListener(e -> {
            try {
                if (!area.print()) return;
                dialog.dispose();
                JOptionPane.showMessageDialog(this, "Terima kasih, silakan menunggu pesanan siap.",
                        "KantinKu", JOptionPane.INFORMATION_MESSAGE);
                txtNamaPembeli.setText("");
                txtHpPembeli.setText("");
                pesananAktifPembeli = null;
                updatePesananSayaTampilan();
                tabbedPane.setSelectedIndex(0);
            } catch (java.awt.print.PrinterException ex) {
                JOptionPane.showMessageDialog(dialog, "Struk gagal dicetak: " + ex.getMessage(),
                        "Struk", JOptionPane.ERROR_MESSAGE);
            }
        });
        tombol.add(btnCetak);

        root.add(atas, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        root.add(tombol, BorderLayout.SOUTH);
        dialog.add(root);
        dialog.setVisible(true);
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

    /**
     * Pesanan milik pembeli yang tersimpan (nama sama = orang yang sama), terbaru lebih dulu.
     * Semua pesanan orang yang sama tetap tersimpan dan muncul kembali walau aplikasi ditutup lalu dibuka lagi.
     */
    private List<Pesanan> pesananSaya() {
        List<Pesanan> hasil = new ArrayList<>();
        try {
            if (!pembeliAktif.isBlank()) {
                return controller.pesananPembeli(pembeliAktif);
            }
            Map<String, Pesanan> semua = new LinkedHashMap<>();
            for (Pesanan p : controller.bacaSemuaPesanan()) semua.put(p.id, p);
            List<String> ids = controller.bacaIdPembeli();
            for (int i = ids.size() - 1; i >= 0; i--) {
                Pesanan p = semua.get(ids.get(i));
                if (p != null) hasil.add(p);
            }
        } catch (IOException ex) {
            System.err.println("Status pesanan belum dapat disinkronkan: " + ex.getMessage());
        }
        return hasil;
    }

    private JPanel buatBarisPesanan(Pesanan p) {
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

        JButton btnStruk = new JButton("Lihat struk");
        styleButton(btnStruk, new Color(245, 235, 225), COLOR_PRIMARY_DARK);
        btnStruk.setBorder(new EmptyBorder(4, 10, 4, 10));
        btnStruk.addActionListener(e -> tampilkanStruk(p));
        JPanel kanan = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        kanan.setOpaque(false);
        kanan.add(lblBadge);
        kanan.add(btnStruk);

        inner.add(lblID, BorderLayout.WEST);
        inner.add(kanan, BorderLayout.EAST);
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

    private JPanel buatDaftarPesanan(List<Pesanan> list) {
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

    private static String esc(String t) {
        return t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
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
        if (pesananAktifPembeli != null) {
            try {
                Pesanan terbaru = controller.cariPesanan(pesananAktifPembeli.id);
                if (terbaru != null) pesananAktifPembeli = terbaru;
            } catch (IOException ex) {
                System.err.println("Status pesanan belum dapat disinkronkan: " + ex.getMessage());
            }
        }

        panelPesananSayaContainer.removeAll();
        panelPesananSayaContainer.setBorder(new EmptyBorder(30, 60, 40, 60));

        JLabel title = new JLabel("Pesanan Saya");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(COLOR_PRIMARY_DARK);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel subtitle = new JLabel("Struk pesanan tersedia untuk dicetak setelah pembayaran.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(COLOR_TEXT_MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelPesananSayaContainer.add(title);
        panelPesananSayaContainer.add(Box.createVerticalStrut(4));
        panelPesananSayaContainer.add(subtitle);
        panelPesananSayaContainer.add(Box.createVerticalStrut(18));

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 220, 210), 1),
                new EmptyBorder(22, 24, 22, 24)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (pesananAktifPembeli == null) {
            JLabel empty = new JLabel("Belum ada pesanan untuk sesi ini.");
            empty.setFont(new Font("SansSerif", Font.PLAIN, 13));
            empty.setForeground(COLOR_TEXT_MUTED);
            card.add(empty);
        } else {
            Pesanan pesanan = pesananAktifPembeli;
            JLabel id = new JLabel("Nomor pesanan: " + pesanan.id);
            id.setFont(new Font("SansSerif", Font.BOLD, 15));
            id.setForeground(COLOR_PRIMARY_DARK);
            JLabel status = new JLabel("Status: " + pesanan.status);
            status.setFont(new Font("SansSerif", Font.PLAIN, 13));
            status.setForeground(COLOR_TERRACOTTA);
            card.add(id);
            card.add(Box.createVerticalStrut(8));
            card.add(status);
            card.add(Box.createVerticalStrut(10));
            for (String item : pesanan.rincian.split("\n")) {
                if (item.isBlank()) continue;
                JLabel detail = new JLabel(item);
                detail.setFont(new Font("SansSerif", Font.PLAIN, 12));
                detail.setForeground(COLOR_TEXT_MUTED);
                card.add(detail);
            }
            JLabel total = new JLabel("Total: Rp " + String.format("%,d", pesanan.total)
                    + "  ·  " + pesanan.metode);
            total.setFont(new Font("SansSerif", Font.BOLD, 13));
            total.setForeground(COLOR_PRIMARY_DARK);
            card.add(Box.createVerticalStrut(10));
            card.add(total);
            card.add(Box.createVerticalStrut(16));

            JButton printReceipt = new JButton("Cetak struk");
            styleButton(printReceipt, COLOR_TERRACOTTA, Color.WHITE);
            printReceipt.setAlignmentX(Component.LEFT_ALIGNMENT);
            printReceipt.addActionListener(event -> tampilkanStruk(pesananAktifPembeli));
            card.add(printReceipt);
        }
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
        panelPesananSayaContainer.add(card);
        ratakiri(panelPesananSayaContainer);
        panelPesananSayaContainer.revalidate();
        panelPesananSayaContainer.repaint();
    }

    private void updatePesananSayaTampilanLegacy() {
        if (panelPesananSayaContainer == null) return;
        List<Pesanan> daftarPesanan = pesananSaya();
        Pesanan terbaru = daftarPesanan.isEmpty() ? null : daftarPesanan.get(0);
        List<Pesanan> pesananAktif = new ArrayList<>();
        List<Pesanan> pesananSelesai = new ArrayList<>();
        for (Pesanan ps : daftarPesanan) {
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
        panelPesananSayaContainer.add(Box.createVerticalStrut(16));

        JPanel cardPembeli = new JPanel(new BorderLayout(12, 0));
        cardPembeli.setBackground(Color.WHITE);
        cardPembeli.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 220, 210), 1),
                new EmptyBorder(12, 20, 12, 20)));
        cardPembeli.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        JLabel lblPembeli = new JLabel(pembeliAktif.isBlank()
                ? "Data pembeli belum tersimpan. Akan tersimpan otomatis setelah kamu memesan."
                : "<html><b>Pembeli: " + esc(pembeliAktif) + "</b>"
                        + (hpPembeliAktif.isBlank() ? "" : " · HP " + esc(hpPembeliAktif))
                        + " · " + daftarPesanan.size() + " pesanan tersimpan</html>");
        lblPembeli.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblPembeli.setForeground(COLOR_PRIMARY_DARK);
        cardPembeli.add(lblPembeli, BorderLayout.CENTER);
        panelPesananSayaContainer.add(cardPembeli);
        panelPesananSayaContainer.add(Box.createVerticalStrut(16));

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
        if (terbaru != null) {
            final Pesanan pesananTerbaru = terbaru;
            JButton btnStrukTerbaru = new JButton("Lihat struk");
            styleButton(btnStrukTerbaru, COLOR_TERRACOTTA, Color.WHITE);
            btnStrukTerbaru.setFont(new Font("SansSerif", Font.BOLD, 12));
            btnStrukTerbaru.addActionListener(e -> tampilkanStruk(pesananTerbaru));
            statusRow.add(btnStrukTerbaru);
        }

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

        ratakiri(panelPesananSayaContainer);
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
        isiWarna(btn);
        btn.setPreferredSize(new Dimension(32, 32));
        btn.setBackground(new Color(245, 235, 225));
        btn.setForeground(Color.BLACK);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(new Color(230, 220, 210), 1));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    /** Look & feel Windows mengabaikan warna latar tombol, sehingga tombol tampil putih; UI dasar memperbaikinya. */
    private static void isiWarna(JButton btn) {
        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setBorderPainted(true);
    }

    private void styleButton(JButton btn, Color bg, Color fg) {
        isiWarna(btn);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
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
            "KantinKuApp/images/" + fileName,
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
}
