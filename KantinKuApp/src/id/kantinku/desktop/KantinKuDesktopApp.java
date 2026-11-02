package id.kantinku.desktop;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class KantinKuDesktopApp {
    private KantinKuDesktopApp() {
    }

    public static void main(String[] args) {
        launch();
    }

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                SellerDataStore dataStore = new SellerDataStore();
                new SellerLoginFrame(new SellerAuthService(), dataStore).setVisible(true);
            } catch (Exception exception) {
                exception.printStackTrace();
                javax.swing.JOptionPane.showMessageDialog(
                        null,
                        "Aplikasi tidak dapat dimulai: " + exception.getMessage(),
                        "KantinKu",
                        javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
