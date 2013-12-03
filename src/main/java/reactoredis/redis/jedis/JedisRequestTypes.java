package reactoredis.redis.jedis;

import redis.clients.jedis.Jedis;

/**
 * Created with IntelliJ IDEA.
 * User: sunhao
 * Date: 13-12-3
 * Time: 下午1:44
 */
public enum JedisRequestTypes {
    GET {
        @Override
        public byte[] executeCommand(Jedis jedis, byte[] data) {
            return jedis.get(data);
        }
    },

    ;

    public abstract byte[] executeCommand(Jedis jedis, byte[] data);
}
