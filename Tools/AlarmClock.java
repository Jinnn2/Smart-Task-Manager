import java.util.Timer;
import java.util.TimerTask;

public class AlarmClock {
    public static void main(String[] args) {
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            public void run() {
                System.out.println("时间到！请注意！");
                timer.cancel(); // 结束定时器
            }
        };
        
        // 设定10分钟后执行任务
        timer.schedule(task, 10 * 60 * 1000);
        System.out.println("闹钟已设置为10分钟后响铃。");
    }
}