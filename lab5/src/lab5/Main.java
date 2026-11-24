package lab5;

import java.awt.EventQueue;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.swing.UIManager;

import lab5.view.MainFrame;

/** Точка входа. Все операции Swing создаются в потоке EDT. */
public final class Main {
    private Main() { }

    public static void main(String[] args) {
        final List<Path> files = new ArrayList<Path>();
        for (String file : args) files.add(Paths.get(file));
        EventQueue.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) { /* При ошибке оформления применяется стандартная тема. */ }
            MainFrame frame = new MainFrame(files);
            frame.setVisible(true);
        });
    }
}
