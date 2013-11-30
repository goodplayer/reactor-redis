package sample.redis;

import reactor.core.Reactor;
import reactor.event.Event;
import reactor.event.dispatch.RingBufferDispatcher;
import reactor.function.Consumer;
import redis.clients.jedis.Jedis;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static reactor.event.selector.Selectors.$;

/**
 * Created with IntelliJ IDEA.
 * User: sunhao
 * Date: 13-11-30
 * Time: 上午10:02
 */
public class RedisTestMultiThread {
    private static AtomicInteger CNT = new AtomicInteger(0);
    private static int TOTAL = 100000;

    public static void main(String[] args) throws InterruptedException {
        final ExecutorService executorService = Executors.newCachedThreadPool();
        final Jedis jedis = new Jedis("192.168.48.134", 6379);

        final Reactor reactor = new Reactor(new RingBufferDispatcher("redis"));

        reactor.on($("redis"), new Consumer<Event<RedisCommand>>() {
            @Override
            public void accept(Event<RedisCommand> event) {
                String result = jedis.get("hahaha");
                executorService.submit(event.getData().callback());
            }
        });

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < TOTAL; i++) {
                    reactor.notify("redis", Event.wrap(new RedisCommand() {
                        @Override
                        public byte[] command() {
                            return new byte[0];
                        }

                        @Override
                        public byte[] data() {
                            return new byte[0];
                        }

                        @Override
                        public RedisCallback callback() {
                            return new RedisCallback() {
                                @Override
                                public void run() {
                                    if (CNT.incrementAndGet() >= TOTAL) {
                                        System.out.println(System.currentTimeMillis());
                                    }
                                }
                            };
                        }
                    }));
                }
            }
        });

        System.out.println(System.currentTimeMillis());

        thread.start();

        TimeUnit.SECONDS.sleep(100);

        System.exit(1);
    }
}
