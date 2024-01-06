package searchengine.services.other;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;


public class MyFjpThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {

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
}