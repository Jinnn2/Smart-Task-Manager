public class SampleTimer {
    public static void main(String[] args) throws Exception {
        System.out.println("SampleTimer 启动，演示沙盒运行 jar。");
        for (int i = 5; i >= 1; i--) {
            System.out.println("倒计时: " + i + " 秒");
            Thread.sleep(1000);
        }
        System.out.println("计时结束，沙盒运行成功。");
    }
}
