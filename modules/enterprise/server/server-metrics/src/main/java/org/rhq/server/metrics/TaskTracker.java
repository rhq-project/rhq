package org.rhq.server.metrics;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author John Sanda
 */
public class TaskTracker {

    private int remainingTasks;

    private boolean schedulingFinished;

    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final Object monitor = new Object();

    private boolean aborted;

    private String errorMessage;

    public void addTask() {
        try {
            lock.writeLock().lock();
            remainingTasks++;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int getRemainingTasksing() {
        return remainingTasks;
    }

    public void finishedSchedulingTasks() {
        try {
            lock.writeLock().lock();
            schedulingFinished = true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void finishedTask() {
        try {
            lock.writeLock().lock();
            remainingTasks--;
            if (schedulingFinished && remainingTasks == 0) {
                synchronized (monitor) {
                    monitor.notify();
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void waitForTasksToFinish() throws InterruptedException, AbortedException {
        try {
            lock.readLock().lock();
            if (aborted) {
                throw new AbortedException(errorMessage);
            }
            if (remainingTasks == 0) {
                return;
            }
        } finally {
            lock.readLock().unlock();
        }
        synchronized (monitor) {
            monitor.wait();
        }
        try {
            lock.readLock().lock();
            if (aborted) {
                throw new AbortedException(errorMessage);
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    public void abort(String msg) {
        try {
            lock.writeLock().lock();
            errorMessage = msg;
            aborted = true;
            synchronized (monitor) {
                monitor.notify();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

}
