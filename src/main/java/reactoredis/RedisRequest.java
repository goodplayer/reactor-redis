package reactoredis;

/**
 * Created with IntelliJ IDEA.
 * User: sunhao
 * Date: 13-11-30
 * Time: 下午2:48
 */
public interface RedisRequest {
    public RedisCommand getCmd();
    public byte[] getData();
    public RedisCallback getCallback();
    public byte[] getResult();
    public void setResult(byte[] result);
}
