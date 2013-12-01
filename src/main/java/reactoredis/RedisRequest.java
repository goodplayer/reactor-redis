package reactoredis;

/**
 *
 * @param <R> 请求类型
 * @param <D> 请求数据
 * @param <B> 返回结果
 */
public interface RedisRequest<R, D, B> {
    public R getCmd();
    public D getData();
    public RedisCallback getCallback();
    public B getResult();
    public void setResult(B result);
}
