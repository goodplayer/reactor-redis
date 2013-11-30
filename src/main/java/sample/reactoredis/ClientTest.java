package sample.reactoredis;

import reactor.core.Reactor;
import reactor.event.Event;
import reactor.event.dispatch.RingBufferDispatcher;
import reactoredis.*;
import redis.clients.jedis.Jedis;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created with IntelliJ IDEA.
 * User: sunhao
 * Date: 13-11-30
 * Time: 下午2:20
 */
public class ClientTest {
    private static byte[] DATA = "USER_INFO_12345678901234567890123456789021".getBytes();

    private static AtomicInteger atomicInteger = new AtomicInteger(0);

    private static final int TOTAL = 100000;

    private static long time1;

    public static void main(String[] args) throws InterruptedException {

        ExecutorService executorService = Executors.newCachedThreadPool();
        final RedisClient redisClient = new RedisClient(
                new InetSocketAddress("192.168.1.243", 6379),
                8,
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
            }
        });

        time1 = System.currentTimeMillis();
        System.out.println(time1);

        thread.start();

        TimeUnit.SECONDS.sleep(100);

        System.out.println("Timeout. interrupted.");

        System.exit(1);

    }

    static class DemoCallback implements RedisCallback {
        private RedisRequest redisRequest;

        DemoCallback(RedisRequest redisRequest) {
            this.redisRequest = redisRequest;
        }

        @Override
        public void run() {
            if (atomicInteger.incrementAndGet() == TOTAL) {
                redisRequest.getResult();
                long time2 = System.currentTimeMillis();
                System.out.println(time2);
                long interval = time2 - time1;
                System.out.println(interval);
                System.out.println(TOTAL / ((double) interval));
            }
        }
    }
}
