package com.freeheap.akkawl.util.throttler;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.concurrent.TimeUnit;

/**
 * @see <a href="http://www.cordinc.com/blog/2010/04/java-multichannel-asynchronous.html">Java Multi-channel Asynchronous Throttler</a>
 */
public final class Rate {

    private final int numberCalls;
    private final int timeLength;
    private final TimeUnit timeUnit;
    private final LinkedList<Long> callHistory = new LinkedList<Long>();

    public Rate(int numberCalls, int timeLength, TimeUnit timeUnit) {
        this.numberCalls = numberCalls;
        this.timeLength = timeLength;
        this.timeUnit = timeUnit;
    }

    private long timeInMillis() {
        return timeUnit.toMillis(timeLength);
    }


    /* package */ void addCall(long callTime) {
        callHistory.addLast(callTime);
    }

    private void cleanOld(long now) {
        ListIterator<Long> i = callHistory.listIterator();
        long threshold = now-timeInMillis();
        while (i.hasNext()) {
            if (i.next()<=threshold) {
                i.remove();
            } else {
                break;
            }
        }
    }

    /* package */ long callTime(long now) {
        cleanOld(now);
        if (callHistory.size()<numberCalls) {
            return now;
        }
        long lastStart = callHistory.getLast()-timeInMillis();
        long firstPeriodCall=lastStart, call;
        int count = 0;
        Iterator<Long> i = callHistory.descendingIterator();
        while (i.hasNext()) {
            call = i.next();
            if (call<lastStart) {
                break;
            } else {
                count++;
                firstPeriodCall = call;
            }
        }
        if (count<numberCalls) {
            return firstPeriodCall+1;
        } else {
            return firstPeriodCall+timeInMillis()+1;
        }
    }
}
