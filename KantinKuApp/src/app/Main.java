package app;

import config.DatabaseStartup;
import java.io.IOException;
import views.pembeli.KantinKuPembeliView;
import javax.swing.*;
import java.awt.*;

public class Main {
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
        SwingUtilities.invokeLater(() -> {
            UIManager.put("OptionPane.messageFont", new Font("SansSerif", Font.PLAIN, 14));
            KantinKuPembeliView app = new KantinKuPembeliView();
            app.setVisible(true);
        });
    }
}
