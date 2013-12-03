package sample.reactoredis;

import com.github.nedis.NettyRedisClientImpl;
import com.github.nedis.RedisClient;
import com.github.nedis.codec.BulkReply;
import com.github.nedis.codec.Reply;
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
    //   java -Xmx2g -Xms2g -cp ".:./lib/*" sample.reactoredis.NedisClientTest



    private static String DATA = "USER_INFO_12345678901234567890123456789021";

    private static final int TOTAL = 1000000;

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

        final RedisClient redisClient = new NettyRedisClientImpl("192.168.1.246", 6379);

        final AsyncNedisRedisClient client = new AsyncNedisRedisClient(
                2,
                new GeneralStrategy<RedisClient, NedisRequestTypes, String, String>() {
                    private int idx = 0;

                    @Override
                    public int getReactorHelperCnt() {
                        return 1;
                    }

                    @Override
                    public RedisClient decideHelperToBeUsed(Request<NedisRequestTypes, String, String> request, RedisClient[] helpers) {
                        return helpers[0];
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
                        return new Reactor(new RingBufferDispatcher("client-queue-" + idx.incrementAndGet()));
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
                                String result = reply == null ? null : ((BulkReply)reply).getString();
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
                        Arrays.fill(redisClients, redisClient);
                        return redisClients;
                    }
                }
        );

        //build send thread
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < TOTAL; i++) {
                    client.request(new Request<NedisRequestTypes, String, String>() {
                        private String result;

                        @Override
                        public NedisRequestTypes getRequestTypeIdentifier() {
                            return NedisRequestTypes.GET;
                        }

                        @Override
                        public String getRequestData() {
                            return DATA;
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
        });

        time1 = System.currentTimeMillis();
        System.out.println("start: " + time1);

        thread.start();

        TimeUnit.SECONDS.sleep(100);

        System.out.println("Timeout. interrupted.");

        System.exit(1);

    }
}
