package com.freeheap.akkawl.util.throttler;

/**
 * @see <a href="http://www.cordinc.com/blog/2010/04/java-multichannel-asynchronous.html">Java Multi-channel Asynchronous Throttler</a>
 */
public interface TimeProvider {
    public static final TimeProvider SYSTEM_PROVIDER = new TimeProvider() {
        @Override public long getCurrentTimeInMillis() {return System.currentTimeMillis();}
    };

    public long getCurrentTimeInMillis();
}