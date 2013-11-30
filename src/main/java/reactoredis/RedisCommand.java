package reactoredis;

import redis.clients.jedis.Jedis;

/**
 * Created with IntelliJ IDEA.
 * User: sunhao
 * Date: 13-11-30
 * Time: 下午2:48
 */
public enum RedisCommand {
    GET {
        @Override
        public byte[] execute(Jedis jedis, byte[] data) {
            return jedis.get(data);
        }
    };


    public abstract byte[] execute(Jedis jedis, byte[] data);
}
