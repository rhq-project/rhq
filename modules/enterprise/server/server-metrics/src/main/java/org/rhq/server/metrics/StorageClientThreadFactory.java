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

    private AtomicInteger threadNumber = new AtomicInteger(0);

    private String poolName;

    public StorageClientThreadFactory() {
        this("StorageClientThreadPool");
    }

    public StorageClientThreadFactory(String name) {
        this.poolName = name;
        log = LogFactory.getLog(poolName);
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
