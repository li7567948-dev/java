package lab5.tests;

import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import lab5.view.GraphPanel;
import lab5.view.MainFrame;

/** Проверка настоящего окна Swing (запускать с графическим дисплеем). */
public final class GuiSmokeTest {
    private GuiSmokeTest() { }

    public static void main(String[] args) throws Exception {
        final MainFrame[] frame = new MainFrame[1];
        SwingUtilities.invokeAndWait(() -> {
            frame[0] = new MainFrame(Collections.emptyList());
            frame[0].setVisible(true);
        });
        if (countGraphs(frame[0]) != 4) throw new AssertionError("Ожидались четыре графика Java2D");
        Thread.sleep(800);
        Rectangle bounds = frame[0].getBounds();
        Robot robot = new Robot();
        BufferedImage screenshot = robot.createScreenCapture(bounds);
        Path output = args.length > 0 ? Paths.get(args[0]) : Paths.get("docs", "gui_smoke.png");
        ImageIO.write(screenshot, "png", output.toFile());
        SwingUtilities.invokeAndWait(frame[0]::dispose);
        System.out.println("Окно создано; четыре графика обнаружены. Снимок: " + output);
    }

    private static int countGraphs(Component component) {
        int result = component instanceof GraphPanel ? 1 : 0;
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                result += countGraphs(child);
            }
        }
        return result;
    }
}
