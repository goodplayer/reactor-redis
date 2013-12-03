package reactoredis.core;

import reactoredis.async.Callback;

/**
 * Created with IntelliJ IDEA.
 * User: sunhao
 * Date: 13-12-3
 * Time: 上午11:12
 */
public interface Request<T, D, R> {
    public T getRequestTypeIdentifier();

    public D getRequestData();

    public R getResult();

    public Request setResult(R result);

    public Callback getCallback();
}
