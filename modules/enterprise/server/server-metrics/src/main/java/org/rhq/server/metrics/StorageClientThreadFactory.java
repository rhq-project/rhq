package org.rhq.server.metrics;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author John Sanda
 */
public class StorageClientThreadFactory implements ThreadFactory, Thread.UncaughtExceptionHandler {

    private final Log log;

    private final AtomicInteger threadNumber = new AtomicInteger(0);

    private final String poolName;

    public StorageClientThreadFactory(String poolName) {
        log = LogFactory.getLog(poolName);
        this.poolName = poolName;
    }
    
    public StorageClientThreadFactory() {
        this("StorageClientThreadPool");
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r, poolName + "-" + threadNumber.getAndIncrement());
        t.setDaemon(false);
        t.setUncaughtExceptionHandler(this);

        return t;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        log.error("Uncaught exception on scheduled thread [" + t.getName() + "]", e);
    }

}
