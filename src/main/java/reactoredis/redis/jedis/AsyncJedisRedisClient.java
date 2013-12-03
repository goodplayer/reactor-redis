package reactoredis.redis.jedis;

import reactoredis.core.*;
import redis.clients.jedis.Jedis;

import java.util.concurrent.ExecutorService;

/**
 * Created with IntelliJ IDEA.
 * User: sunhao
 * Date: 13-12-3
 * Time: 下午1:42
 */
public class AsyncJedisRedisClient extends AsyncClient<Jedis, JedisRequestTypes, byte[], byte[]> {
    /**
     * 创建RedisClient
     *
     * @param reactorCnt       reactor数量，2的reactorCnt次幂，此处取值0~31
     * @param generalStrategy  公共策略
     * @param reactorFactory   reactorFactory
     * @param executorService  redis处理线程池
     * @param consumerStrategy redis请求消费者策略
     * @param helperBuilder    消费者助手
     */
    public AsyncJedisRedisClient(int reactorCnt, GeneralStrategy<Jedis, JedisRequestTypes, byte[], byte[]> generalStrategy, ReactorFactory reactorFactory, ExecutorService executorService, ConsumerStrategy<JedisRequestTypes, Jedis, byte[], byte[]> consumerStrategy, HelperBuilder<Jedis> helperBuilder) {
        super(reactorCnt, generalStrategy, reactorFactory, executorService, consumerStrategy, helperBuilder);
    }
}
