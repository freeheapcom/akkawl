package com.freeheap.akkawl.util.throttler;

import java.util.concurrent.Future;

/**
 * @see <a href="http://www.cordinc.com/blog/2010/04/java-multichannel-asynchronous.html">Java Multi-channel Asynchronous Throttler</a>
 */
public interface ChannelThrottler {
    Future<?> submit(Runnable task);
    Future<?> submit(Object channelKey, Runnable task);
}