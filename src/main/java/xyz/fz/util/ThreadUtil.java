package xyz.fz.util;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class ThreadUtil {

    private static final ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(
            1,
            new BasicThreadFactory.Builder().namingPattern("thread-util-pool-%d").daemon(true).build()
    );

    public static void execute(Runnable runnable) {
        executorService.execute(runnable);
    }
}
