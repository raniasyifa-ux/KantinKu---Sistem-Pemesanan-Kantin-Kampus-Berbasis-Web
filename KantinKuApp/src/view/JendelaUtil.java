package view;

import java.awt.Frame;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;

/** Menutup aplikasi hanya ketika semua jendela KantinKu (pembeli, penjual, portal) sudah tertutup. */
public final class JendelaUtil {
    private JendelaUtil() {
    }

    public static void daftarkan(JFrame frame) {
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                javax.swing.SwingUtilities.invokeLater(JendelaUtil::keluarBilaKosong);
            }
        });
    }

    private static void keluarBilaKosong() {
        for (Frame f : Frame.getFrames()) {
            if (f.isDisplayable() && f.isVisible()) {
                return;
            }
        }
        for (Window w : Window.getWindows()) {
            if (w.isDisplayable() && w.isVisible()) {
                return;
            }
        }
        System.exit(0);
    }
}
