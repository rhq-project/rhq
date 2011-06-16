package org.rhq.core.pc.drift;

import java.util.PriorityQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ScheduleQueueImpl implements ScheduleQueue {

    private PriorityQueue<DriftDetectionSchedule> queue = new PriorityQueue<DriftDetectionSchedule>();

    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public DriftDetectionSchedule dequeue() {
        Lock writeLock = lock.writeLock();
        try {
            return queue.poll();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean enqueue(DriftDetectionSchedule schedule) {
        Lock writeLock = lock.writeLock();
        boolean inserted = false;
        try {
            writeLock.lock();
            inserted = queue.offer(schedule);
        } finally {
            writeLock.unlock();
        }
        return inserted;
    }

    @Override
    public void clear() {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            queue.clear();
        } finally {
            writeLock.unlock();
        }
    }
}
