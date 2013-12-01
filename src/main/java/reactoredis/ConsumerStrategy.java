package reactoredis;

import reactor.event.Event;
import redis.clients.jedis.Jedis;

import java.util.concurrent.ExecutorService;

/**
 * 事件消费策略
 *
 * @param <H> 消费者工具或消费者助手
 * @param <R> 请求类型
 * @param <D> 请求数据
 * @param <B> 响应结果
 */
public interface ConsumerStrategy<H, R, D, B> {
    public void consumeEvent(Event<RedisRequest<R, D, B>> event, H helper, ExecutorService executorService);
}
