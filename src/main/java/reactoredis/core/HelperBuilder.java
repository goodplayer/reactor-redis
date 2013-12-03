package reactoredis.core;

/**
 * Created with IntelliJ IDEA.
 * User: sunhao
 * Date: 13-12-1
 * Time: 下午12:36
 */
public interface HelperBuilder<H> {
    public H build();
    public H[] build(int cnt);
}
