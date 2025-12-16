import java.util.Timer;
import java.util.TimerTask;

public class Reminder {
    public static void main(String[] args) {
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                System.out.println("时间到，别忘了吃饭！");
            }
        };

        // 设置每天的午餐时间为12点提醒
        long delay = 0; // 延迟时间，单位毫秒
        long period = 24 * 60 * 60 * 1000; // 每天执行一次，单位毫秒
        timer.scheduleAtFixedRate(task, delay, period);
    }
}