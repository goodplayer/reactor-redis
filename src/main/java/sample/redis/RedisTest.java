package sample.redis;

import reactor.core.Reactor;
import reactor.event.Event;
import reactor.event.dispatch.RingBufferDispatcher;
import reactor.function.Consumer;
import redis.clients.jedis.Jedis;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static reactor.event.selector.Selectors.$;

/**
 * Created with IntelliJ IDEA.
 * User: sunhao
 * Date: 13-11-30
 * Time: 上午10:02
 */
public class RedisTest {
    private static int CNT = 0;
    private static int TOTAL = 100000;

    //TODO 每一个jedis一个reactor，使用的时候每个reactor都作为一个连接并随机分配给一个或多个用户使用
    //TODO 或者  user : reactor : jedis = nn : 1 : n
    //TODO 使用链式reactor，分成多个步骤，组合，最后触发callback
    //问题：多个reactor是否有性能问题
    public static void main(String[] args) throws InterruptedException {
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        final Jedis jedis = new Jedis("192.168.1.243", 6379);

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
                                    CNT++;
                                    if (CNT % 1000 == 0 ) {
                                        System.out.println(CNT);
                                    }
                                    if (CNT >= TOTAL) {
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
