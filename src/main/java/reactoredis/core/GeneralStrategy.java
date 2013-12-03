package reactoredis.core;

/**
 * Created with IntelliJ IDEA.
 * User: sunhao
 * Date: 13-12-3
 * Time: 上午11:07
 */
public interface GeneralStrategy<H, T, D, R> {
    //the number of clients that each reactor will use
    public int getReactorHelperCnt();
    //decide which
    public H decideHelperToBeUsed(Request<T, D, R> request, H[] helpers);
    //event id
    public String getEventId();
    //generate idx
    public int genNextIdxOfReactorToUse(int wildcard);
}
