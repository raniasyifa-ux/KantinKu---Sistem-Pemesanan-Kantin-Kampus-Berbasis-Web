package app;

import config.DatabaseStartup;
import java.io.IOException;
import controllers.PenjualController;
import services.SellerDataStore;
import services.SellerAuthService;
import views.penjual.SellerLoginFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class KantinKuDesktopApp {
    private KantinKuDesktopApp() {
    }

    public static void main(String[] args) {
        launch();
    }

    public static void launch() {
        try {
            DatabaseStartup.initialize();
        } catch (IOException exception) {
            exception.printStackTrace();
            SwingUtilities.invokeLater(() -> javax.swing.JOptionPane.showMessageDialog(
                    null,
                    "Aplikasi tidak dapat terhubung ke MySQL: " + exception.getMessage(),
                    "KantinKu - Koneksi Database",
                    javax.swing.JOptionPane.ERROR_MESSAGE));
            return;
        }
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                SellerDataStore dataStore = new SellerDataStore();
                PenjualController controller = new PenjualController(new SellerAuthService(), dataStore);
                new SellerLoginFrame(controller).setVisible(true);
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
