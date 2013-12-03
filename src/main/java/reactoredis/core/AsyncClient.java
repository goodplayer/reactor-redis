package reactoredis.core;

import com.google.common.base.Preconditions;
import com.google.common.math.IntMath;
import reactor.core.Reactor;
import reactor.event.Event;
import reactor.function.Consumer;

import java.util.concurrent.ExecutorService;

import static reactor.event.selector.Selectors.$;

/**
 * @param <H> client type
 * @param <T> request type identifier
 * @param <D> request data type
 * @param <R> request result type
 */
// N worker -> M client : outside control
// 1 client -> N reactor : strategy
// 1 reactor -> N connection : strategy
public class AsyncClient<H, T, D, R> {
    private final Reactor[] reactors;
    private final int exactCnt;
    private final ExecutorService executorService;
    private final ReactorFactory reactorFactory;
    private final ConsumerStrategy<T, H, D, R> consumerStrategy;
    private final HelperBuilder<H> helperBuilder;
    private final GeneralStrategy<H, T, D, R> generalStrategy;

    // wildcard for exactCnt to determine a reactor to notify event
    private final int wildcard;

    private final String EVENT_ID;

    /**
     * 创建AsyncClient
     *
     * @param reactorCnt       reactor数量，2的reactorCnt次幂，此处取值0~31
     * @param generalStrategy  公共策略
     * @param reactorFactory   reactorFactory
     * @param executorService  处理线程池
     * @param consumerStrategy 请求消费者策略
     * @param helperBuilder    消费者助手
     */
    public AsyncClient(
            int reactorCnt,
            GeneralStrategy<H, T, D, R> generalStrategy,
            ReactorFactory reactorFactory,
            ExecutorService executorService,
            ConsumerStrategy<T, H, D, R> consumerStrategy,
            HelperBuilder<H> helperBuilder
    ) {
        Preconditions.checkArgument(reactorCnt >= 0, "reactorCnt should >= 0");
        this.exactCnt = IntMath.pow(2, reactorCnt);
        Preconditions.checkArgument(this.exactCnt != 0, "reactorCnt should <= 31");
        int wildcard = 0;
        for (int i = 0; i < reactorCnt; i++) {
            wildcard |= (1 << i);
        }
        this.wildcard = wildcard;
        this.reactors = new Reactor[this.exactCnt];
        this.reactorFactory = Preconditions.checkNotNull(reactorFactory, "reactorFactory");
        this.executorService = Preconditions.checkNotNull(executorService, "executorService");
        this.generalStrategy = Preconditions.checkNotNull(generalStrategy, "generalStrategy");
        EVENT_ID = this.generalStrategy.getEventId();
        this.helperBuilder = Preconditions.checkNotNull(helperBuilder, "consumerHelper");

        this.consumerStrategy = Preconditions.checkNotNull(consumerStrategy, "consumerStrategy");

        for (int i = 0; i < this.exactCnt; i++) {
            Reactor reactor = this.reactorFactory.buildReactor();
            reactors[i] = reactor;
            final int finalI = i;
            Preconditions.checkArgument(this.generalStrategy.getReactorHelperCnt() > 0, "reactor helper cnt must > 0");
            final H[] genConsumerHelper = this.helperBuilder.build(this.generalStrategy.getReactorHelperCnt());
            Preconditions.checkArgument(genConsumerHelper.length == this.generalStrategy.getReactorHelperCnt(), "GeneralStrategy does not generate enough consumerHelper.");
            reactor.on($(this.EVENT_ID), new Consumer<Event<Request<T, D, R>>>() {//FIXME will cause Type erasure problem
                private int id = finalI;
                private H[] consumerHelper = genConsumerHelper;

                @Override
                public void accept(Event<Request<T, D, R>> event) {
                    AsyncClient.this.consumerStrategy.consumeEvent(event, AsyncClient.this.generalStrategy.decideHelperToBeUsed(event.getData(), this.consumerHelper), AsyncClient.this.executorService);
                }
            });
        }
    }

    public final Event<Request<T, D, R>> request(Request<T, D, R> request) {
        Event<Request<T, D, R>> event = Event.wrap(request);
        reactors[this.generalStrategy.genNextIdxOfReactorToUse(this.wildcard)].notify(this.generalStrategy.getEventId(), event);
        return event;
    }
}
