package redis;

/**
 * Created with IntelliJ IDEA.
 * User: sunhao
 * Date: 13-11-30
 * Time: 上午10:09
 */
public interface RedisCommand {
    //TODO 改造一下，一下返回的command使用enum，传入的参数为jedis和data
    public byte[] command();
    public byte[] data();
    public RedisCallback callback();
}
