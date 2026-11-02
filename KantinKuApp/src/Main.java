import view.KantinKuPembeliView;
import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UIManager.put("OptionPane.messageFont", new Font("SansSerif", Font.PLAIN, 14));
            Object[] options = {"Pembeli", "Penjual", "Keduanya"};
            int choice = JOptionPane.showOptionDialog(
                null,
                "Selamat datang di KantinKu. Pilih halaman yang ingin dibuka.\n"
                        + "Pilih \"Keduanya\" untuk membuka pembeli dan penjual sekaligus.",
                "KantinKu",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
            );
            if (choice == 0 || choice == 2) {
                KantinKuPembeliView app = new KantinKuPembeliView();
                app.setVisible(true);
            }
            if (choice == 1 || choice == 2) {
                id.kantinku.desktop.KantinKuDesktopApp.launch();
            }
        });
    }
}
