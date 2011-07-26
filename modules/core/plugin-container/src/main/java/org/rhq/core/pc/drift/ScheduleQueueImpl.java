package org.rhq.core.pc.drift;

import java.util.PriorityQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ScheduleQueueImpl implements ScheduleQueue {

    private PriorityQueue<DriftDetectionSchedule> queue = new PriorityQueue<DriftDetectionSchedule>();

    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public DriftDetectionSchedule dequeue() {
        try {
            lock.writeLock().lock();
            return queue.poll();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean enqueue(DriftDetectionSchedule schedule) {
        boolean inserted = false;
        try {
            lock.writeLock().lock();
            inserted = queue.offer(schedule);
        } finally {
            lock.writeLock().unlock();
        }
        return inserted;
    }

    @Override
    public void clear() {
        try {
            lock.writeLock().lock();
            queue.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
}
