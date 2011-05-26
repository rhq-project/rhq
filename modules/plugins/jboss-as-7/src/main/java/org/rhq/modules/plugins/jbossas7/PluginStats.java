package org.rhq.modules.plugins.jbossas7;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Singleton that keeps track of some statistics of this plugin
 * @author Heiko W. Rupp
 */
public class PluginStats {
    private static PluginStats ourInstance = new PluginStats();

    AtomicLong requestCount = new AtomicLong();
    AtomicLong requestTime = new AtomicLong();
    private static final int FIFO_SIZE = 200; // Initial capacity
    List<Long> maxTime = new ArrayList<Long>(FIFO_SIZE);
    final Object lock = new Object();


    public static PluginStats getInstance() {
        return ourInstance;
    }

    private PluginStats() {
    }

    public void incrementRequestCount() {
        requestCount.incrementAndGet();
    }

    public void addRequestTime(long l) {
        requestTime.addAndGet(l);
        insertTime(l);
    }

    public long getRequestCount() {
        return requestCount.get();
    }

    public long getRequestTime() {
        return requestTime.get();
    }

    public long getMaxTime() {
        long max = 0;
        synchronized (lock) {
            for (Long i : maxTime)
                if (i > max )
                    max = i;
            maxTime = new ArrayList<Long>();
        }
        return max;
    }

    private void insertTime(long time) {
        synchronized (lock) {
            maxTime.add(time);
        }
    }
}
