package sample.reactoredis;

import com.github.nedis.NettyRedisClientImpl;
import com.github.nedis.RedisClient;
import com.github.nedis.codec.BulkReply;
import com.github.nedis.codec.Reply;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.dsl.ProducerType;
import reactor.core.Reactor;
import reactor.event.Event;
import reactor.event.dispatch.RingBufferDispatcher;
import reactoredis.async.Callback;
import reactoredis.core.*;
import reactoredis.redis.RedisCallback;
import reactoredis.redis.nedis.AsyncNedisRedisClient;
import reactoredis.redis.nedis.NedisRequestTypes;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created with IntelliJ IDEA.
 * User: sunhao
 * Date: 13-12-3
 * Time: 下午3:05
 */
public class NedisClientTest {
    //run test
    //   java -Xmx4g -Xms4g -Xmn1500m -XX:+UseConcMarkSweepGC -cp ".:./lib/*" sampleeactoredis.NedisClientTest
    //

    // two redis : 6379, 16379
    // optional():
    // 0 1 core -> one
    // 2 3 core -> two


    private static String DATA = "USER_INFO_12345678901234567890123456789021";

    private static final int TOTAL = 2000000;

    private static long time1;

    private static final double STATISTICS_RATE = 0.95;

    static class ssss {
        public int cnt = 0;
    }

    final static Map<String, ssss> local = new ConcurrentHashMap<String, ssss>();
    static ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    public static void main(String[] args) throws InterruptedException {
//        RedisClient redisClient = new NettyRedisClientImpl("192.168.1.246", 6379);
//        String result = redisClient.get(DATA);
//        System.out.println(result);

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

//        ExecutorService executorService = Executors.newCachedThreadPool();
//        ExecutorService executorService = Executors.newFixedThreadPool(200);
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        ExecutorService bossPool = Executors.newFixedThreadPool(1);
        ExecutorService workerPool = Executors.newFixedThreadPool(1);

        final RedisClient redisClient = new NettyRedisClientImpl("192.168.1.246", 6379, workerPool, workerPool);
        final RedisClient redisClient2 = new NettyRedisClientImpl("192.168.1.246", 16379, bossPool, bossPool);

        System.out.println("start insert data....");
        final String[] KEYS = insertData(redisClient);
        insertData(redisClient2);
        System.out.println("end insert data.");
        System.gc();
        System.out.println("wait 10s...");
        TimeUnit.SECONDS.sleep(10);
        System.out.println("wait done. start task...");

        final AsyncNedisRedisClient client = new AsyncNedisRedisClient(
                2,
                new GeneralStrategy<RedisClient, NedisRequestTypes, String, String>() {
                    private int idx = 0;
                    private int clientIdx = 0;

                    @Override
                    public int getReactorHelperCnt() {
                        return 2;
                    }

                    @Override
                    public RedisClient decideHelperToBeUsed(Request<NedisRequestTypes, String, String> request, RedisClient[] helpers) {
                        return helpers[clientIdx++ & 1];
                    }

                    @Override
                    public String getEventId() {
                        return "nedis";
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
//                        return new Reactor(new RingBufferDispatcher("client-queue-" + idx.incrementAndGet()));
                        return new Reactor(new RingBufferDispatcher("client-queue-" + idx.incrementAndGet(), 2048, ProducerType.MULTI, new BlockingWaitStrategy()));
                    }
                },
                executorService,
                new ConsumerStrategy<NedisRequestTypes, RedisClient, String, String>() {
                    @Override
                    public void consumeEvent(Event<Request<NedisRequestTypes, String, String>> event, RedisClient helper, final ExecutorService executorService) {
                        final Request<NedisRequestTypes, String, String> request = event.getData();
//                        String result = request.getRequestTypeIdentifier().execute(helper, request.getRequestData());
//                        request.setResult(result);
//                        executorService.submit(request.getCallback());
                        request.getRequestTypeIdentifier().executeAsync(helper, request.getRequestData(), new com.github.nedis.callback.Callback() {
                            @Override
                            public void onReceive(Reply reply) {
                                String result = reply == null ? null : ((BulkReply) reply).getString();
                                request.setResult(result);
//                                executorService.submit(request.getCallback());

                                ssss s = local.get(Thread.currentThread().getName());
                                if (s != null) {
                                    s.cnt++;
                                } else {
                                    s = new ssss();
                                    local.put(Thread.currentThread().getName(), s);
                                }
                            }
                        });
                    }
                },
                new HelperBuilder<RedisClient>() {
                    @Override
                    public RedisClient build() {
                        return redisClient;
                    }

                    @Override
                    public RedisClient[] build(int cnt) {
                        RedisClient[] redisClients = new RedisClient[cnt];
                        if (cnt == 2) {
//                            redisClients[0] = redisClient;
//                            redisClients[1] = redisClient2;
//                            System.out.println("build two client for each.");
                            // make client order safe
                            redisClients[0] = new NettyRedisClientImpl("192.168.1.246", 6379, Executors.newFixedThreadPool(1), Executors.newFixedThreadPool(2));
                            redisClients[1] = new NettyRedisClientImpl("192.168.1.246", 16379, Executors.newFixedThreadPool(1), 1, Executors.newFixedThreadPool(2), 1);
                        } else {
                            Arrays.fill(redisClients, redisClient);
                        }
                        return redisClients;
                    }
                }
        );

        //build send thread
        Thread thread = new Thread(new SendTask(client, KEYS));
        Thread thread2 = new Thread(new SendTask(client, KEYS));

        time1 = System.currentTimeMillis();
        System.out.println("start: " + time1);

        thread.start();
        thread2.start();

        TimeUnit.SECONDS.sleep(100);

        System.out.println("Timeout. interrupted.");

        System.exit(1);

    }

    private static String[] insertData(final RedisClient redisClient) {
        String[] keys = new String[TOTAL];
        System.out.println("gen keys....");
        for (int i = 0, k = 1000000; i < TOTAL; i++, k++) {
            keys[i] = "USER_INFO_abcdefghijklmnopqrstuvwxyz" + k;
        }
        System.out.println("set key/value....");
        for (int i = 0, k = 1000000; i < TOTAL; i++, k++) {
            redisClient.set(keys[i], "/faceshow/FrontServer/FrontServer000000" + k);
        }
        System.out.println("prepare done.");
        return keys;
    }

    private static class SendTask implements Runnable {
        private AsyncNedisRedisClient client;
        private String[] KEYS;

        private SendTask(AsyncNedisRedisClient client, String[] keys) {
            this.client = client;
            this.KEYS = keys;
        }

        @Override
        public void run() {
            int sendTotal = TOTAL/2;
            for (int i = 0; i < sendTotal; i++) {
                final int finalI = i;
                client.request(new Request<NedisRequestTypes, String, String>() {
                    private String result;

                    @Override
                    public NedisRequestTypes getRequestTypeIdentifier() {
                        return NedisRequestTypes.GET;
                    }

                    @Override
                    public String getRequestData() {
                        return KEYS[finalI];
                    }

                    @Override
                    public String getResult() {
                        return this.result;
                    }

                    @Override
                    public Request setResult(String result) {
                        this.result = result;
                        return this;
                    }

                    @Override
                    public Callback getCallback() {
                        return new RedisCallback<NedisRequestTypes, String, String>(this) {
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
    }
}
