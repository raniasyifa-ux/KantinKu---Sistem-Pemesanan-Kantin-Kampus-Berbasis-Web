package views.penjual;

import controllers.PenjualController;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.io.IOException;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JToggleButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

@SuppressWarnings("serial")
public final class SellerLoginFrame extends JFrame {
    private static final Color PRIMARY = new Color(0xC8553D);
    private static final Color PRIMARY_HOVER = new Color(0xA8432F);
    private static final Color PAGE_BACKGROUND = new Color(0xF3E5D2);
    private static final Color TEXT = new Color(0x3B2A25);
    private static final Color MUTED = new Color(0x8A7B73);
    private static final Color BORDER = new Color(0xE8DDD0);

    private final PenjualController controller;
    private final CardLayout pageLayout = new CardLayout();
    private final JPanel pages = new JPanel(pageLayout);
    private final JButton loginButton = createPrimaryButton("Masuk sebagai penjual");
    private final JTextField usernameField = new PromptTextField("Username atau email penjual", 24);
    private final JPasswordField passwordField = new PromptPasswordField("Password", 24);
    private final JTextField registerUsernameField = new PromptTextField("Contoh: KantinBunda", 24);
    private final JTextField registerEmailField = new PromptTextField("toko@email.com", 24);
    private final JPasswordField registerPasswordField = new PromptPasswordField("Buat password", 24);
    private final JTextField resetEmailField = new PromptTextField("Email terdaftar", 24);
    private final JPasswordField resetPasswordField = new PromptPasswordField("Password baru", 24);
    private final JLabel statusMessage = new JLabel();
    private JPanel statusCard;
    private JButton registerButton;
    private JButton resetButton;

    public SellerLoginFrame(PenjualController controller) {
        super("Login Penjual | KantinKu");
        this.controller = controller;
        util.JendelaUtil.daftarkan(this);
        setMinimumSize(new Dimension(540, 640));
        setSize(700, 720);
        setLocationRelativeTo(null);
        setContentPane(createContent());
        getRootPane().setDefaultButton(loginButton);
    }

    private JPanel createContent() {
        JPanel page = new JPanel(new GridBagLayout());
        page.setBackground(PAGE_BACKGROUND);

        JPanel card = new JPanel(new BorderLayout(0, 25)) {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                return new Dimension(Math.max(420, d.width), Math.max(490, d.height));
            }

            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(Color.WHITE);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g.setColor(BORDER);
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(40, 35, 30, 35));

