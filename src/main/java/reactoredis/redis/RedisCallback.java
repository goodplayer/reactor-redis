package reactoredis.redis;

import reactoredis.async.Callback;
import reactoredis.core.Request;

/**
 * Created with IntelliJ IDEA.
 * User: sunhao
 * Date: 13-12-3
 * Time: 下午2:37
 */
public abstract class RedisCallback<T, D, R> implements Callback {
    private Request<T, D, R> request;
    public RedisCallback(Request<T, D, R> request) {
        this.request = request;
    }

    public Request<T, D, R> getRequest() {
        return request;
    }
}
