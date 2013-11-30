package reactoredis;

import com.google.common.base.Preconditions;
import com.google.common.math.IntMath;
import reactor.core.Reactor;
import reactor.event.Event;
import reactor.function.Consumer;
import redis.clients.jedis.Jedis;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;

import static reactor.event.selector.Selectors.$;

/**
 * RedisClient with Reactor（not thread safe）
 * User: sunhao
 * Date: 13-11-30
 * Time: 下午1:29
 */
public class RedisClient {
    private final Reactor[] reactors;
    private final int exactCnt;
    private final ExecutorService executorService;
    private final ReactorFactory reactorFactory;
    private final ConsumerStrategy consumerStrategy;

    // wildcard for exactCnt to determine a reactor to notify event
    private final int wildcard;

    private InetSocketAddress redisServer;

    private static final String EVENT_ID = "redis";

    /**
     * 创建RedisClient
     *
     * @param redisServer      redis服务器地址
     * @param reactorCnt       reactor数量，2的reactorCnt次幂，此处取值0~31
     * @param reactorFactory   reactorFactory
     * @param executorService  redis处理线程池
     * @param consumerStrategy redis请求消费者策略
     */
    public RedisClient(
            InetSocketAddress redisServer,
            int reactorCnt,
            ReactorFactory reactorFactory,
            ExecutorService executorService,
            ConsumerStrategy consumerStrategy
    ) {
        Preconditions.checkArgument(reactorCnt >= 0, "reactorCnt should >= 0");
        this.exactCnt = IntMath.pow(2, reactorCnt);
        Preconditions.checkArgument(this.exactCnt != 0, "reactorCnt should <= 31");
        int wildcard = 0;
        for (int i = 0; i < reactorCnt; i++) {
            wildcard |= (1 << i);
        }
        this.wildcard = wildcard;
        this.reactors = new Reactor[this.exactCnt];
        this.reactorFactory = Preconditions.checkNotNull(reactorFactory, "reactorFactory");
        this.executorService = Preconditions.checkNotNull(executorService, "executorService");
        this.redisServer = Preconditions.checkNotNull(redisServer, "redisServer");
        this.consumerStrategy = Preconditions.checkNotNull(consumerStrategy, "consumerStrategy");

        for (int i = 0; i < this.exactCnt; i++) {
            Reactor reactor = this.reactorFactory.buildReactor();
            reactors[i] = reactor;
            final int finalI = i;
            reactor.on($(getEventId()), new Consumer<Event<RedisRequest>>() {
                private int id = finalI;
                private Jedis jedis = new Jedis(RedisClient.this.redisServer.getHostName(), RedisClient.this.redisServer.getPort());

                @Override
                public void accept(Event<RedisRequest> event) {
                    RedisClient.this.consumerStrategy.consumeEvent(event, this.jedis, RedisClient.this.executorService);
                }
            });
        }
    }

    protected String getEventId() {
        return EVENT_ID;
    }

    private int curIdx = 0;

    private int genIdxAndInc() {
        int result = curIdx++;
        curIdx &= this.wildcard;
        return result & this.wildcard;
    }

    public final Event<RedisRequest> request(RedisRequest redisRequest) {
        Event<RedisRequest> event = Event.wrap(redisRequest);
        reactors[genIdxAndInc()].notify(getEventId(), event);
        return event;
    }
}
