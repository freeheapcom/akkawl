package com.freeheap.akkawl.util.throttler;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * Created by minhdo on 8/5/16.
 */
public final class QueueChannelThrottler extends AbstractChannelThrottler {

    private final Runnable processQueueTask = new Runnable() {
        @Override public void run() {
            FutureTask<?> task = tasks.poll();
            if (task!=null && !task.isCancelled()) {
                task.run();
            }
        }
    };
    private final Queue<FutureTask<?>> tasks = new LinkedList<FutureTask<?>>();

    public QueueChannelThrottler(Rate totalRate) {
        this(totalRate, Executors.newSingleThreadScheduledExecutor(), new HashMap<Object, Rate>(), TimeProvider.SYSTEM_PROVIDER);
    }

    public QueueChannelThrottler(Rate totalRate, Map<Object, Rate> channels) {
        this(totalRate, Executors.newSingleThreadScheduledExecutor(), channels, TimeProvider.SYSTEM_PROVIDER);
    }

    public QueueChannelThrottler(Rate totalRate, ScheduledExecutorService scheduler, Map<Object, Rate> channels, TimeProvider timeProvider) {
        super(totalRate, scheduler, channels, timeProvider);
    }

    @Override public Future<?> submit(Runnable task) {
        return submit(null, task);
    }

    @SuppressWarnings("unchecked")
    @Override public Future<?> submit(Object channelKey, Runnable task) {
        long throttledTime = channelKey==null?callTime(null):callTime(channels.get(channelKey));
        FutureTask runTask = new FutureTask(task, null);
        tasks.add(runTask);
        long now = timeProvider.getCurrentTimeInMillis();
        scheduler.schedule(processQueueTask, throttledTime<now?0:throttledTime-now, TimeUnit.MILLISECONDS);
        return runTask;
    }

    public void addRate(Object obj, Rate rate) { //it is ok to add twice
        super.channels.put(obj, rate);
    }
}
