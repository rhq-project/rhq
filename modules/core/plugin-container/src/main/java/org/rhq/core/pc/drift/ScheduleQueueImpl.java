package org.rhq.core.pc.drift;

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
        return activeSchedule != null && activeSchedule.getResourceId() == resourceId &&
               activeSchedule.getDriftConfiguration().getName().equals(config.getName());

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
        DriftDetectionSchedule scheduleToRemove = null;
        try {
            lock.readLock().lock();
            if (isActiveSchedule(resourceId, config)) {
                // The schedule to be removed is the currently active schedule so "upgrade"
                // to the write lock and return the schedule while removing it from the queue
                // at the same time.
                try {
                    lock.writeLock().lock();
                    DriftDetectionSchedule removedSchedule = activeSchedule;
                    activeSchedule = null;
                    return removedSchedule;
                } finally {
                    lock.writeLock().unlock();
                }
            }

            for (DriftDetectionSchedule s : queue) {
                if (s.getResourceId() == resourceId &&
                    s.getDriftConfiguration().getName().equals(config.getName())) {
                    scheduleToRemove = s;
                    break;
                }
            }

            // The schedule was not found in the queue so we can simply return null without
            // any additional processing.
            if (scheduleToRemove == null) {
                return null;
            }

            boolean removed = false;
            // At this point, we found the target schedule in the queue. We "upgrade" to
            // the write lock and remove it from the queue. If the schedule was successfully
            // removed we return it; otherwise, return null.
            try {
                lock.writeLock().lock();
                removed = queue.remove(scheduleToRemove);
            } finally {
                lock.writeLock().unlock();
            }

            return removed ? scheduleToRemove : null;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public DriftDetectionSchedule update(int resourceId, DriftConfiguration config) {
        try {
            lock.writeLock().lock();
            if (isActiveSchedule(resourceId, config)) {
                update(activeSchedule, config);
                return activeSchedule.copy();
            }

            DriftDetectionSchedule schedule = remove(resourceId, config);
            if (schedule == null) {
                return null;
            }

            update(schedule, config);

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
