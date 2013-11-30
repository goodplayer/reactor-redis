package reactoredis;

import reactor.core.Reactor;

/**
 * Created with IntelliJ IDEA.
 * User: sunhao
 * Date: 13-11-30
 * Time: 下午2:01
 */
public interface ReactorFactory {
    public Reactor buildReactor();
}
