package views.antri;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import models.Pesanan;
import controllers.AntrianController;
import util.JendelaUtil;

@SuppressWarnings("serial")
public final class AntrianFrame extends JFrame {
    private static final Color BACKGROUND = new Color(0xF3E5D2);
    private static final Color TEXT = new Color(0x3B2A25);
    private static final Color MUTED = new Color(0x8A7B73);
    private static final Color BORDER = new Color(0xE8DDD0);
    private static final Color PRIMARY = new Color(0xC8553D);
    private static final Color SAGE = new Color(0x3F5748);

    private final JPanel results = new JPanel();
    private final JLabel count = new JLabel("Memuat antrian...");
    private final Timer refreshTimer;
    private final AntrianController controller;
    private long lastOrderMarker = Long.MIN_VALUE;

    public AntrianFrame() {
        this(new AntrianController());
    }

    public AntrianFrame(AntrianController controller) {
        super("Antrian Pesanan - KantinKu");
        this.controller = controller;
        JendelaUtil.daftarkan(this);
        setMinimumSize(new Dimension(760, 520));
        setSize(980, 720);
        setLocationRelativeTo(null);

        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(BACKGROUND);
        page.add(createHeader(), BorderLayout.NORTH);

        results.setLayout(new BoxLayout(results, BoxLayout.Y_AXIS));
        results.setBackground(BACKGROUND);
        results.setBorder(new EmptyBorder(18, 30, 24, 30));
        JScrollPane scroll = new JScrollPane(results);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(BACKGROUND);
        page.add(scroll, BorderLayout.CENTER);
        setContentPane(page);

        refreshQueue();
        refreshTimer = new Timer(1500, event -> {
            long marker = controller.penandaPesanan();
            if (marker != lastOrderMarker) {
                refreshQueue();
            }
        });
        refreshTimer.start();
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent event) {
                refreshTimer.stop();
            }
        });
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 8));
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(20, 30, 18, 30)));

        JPanel titleGroup = new JPanel();
        titleGroup.setOpaque(false);
        titleGroup.setLayout(new BoxLayout(titleGroup, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Antrian Pesanan");
        title.setForeground(TEXT);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 25));
        JLabel subtitle = new JLabel("Progress seluruh pesanan yang masih aktif");
        subtitle.setForeground(MUTED);
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        titleGroup.add(title);
        titleGroup.add(Box.createVerticalStrut(4));
        titleGroup.add(subtitle);
        header.add(titleGroup, BorderLayout.WEST);

        count.setForeground(SAGE);
        count.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        count.setHorizontalAlignment(SwingConstants.RIGHT);
        header.add(count, BorderLayout.EAST);
        return header;
    }

    private void refreshQueue() {
        try {
            List<Pesanan> all = controller.bacaPesanan();
            lastOrderMarker = controller.penandaPesanan();
            List<Pesanan> active = new ArrayList<>();
            for (Pesanan pesanan : all) {
                if (isActive(pesanan.status)) {
                    active.add(pesanan);
                }
            }

            results.removeAll();
            count.setText(active.size() + " pesanan aktif");
            if (active.isEmpty()) {
                JLabel empty = new JLabel("Belum ada antrian aktif.", SwingConstants.CENTER);
                empty.setForeground(MUTED);
                empty.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
                empty.setBorder(new EmptyBorder(30, 4, 30, 4));
                empty.setAlignmentX(Component.LEFT_ALIGNMENT);
                results.add(empty);
            } else {
                for (Pesanan pesanan : active) {
                    results.add(createOrderCard(pesanan));
                    results.add(Box.createVerticalStrut(12));
                }
            }
        } catch (IOException exception) {
            results.removeAll();
            JLabel error = new JLabel("Antrian tidak dapat dibaca: " + exception.getMessage());
            error.setForeground(PRIMARY);
            error.setBorder(new EmptyBorder(20, 4, 20, 4));
            results.add(error);
            count.setText("Gagal memuat antrian");
        }
        results.revalidate();
        results.repaint();
    }

    private JPanel createOrderCard(Pesanan pesanan) {
        JPanel card = new JPanel(new BorderLayout(14, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(15, 17, 15, 17)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 170));

        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(pesanan.id + "  ·  " + pesanan.nama);
        title.setForeground(TEXT);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        JLabel items = new JLabel("<html>" + escapeHtml(pesanan.rincian).replace("\n", "<br>") + "</html>");
        items.setForeground(MUTED);
        items.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        JLabel time = new JLabel("Dipesan: " + pesanan.waktu);
        time.setForeground(MUTED);
        time.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        details.add(title);
        details.add(Box.createVerticalStrut(6));
        details.add(items);
        details.add(Box.createVerticalStrut(6));
        details.add(time);
        card.add(details, BorderLayout.CENTER);

        JPanel progressPanel = new JPanel();
        progressPanel.setOpaque(false);
        progressPanel.setLayout(new BoxLayout(progressPanel, BoxLayout.Y_AXIS));
        JLabel status = new JLabel(pesanan.status);
        status.setForeground(PRIMARY);
        status.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        status.setAlignmentX(Component.RIGHT_ALIGNMENT);
        JProgressBar progress = new JProgressBar(0, 3);
        progress.setValue(progressValue(pesanan.status));
        progress.setStringPainted(true);
        progress.setString("Progress");
        progress.setForeground(SAGE);
        progress.setPreferredSize(new Dimension(170, 20));
        progress.setMaximumSize(new Dimension(190, 20));
        progressPanel.add(status);
        progressPanel.add(Box.createVerticalStrut(10));
        progressPanel.add(progress);
        card.add(progressPanel, BorderLayout.EAST);
        return card;
    }

    private static boolean isActive(String status) {
        return status.equalsIgnoreCase("Menunggu konfirmasi")
                || status.equalsIgnoreCase("Diproses")
                || status.equalsIgnoreCase("Siap Diambil");
    }

    private static int progressValue(String status) {
        if (status.equalsIgnoreCase("Menunggu konfirmasi")) {
            return 1;
        }
        if (status.equalsIgnoreCase("Diproses")) {
            return 2;
        }
        return 3;
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
