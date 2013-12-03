package reactoredis.redis.nedis;

import com.github.nedis.RedisClient;
import reactoredis.core.*;

import java.util.concurrent.ExecutorService;

/**
 * Created with IntelliJ IDEA.
 * User: sunhao
 * Date: 13-12-3
 * Time: 下午3:15
 */
public class AsyncNedisRedisClient extends AsyncClient<RedisClient, NedisRequestTypes, String, String> {
    /**
     * 创建AsyncClient
     *
     * @param reactorCnt       reactor数量，2的reactorCnt次幂，此处取值0~31
     * @param generalStrategy  公共策略
     * @param reactorFactory   reactorFactory
     * @param executorService  处理线程池
     * @param consumerStrategy 请求消费者策略
     * @param helperBuilder    消费者助手
     */
    public AsyncNedisRedisClient(int reactorCnt, GeneralStrategy<RedisClient, NedisRequestTypes, String, String> generalStrategy, ReactorFactory reactorFactory, ExecutorService executorService, ConsumerStrategy<NedisRequestTypes, RedisClient, String, String> consumerStrategy, HelperBuilder<RedisClient> helperBuilder) {
        super(reactorCnt, generalStrategy, reactorFactory, executorService, consumerStrategy, helperBuilder);
    }
}
