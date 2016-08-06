package com.freeheap.akkawl.util.throttler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @see <a href="http://www.cordinc.com/blog/2010/04/java-multichannel-asynchronous.html">Java Multi-channel Asynchronous Throttler</a>
 */
/* package */ abstract class AbstractChannelThrottler implements ChannelThrottler {

    protected final Rate totalRate;
    protected final TimeProvider timeProvider;
    protected final ScheduledExecutorService scheduler;
    protected final Map<Object, Rate> channels = new HashMap<Object, Rate>();

    protected AbstractChannelThrottler(Rate totalRate, ScheduledExecutorService scheduler, Map<Object, Rate> channels, TimeProvider timeProvider) {
        this.totalRate = totalRate;
        this.scheduler = scheduler;
        this.channels.putAll(channels);
        this.timeProvider = timeProvider;
    }

    protected synchronized long callTime(Rate channel) {
        long now = timeProvider.getCurrentTimeInMillis();
        long callTime = totalRate.callTime(now);
        if (channel!=null) {
            callTime = Math.max(callTime, channel.callTime(now));
            channel.addCall(callTime);
        }
        totalRate.addCall(callTime);
        return callTime;
    }

    protected long getThrottleDelay(Object channelKey) {
        long delay = callTime(channels.get(channelKey))-timeProvider.getCurrentTimeInMillis();
        return delay<0?0:delay;
    }
}