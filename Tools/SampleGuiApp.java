import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Font;
import java.time.LocalTime;

public class SampleGuiApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("沙盒示例 GUI");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setSize(420, 200);
            frame.setLocationRelativeTo(null);

            JLabel label = new JLabel("你好，这是沙盒独立弹窗示例。当前时间：" + LocalTime.now(), JLabel.CENTER);
            label.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
            frame.setLayout(new BorderLayout());
            frame.add(label, BorderLayout.CENTER);

            frame.setVisible(true);
        });
    }
}
