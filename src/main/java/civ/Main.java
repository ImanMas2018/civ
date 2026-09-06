package civ;

import civ.view.MainWindow;
import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {
        boolean clientOnly = args.length > 0 && "client".equalsIgnoreCase(args[0]);
        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.setVisible(true);
            if (clientOnly) {
                window.showConnect(true);
            }
        });
    }
}
