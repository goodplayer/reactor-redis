package reactoredis.core;

import reactor.event.Event;

import java.util.concurrent.ExecutorService;

/**
 * Created with IntelliJ IDEA.
 * User: sunhao
 * Date: 13-12-3
 * Time: 上午11:27
 */
public interface ConsumerStrategy<T, H, D, R> {
    public void consumeEvent(Event<Request<T, D, R>> event, H helper, ExecutorService executorService);
}
