package perf;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created with IntelliJ IDEA.
 * User: sunhao
 * Date: 13-11-30
 * Time: 下午5:07
 */
public class AtomicIntegerTest {
    public static final AtomicInteger atomicInteger = new AtomicInteger(0);
    private static int THREAD_CNT = 16;//CAS与线程
    private static final int CNT = 1000000000;

    public static void main(String[] args) throws InterruptedException {
        Thread[] threads = new Thread[THREAD_CNT];
        for (int i = 0; i < THREAD_CNT; i++) {
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        if (atomicInteger.incrementAndGet() >= CNT) {
                            System.out.println(System.currentTimeMillis());
                            return;
                        }
                    }
                }
            });
        }

        System.out.println(System.currentTimeMillis());

        for (Thread thread : threads) {
            thread.start();
        }

        TimeUnit.SECONDS.sleep(1000);

        System.exit(1);
    }
}
