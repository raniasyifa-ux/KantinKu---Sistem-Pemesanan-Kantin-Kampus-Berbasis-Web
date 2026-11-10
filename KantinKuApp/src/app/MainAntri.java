package app;

import config.DatabaseStartup;
import controllers.AntrianController;
import java.io.IOException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import views.antri.AntrianFrame;

public final class MainAntri {
    private MainAntri() {
    }

    public static void main(String[] args) {
        try {
            DatabaseStartup.initialize();
        } catch (IOException exception) {
            exception.printStackTrace();
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                    "Aplikasi tidak dapat terhubung ke MySQL: " + exception.getMessage(),
                    "KantinKu - Koneksi Database", JOptionPane.ERROR_MESSAGE));
            return;
        }
        SwingUtilities.invokeLater(() -> new AntrianFrame(new AntrianController()).setVisible(true));
    }
}
