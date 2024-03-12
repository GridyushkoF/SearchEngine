package searchengine.util;

import lombok.extern.log4j.Log4j2;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

@Log4j2
public class MyForkJoinPoolThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {

    @Override
    public final ForkJoinWorkerThread newThread(ForkJoinPool pool) {
        return new CustomForkJoinWorkerThread(pool);
    }

    private static class CustomForkJoinWorkerThread extends ForkJoinWorkerThread {

        private CustomForkJoinWorkerThread(final ForkJoinPool pool) {
            super(pool);
            setContextClassLoader(Thread.currentThread()
                    .getContextClassLoader());
        }
    }
    public static ForkJoinPool createUniqueForkJoinPool() {
        return new ForkJoinPool(
                Runtime.getRuntime().availableProcessors(),
                new MyForkJoinPoolThreadFactory(),
                (t, e) -> log.error(LogMarkers.EXCEPTIONS, "Exception while creating MyFjpThreadFactory()", e),
                true
        );
    }
}