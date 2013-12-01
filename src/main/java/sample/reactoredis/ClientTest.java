package sample.reactoredis;

import reactor.core.Reactor;
import reactor.event.Event;
import reactor.event.dispatch.RingBufferDispatcher;
import reactoredis.*;
import redis.clients.jedis.Jedis;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created with IntelliJ IDEA.
 * User: sunhao
 * Date: 13-11-30
 * Time: 下午2:20
 */
public class ClientTest {
    private static byte[] DATA = "USER_INFO_12345678901234567890123456789021".getBytes();

    private static final int TOTAL = 1000000;

    private static long time1;

    private static final double STATISTICS_RATE = 0.95;


    static class ssss {
        public int cnt = 0;
    }

    final static Map<String, ssss> local = new ConcurrentHashMap<String, ssss>();
    static ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    public static void main(String[] args) throws InterruptedException {
        scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                int sum = 0;
                for (Map.Entry<String, ssss> entry : local.entrySet()) {
                    sum += entry.getValue().cnt;
                }
                if (sum >= (TOTAL * STATISTICS_RATE)) {//estimate, not exact
                    long time2 = System.currentTimeMillis();
                    long interval = time2 - time1;
                    System.out.println(interval);
                    System.out.println((TOTAL * STATISTICS_RATE / ((double) interval) * 1000) + " requests per second");
                    System.out.println("statistic end: " + time2);
                    scheduledExecutorService.shutdown();
                }
            }
        }, 0, 100, TimeUnit.MILLISECONDS);

        ExecutorService executorService = Executors.newCachedThreadPool();
        final RedisClient redisClient = new RedisClient(
                new InetSocketAddress("192.168.48.134", 6379),
                4,
                new ReactorFactory() {
                    private AtomicInteger idx = new AtomicInteger(0);

                    @Override
                    public Reactor buildReactor() {
                        return new Reactor(new RingBufferDispatcher("client-queue-" + idx.incrementAndGet()));
                    }
                },
                executorService,
                new ConsumerStrategy() {
                    @Override
                    public void consumeEvent(final Event<RedisRequest> event, final Jedis jedis, final ExecutorService executorService) {
                        RedisRequest redisRequest = event.getData();
                        byte[] result = redisRequest.getCmd().execute(jedis, redisRequest.getData());
                        redisRequest.setResult(result);
                        executorService.submit(redisRequest.getCallback());
                    }
                }
        );

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < TOTAL; i++) {
                    redisClient.request(new RedisRequest() {
                        @Override
                        public RedisCommand getCmd() {
                            return RedisCommand.GET;
                        }

                        @Override
                        public byte[] getData() {
                            return DATA;
                        }

                        @Override
                        public RedisCallback getCallback() {
                            return new DemoCallback(this);
                        }

                        private byte[] result;

                        @Override
                        public byte[] getResult() {
                            return this.result;
                        }

                        @Override
                        public void setResult(byte[] result) {
                            this.result = result;
                        }
                    });
                }
                System.out.println("send done: " + System.currentTimeMillis());
            }
        });

        time1 = System.currentTimeMillis();
        System.out.println("start: " + time1);

        thread.start();

        TimeUnit.SECONDS.sleep(100);

        System.out.println("Timeout. interrupted.");

        System.exit(1);

    }

    static class DemoCallback implements RedisCallback {
        private RedisRequest redisRequest;

        public DemoCallback(RedisRequest redisRequest) {
            this.redisRequest = redisRequest;
        }

        @Override
        public void run() {
            //old statistic
//            if (atomicInteger.incrementAndGet() == TOTAL) {
//                redisRequest.getResult();
//                long time2 = System.currentTimeMillis();
//                System.out.println(time2);
//                long interval = time2 - time1;
//                System.out.println(interval);
//                System.out.println(TOTAL / ((double) interval));
//            }
            ssss s = local.get(Thread.currentThread().getName());
            if (s != null) {
                s.cnt++;
            } else {
                s = new ssss();
                local.put(Thread.currentThread().getName(), s);
            }
        }
    }
}
