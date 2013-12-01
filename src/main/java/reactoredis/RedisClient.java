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

/**
 * RedisClient with Reactor（not thread safe）
 *
 * @param <H> 消费者工具或消费者助手
 * @param <R> 请求类型
 */
public class RedisClient<H, R> {
    private final Reactor[] reactors;
    private final int exactCnt;
    private final ExecutorService executorService;
    private final ReactorFactory reactorFactory;
    private final ConsumerStrategy<H, R, byte[], byte[]> consumerStrategy;
    private final HelperBuilder<H> helperBuilder;

    // wildcard for exactCnt to determine a reactor to notify event
    private final int wildcard;

    private static final String EVENT_ID = "redis";

    /**
     * 创建RedisClient
     *
     * @param reactorCnt       reactor数量，2的reactorCnt次幂，此处取值0~31
     * @param reactorFactory   reactorFactory
     * @param executorService  redis处理线程池
     * @param consumerStrategy redis请求消费者策略
     * @param helperBuilder    消费者助手
     */
    public RedisClient(
            int reactorCnt,
            ReactorFactory reactorFactory,
            ExecutorService executorService,
            ConsumerStrategy<H, R, byte[], byte[]> consumerStrategy,
            HelperBuilder<H> helperBuilder
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
        this.consumerStrategy = Preconditions.checkNotNull(consumerStrategy, "consumerStrategy");
        this.helperBuilder = Preconditions.checkNotNull(helperBuilder, "consumerHelper");

        for (int i = 0; i < this.exactCnt; i++) {
            Reactor reactor = this.reactorFactory.buildReactor();
            reactors[i] = reactor;
            final int finalI = i;
            reactor.on($(getEventId()), new Consumer<Event<RedisRequest<R, byte[], byte[]>>>() {//FIXME will cause Type erasure problem
                private int id = finalI;
                private H consumerHelper = RedisClient.this.helperBuilder.build();

                @Override
                public void accept(Event<RedisRequest<R, byte[], byte[]>> event) {
                    RedisClient.this.consumerStrategy.consumeEvent(event, consumerHelper, RedisClient.this.executorService);
                }
            });
        }
    }

    protected String getEventId() {//TODO try to be implemented with strategy pattern
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
