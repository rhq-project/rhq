package org.rhq.core.pc.drift;

import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.rhq.core.domain.drift.DriftConfiguration;

public class ScheduleQueueImpl implements ScheduleQueue {

    private PriorityQueue<DriftDetectionSchedule> queue = new PriorityQueue<DriftDetectionSchedule>();

    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private DriftDetectionSchedule activeSchedule;

    @Override
    public DriftDetectionSchedule getNextSchedule() {
        try {
            lock.writeLock().lock();
            if (activeSchedule != null) {
                throw new IllegalStateException("There is already an active schedule that must be deactivated " +
                    "before getting the next schedule.");
            }
            activeSchedule = queue.poll();
            return activeSchedule == null ? null : activeSchedule.copy();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private boolean isActiveSchedule(int resourceId, DriftConfiguration config) {
        try {
            lock.readLock().lock();
            return activeSchedule != null && activeSchedule.getResourceId() == resourceId &&
                   activeSchedule.getDriftConfiguration().getName().equals(config.getName());
        } finally {
            lock.readLock().unlock();
        }

    }

    @Override
    public void deactivateSchedule() {
        try {
            lock.writeLock().lock();
            if (activeSchedule == null) {
                return;
            }
            activeSchedule.updateShedule();
            queue.offer(activeSchedule);
            activeSchedule = null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean addSchedule(DriftDetectionSchedule schedule) {
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
    public DriftDetectionSchedule remove(int resourceId, DriftConfiguration config) {
        try {
            lock.writeLock().lock();
            if (isActiveSchedule(resourceId, config)) {
                DriftDetectionSchedule removedSchedule = activeSchedule;
                activeSchedule = null;
                return removedSchedule;
            }

            Iterator<DriftDetectionSchedule> iterator = queue.iterator();
            while (iterator.hasNext()) {
                DriftDetectionSchedule schedule = iterator.next();
                if (schedule.getResourceId() == resourceId &&
                    schedule.getDriftConfiguration().getName().equals(config.getName())) {
                    iterator.remove();
                    return schedule;
                }
            }

            return null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public DriftDetectionSchedule update(int resourceId, DriftConfiguration config) {
        DriftDetectionSchedule schedule = remove(resourceId, config);
        if (schedule == null) {
            return null;
        }

        update(schedule, config);

        try {
            lock.writeLock().lock();
            if (queue.offer(schedule)) {
                return schedule.copy();
            }
            return null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void update(DriftDetectionSchedule schedule, DriftConfiguration config) {
        schedule.getDriftConfiguration().setEnabled(config.getEnabled());
        schedule.getDriftConfiguration().setInterval(config.getInterval());
    }

    @Override
    public void clear() {
        try {
            lock.writeLock().lock();
            activeSchedule = null;
            queue.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
}
