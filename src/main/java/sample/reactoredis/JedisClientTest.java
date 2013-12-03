package sample.reactoredis;

import reactor.core.Reactor;
import reactor.event.Event;
import reactor.event.dispatch.RingBufferDispatcher;
import reactoredis.async.Callback;
import reactoredis.core.*;
import reactoredis.redis.RedisCallback;
import reactoredis.redis.jedis.AsyncJedisRedisClient;
import reactoredis.redis.jedis.JedisRequestTypes;
import redis.clients.jedis.Jedis;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created with IntelliJ IDEA.
 * User: sunhao
 * Date: 13-12-3
 * Time: 下午1:46
 */
public class JedisClientTest {
    private static byte[] DATA = "USER_INFO_12345678901234567890123456789021".getBytes();

    private static final int TOTAL = 500000;

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

        final InetSocketAddress address = new InetSocketAddress("192.168.1.246", 6379);

        //build client
        final AsyncJedisRedisClient client = new AsyncJedisRedisClient(
                8,
                new GeneralStrategy<Jedis, JedisRequestTypes, byte[], byte[]>() {
                    private int idx = 0;

                    @Override
                    public int getReactorHelperCnt() {
                        return 1;
                    }

                    @Override
                    public Jedis decideHelperToBeUsed(Request<JedisRequestTypes, byte[], byte[]> request, Jedis[] helpers) {
                        return helpers[0];
                    }

                    @Override
                    public String getEventId() {
                        return "redis";
                    }

                    @Override
                    public int genNextIdxOfReactorToUse(int wildcard) {
                        return (idx++) & wildcard;
                    }
                },
                new ReactorFactory() {
                    private AtomicInteger idx = new AtomicInteger(0);

                    @Override
                    public Reactor buildReactor() {
                        return new Reactor(new RingBufferDispatcher("client-queue-" + idx.incrementAndGet()));
                    }
                },
                executorService,
                new ConsumerStrategy<JedisRequestTypes, Jedis, byte[], byte[]>() {
                    @Override
                    public void consumeEvent(Event<Request<JedisRequestTypes, byte[], byte[]>> event, Jedis helper, ExecutorService executorService) {
                        Request<JedisRequestTypes, byte[], byte[]> request = event.getData();
                        byte[] result = request.getRequestTypeIdentifier().executeCommand(helper, request.getRequestData());
                        request.setResult(result);
                        executorService.submit(request.getCallback());
                    }
                },
                new HelperBuilder<Jedis>() {
                    @Override
                    public Jedis build() {
                        return new Jedis(address.getHostName(), address.getPort());
                    }

                    @Override
                    public Jedis[] build(int cnt) {
                        Jedis[] jedises = new Jedis[cnt];
                        for (int i = 0; i < cnt; i++) {
                            jedises[i] = new Jedis(address.getHostName(), address.getPort());
                        }
                        return jedises;
                    }
                }
        );

        //build send thread
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < TOTAL; i++) {
                    client.request(new Request<JedisRequestTypes, byte[], byte[]>() {
                        private byte[] result;
                        @Override
                        public JedisRequestTypes getRequestTypeIdentifier() {
                            return JedisRequestTypes.GET;
                        }
                        @Override
                        public byte[] getRequestData() {
                            return DATA;
                        }
                        @Override
                        public byte[] getResult() {
                            return result;
                        }
                        @Override
                        public Request setResult(byte[] result) {
                            this.result = result;
                            return this;
                        }
                        @Override
                        public Callback getCallback() {
                            return new RedisCallback<JedisRequestTypes, byte[], byte[]>(this) {
                                @Override
                                public void run() {
                                    ssss s = local.get(Thread.currentThread().getName());
                                    if (s != null) {
                                        s.cnt++;
                                    } else {
                                        s = new ssss();
                                        local.put(Thread.currentThread().getName(), s);
                                    }
//                                    this.getRequest().getResult();
                                }
                            };
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
}
