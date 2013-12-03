package reactoredis.redis.nedis;

import com.github.nedis.RedisClient;
import com.github.nedis.callback.Callback;

/**
 * Created with IntelliJ IDEA.
 * User: sunhao
 * Date: 13-12-3
 * Time: 下午3:15
 */
public enum NedisRequestTypes {
    GET {
        @Override
        public String execute(RedisClient redisClient, String data) {
            return redisClient.get(data);
        }

        @Override
        public void executeAsync(RedisClient redisClient, String data, Callback callback) {
            redisClient.get(data, callback);
        }
    },

    ;

    public abstract String execute(RedisClient redisClient, String data);

    public abstract void executeAsync(RedisClient redisClient, String data, Callback callback);
}