        pages.setOpaque(false);
        pages.add(createLoginPage(), "login");
        pages.add(createRegisterPage(), "register");
        pages.add(createResetPage(), "reset");
        statusMessage.setOpaque(true);
        statusMessage.setForeground(TEXT);
        statusMessage.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        statusMessage.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 10));
        statusMessage.setVisible(false);
        statusCard = card;
        card.add(statusMessage, BorderLayout.NORTH);
        card.add(pages, BorderLayout.CENTER);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets = new Insets(20, 20, 20, 20);
        page.add(card, constraints);
        return page;
    }

    private JPanel createLoginPage() {
        JPanel content = createAuthPage("Masuk ke toko", "Masuk ke akun penjual KantinKu");
        JPanel form = createFormPanel();
        addField(form, 0, "Username / Email", usernameField);
        addField(form, 2, "Password", passwordControl(passwordField));

        loginButton.addActionListener(event -> authenticate());
        JPanel formOptions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        formOptions.setOpaque(false);
        JButton forgot = createLink("Lupa Password?");
        forgot.addActionListener(event -> showPage("reset", resetButton));
        formOptions.add(forgot);
        addFormComponent(form, 4, formOptions);
        addFormComponent(form, 5, loginButton);

        JPanel links = new JPanel(new GridBagLayout());
        links.setOpaque(false);
        addSwitchText(links, 0, "Belum punya akun toko? ", "Daftarkan toko",
                () -> showPage("register", registerButton));
        content.add(form, BorderLayout.CENTER);
        content.add(links, BorderLayout.SOUTH);
        return content;
    }

    private JPanel createRegisterPage() {
        JPanel content = createAuthPage("Daftarkan toko", "Buat akun penjual untuk mulai menerima pesanan.");
        JPanel form = createFormPanel();
        addField(form, 0, "Nama toko / Username (4-12 karakter)", registerUsernameField);
        addField(form, 2, "Email toko", registerEmailField);
        addField(form, 4, "Password (min. 8 karakter, huruf dan angka)",
                passwordControl(registerPasswordField));

        registerButton = createPrimaryButton("Daftar sebagai penjual");
        registerButton.addActionListener(event -> registerAccount());
        addFormComponent(form, 6, registerButton);

        JPanel links = new JPanel(new GridBagLayout());
        links.setOpaque(false);
        addSwitchText(links, 0, "Sudah punya akun toko? ", "Masuk di sini",
                () -> showPage("login", loginButton));
        content.add(form, BorderLayout.CENTER);
        content.add(links, BorderLayout.SOUTH);
        return content;
    }

    private JPanel createResetPage() {
        JPanel content = createAuthPage("Atur ulang password toko", "Masukkan email akun penjual dan password baru.");
        JPanel form = createFormPanel();
        addField(form, 0, "Email toko", resetEmailField);
        addField(form, 2, "Password baru", passwordControl(resetPasswordField));

        resetButton = createPrimaryButton("Atur ulang password");
        resetButton.addActionListener(event -> resetPassword());
        addFormComponent(form, 4, resetButton);

        JPanel links = new JPanel(new GridBagLayout());
        links.setOpaque(false);
        addLink(links, 0, "Kembali ke login toko", () -> showPage("login", loginButton));
        content.add(form, BorderLayout.CENTER);
        content.add(links, BorderLayout.SOUTH);
        return content;
    }

    private JPanel createAuthPage(String titleText, String subtitleText) {
        JPanel content = new JPanel(new BorderLayout(0, 25));
        content.setOpaque(false);
        JPanel heading = new JPanel(new GridBagLayout());
        heading.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.CENTER;
        JLabel title = new JLabel(titleText, SwingConstants.CENTER);
        title.setForeground(TEXT);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        heading.add(title, constraints);
        constraints.gridy = 1;
        constraints.insets = new Insets(6, 0, 0, 0);
        JLabel subtitle = new JLabel(subtitleText, SwingConstants.CENTER);
        subtitle.setForeground(MUTED);
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        heading.add(subtitle, constraints);
        content.add(heading, BorderLayout.NORTH);
        return content;
    }

    private static JPanel createFormPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        return form;
    }

    private static void addField(JPanel form, int row, String labelText, java.awt.Component field) {
        JLabel label = new JLabel(labelText);
        label.setForeground(TEXT);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(row == 0 ? 0 : 18, 0, 6, 0);
        form.add(label, constraints);
        if (field instanceof JTextField) {
            styleInput((JTextField) field);
        } else {
            field.setPreferredSize(new Dimension(280, 44));
        }
        constraints.gridy = row + 1;
        constraints.insets = new Insets(0, 0, 0, 0);
        form.add(field, constraints);
    }

    private static void styleInput(JTextField field) {
        field.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        field.setPreferredSize(new Dimension(280, 44));
        field.setBackground(new Color(0xF8FAFC));
        field.setForeground(TEXT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE2E8F0), 2),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
    }

    private static JPanel passwordControl(JPasswordField field) {
        styleInput(field);
        char echoChar = field.getEchoChar();
        field.setPreferredSize(new Dimension(200, 44));
        JPanel control = new JPanel(new BorderLayout(8, 0));
        control.setOpaque(false);
        JToggleButton toggle = new JToggleButton("Lihat");
        toggle.setToolTipText("Tampilkan atau sembunyikan password");
        toggle.getAccessibleContext().setAccessibleName("Lihat atau sembunyikan password");
        toggle.addActionListener(event -> {
            field.setEchoChar(toggle.isSelected() ? (char) 0 : echoChar);
            toggle.setText(toggle.isSelected() ? "Sembunyi" : "Lihat");
        });
        control.add(field, BorderLayout.CENTER);
        control.add(toggle, BorderLayout.EAST);
        return control;
    }

    private static void addFormComponent(JPanel form, int row, java.awt.Component component) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 0, 0);
        form.add(component, constraints);
    }

    private static void addLink(JPanel links, int row, String text, Runnable action) {
        JButton link = createLink(text);
        link.addActionListener(event -> action.run());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.insets = new Insets(row == 0 ? 22 : 2, 0, 0, 0);
        links.add(link, constraints);
    }

    private static void addSwitchText(JPanel links, int row, String prefix, String linkText, Runnable action) {
        JPanel line = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        line.setOpaque(false);
        JLabel caption = new JLabel(prefix);
        caption.setForeground(MUTED);
        caption.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        line.add(caption);
        JButton link = createLink(linkText);
        link.addActionListener(event -> action.run());
        line.add(link);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.insets = new Insets(row == 0 ? 22 : 2, 0, 0, 0);
        links.add(line, constraints);
    }

    private static JButton createLink(String text) {
        JButton link = new JButton(text);
        link.setForeground(PRIMARY);
        link.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        link.setBackground(Color.WHITE);
        link.setBorderPainted(false);
        link.setContentAreaFilled(false);
        link.setFocusPainted(false);
        link.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        return link;
    }

    /** Ukuran kartu dihitung ulang saat banner pesan muncul/hilang supaya isi tidak terpotong. */
    private void relayoutCard() {
        if (statusCard != null) {
            statusCard.revalidate();
            Container parent = statusCard.getParent();
            if (parent != null) parent.revalidate();
            statusCard.repaint();
        }
    }

    private void showPage(String page, JButton defaultButton) {
        statusMessage.setVisible(false);
        relayoutCard();
        pageLayout.show(pages, page);
        getRootPane().setDefaultButton(defaultButton);
    }

    private static JButton createPrimaryButton(String text) {
        JButton button = new JButton(text);
        button.setUI(new BasicButtonUI());
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setBackground(PRIMARY);
        button.setForeground(Color.WHITE);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(280, 48));
        button.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));
        button.getModel().addChangeListener(event -> button.setBackground(
                button.getModel().isRollover() ? PRIMARY_HOVER : PRIMARY));
        return button;
    }

    private void authenticate() {
        String username = usernameField.getText().trim();
        char[] password = passwordField.getPassword();
        if (username.isEmpty()) {
            Arrays.fill(password, '\0');
            usernameField.requestFocusInWindow();
            showMessage("Username/email wajib diisi.", "Input belum lengkap",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (password.length == 0) {
            Arrays.fill(password, '\0');
            passwordField.requestFocusInWindow();
            showMessage("Password wajib diisi.", "Input belum lengkap",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            String accountName = controller.authenticate(username, password);
            if (accountName == null) {
                showMessage("Username/email atau password tidak cocok.", "Login gagal",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            new SellerDashboardFrame(controller, accountName).setVisible(true);
            dispose();
        } catch (IOException exception) {
            showStorageError(exception);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private void registerAccount() {
        char[] password = registerPasswordField.getPassword();
        try {
            String username = registerUsernameField.getText().trim();
            String email = registerEmailField.getText().trim();
            if (username.isEmpty()) {
                registerUsernameField.requestFocusInWindow();
                showMessage("Nama toko / username wajib diisi.", "Input belum lengkap",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (email.isEmpty()) {
                registerEmailField.requestFocusInWindow();
                showMessage("Email toko wajib diisi.", "Input belum lengkap",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (password.length == 0) {
                registerPasswordField.requestFocusInWindow();
                showMessage("Password wajib diisi.", "Input belum lengkap",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            controller.register(username, email, password);
            registerUsernameField.setText("");
            registerEmailField.setText("");
            registerPasswordField.setText("");
            showPage("login", loginButton);
            showMessage("Akun penjual desktop berhasil dibuat. Silakan masuk.", "Pendaftaran berhasil",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException exception) {
            showMessage(exception.getMessage(), "Pendaftaran gagal", JOptionPane.WARNING_MESSAGE);
        } catch (IOException exception) {
            showStorageError(exception);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private void resetPassword() {
        char[] password = resetPasswordField.getPassword();
        try {
            String email = resetEmailField.getText().trim();
            if (email.isEmpty()) {
                resetEmailField.requestFocusInWindow();
                showMessage("Email terdaftar wajib diisi.", "Input belum lengkap",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (password.length == 0) {
                resetPasswordField.requestFocusInWindow();
                showMessage("Password baru wajib diisi.", "Input belum lengkap",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            controller.resetPassword(email, password);
            resetEmailField.setText("");
            resetPasswordField.setText("");
            showPage("login", loginButton);
            showMessage("Password berhasil diubah. Silakan masuk kembali.", "Password diperbarui",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException exception) {
            showMessage(exception.getMessage(), "Reset password gagal", JOptionPane.WARNING_MESSAGE);
        } catch (IOException exception) {
            showStorageError(exception);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private void showStorageError(IOException exception) {
        showMessage("Data akun tidak dapat disimpan atau dibaca: " + exception.getMessage(),
                "Kesalahan penyimpanan", JOptionPane.ERROR_MESSAGE);
    }

    private void showMessage(String message, String title, int messageType) {
        boolean error = messageType == JOptionPane.ERROR_MESSAGE || messageType == JOptionPane.WARNING_MESSAGE;
        Color accent = error
                ? new Color(0xB44C4C)
                : new Color(0x4D8054);
        statusMessage.setText("<html>" + message + "</html>");
        statusMessage.setBackground(error
                ? new Color(0xFFF0EF)
                : new Color(0xF0F7EF));
        statusMessage.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
                BorderFactory.createEmptyBorder(12, 10, 12, 10)));
        statusMessage.setVisible(true);
        relayoutCard();
        revalidate();
        repaint();
    }

    private static final class PromptTextField extends JTextField {
        private final String prompt;

        private PromptTextField(String prompt, int columns) {
            super(columns);
            this.prompt = prompt;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (getText().isEmpty()) {
                paintPrompt(graphics, prompt, getInsets().left, getHeight() / 2 + getFontMetrics(getFont()).getAscent() / 2 - 2);
            }
        }
    }

    private static final class PromptPasswordField extends JPasswordField {
        private final String prompt;

        private PromptPasswordField(String prompt, int columns) {
            super(columns);
            this.prompt = prompt;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (getPassword().length == 0) {
                paintPrompt(graphics, prompt, getInsets().left, getHeight() / 2 + getFontMetrics(getFont()).getAscent() / 2 - 2);
            }
        }
    }

    private static void paintPrompt(Graphics graphics, String prompt, int x, int baseline) {
        graphics.setColor(new Color(0x8A7B73));
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        graphics.drawString(prompt, x, baseline);
    }
}
