package top.kwseeker.async.future.car;

public class TimeUtil {

    public static void sleep(int ms) {
        try {
            Thread.sleep(ms);
            //int cycle = Math.floorDiv(ms, 10);
            //for (int i = 0; i < cycle; i++) {
            //    System.out.print(".");
            //    Thread.sleep(10);
            //}
            //System.out.println();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
