package reactoredis;

import reactor.event.Event;
import redis.clients.jedis.Jedis;

import java.util.concurrent.ExecutorService;

/**
 * Created with IntelliJ IDEA.
 * User: sunhao
 * Date: 13-11-30
 * Time: 下午3:00
 */
public interface ConsumerStrategy {
    public void consumeEvent(Event<RedisRequest> event, Jedis jedis, ExecutorService executorService);
}
