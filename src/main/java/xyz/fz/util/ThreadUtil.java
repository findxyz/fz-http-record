package xyz.fz.util;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class ThreadUtil {

    private static ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors() * 2,
            new BasicThreadFactory.Builder().namingPattern("util-process-pool-%d").daemon(true).build()
    );

    public static ScheduledExecutorService executorService() {
        return executorService;
    }
}
