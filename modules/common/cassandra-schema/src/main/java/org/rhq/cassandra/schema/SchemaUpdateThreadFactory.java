package org.rhq.cassandra.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author John Sanda
 */
public class SchemaUpdateThreadFactory implements ThreadFactory, Thread.UncaughtExceptionHandler {

    private final Log log;

    private AtomicInteger threadNumber = new AtomicInteger(0);

    private String poolName = "SchemaUpdateThreadPool";

    private Thread schemaUpdateThread;

    private List<Thread> threads = new ArrayList<Thread>();

    public SchemaUpdateThreadFactory() {
        log = LogFactory.getLog(poolName);
        schemaUpdateThread = Thread.currentThread();
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r, poolName + "-" + threadNumber.getAndIncrement());
        threads.add(t);
        t.setDaemon(false);
        t.setUncaughtExceptionHandler(this);

        return t;
    }

    public List<Thread> getThreads() {
        return threads;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        log.error("Uncaught exception on scheduled thread [" + t.getName() + "]", e);
        schemaUpdateThread.interrupt();
    }

}
