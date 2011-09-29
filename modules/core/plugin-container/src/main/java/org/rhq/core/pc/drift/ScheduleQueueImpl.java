package org.rhq.core.pc.drift;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.core.domain.drift.DriftConfigurationComparator;

public class ScheduleQueueImpl implements ScheduleQueue {

    private static final Runnable NO_OP = new Runnable() {
        @Override
        public void run() {
        }
    };

    private PriorityQueue<DriftDetectionSchedule> queue = new PriorityQueue<DriftDetectionSchedule>();

    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private DriftDetectionSchedule activeSchedule;

    private Runnable deactivationTask;

    @Override
    public DriftDetectionSchedule getNextSchedule() {
        try {
            lock.writeLock().lock();
            if (activeSchedule != null) {
                throw new IllegalStateException("There is already an active schedule that must be deactivated "
                    + "before getting the next schedule.");
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
            return activeSchedule != null && activeSchedule.getResourceId() == resourceId
                && activeSchedule.getDriftConfiguration().getName().equals(config.getName());
        } finally {
            lock.readLock().unlock();
        }
    }

    private boolean isActiveSchedule(int resourceId, DriftConfiguration config,
        DriftConfigurationComparator comparator) {
        try {
            lock.readLock().lock();
            return activeSchedule != null && activeSchedule.getResourceId() == resourceId
                && comparator.compare(activeSchedule.getDriftConfiguration(), config) == 0;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void deactivateSchedule() {
        try {
            lock.writeLock().lock();
            if (deactivationTask != null) {
                deactivationTask.run();
            }
            if (activeSchedule == null) {
                return;
            }
            activeSchedule.updateShedule();
            queue.offer(activeSchedule);
            activeSchedule = null;
        } finally {
            deactivationTask = null;
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
    public boolean contains(int resourceId, DriftConfiguration config) {
        if (isActiveSchedule(resourceId, config)) {
            return true;
        }
        try {
            lock.readLock().lock();
            for (DriftDetectionSchedule schedule : queue) {
                if (schedule.getResourceId() == resourceId &&
                    schedule.getDriftConfiguration().getName().equals(config.getName())) {
                    return true;
                }
            }
            return false;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean contains(int resourceId, DriftConfiguration config, DriftConfigurationComparator comparator) {
        if (isActiveSchedule(resourceId, config, comparator)) {
            return true;
        }
        try {
            lock.readLock().lock();
            for (DriftDetectionSchedule schedule : queue) {
                if (schedule.getResourceId() == resourceId &&
                    comparator.compare(schedule.getDriftConfiguration(), config) == 0) {
                    return true;
                }
            }
            return false;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public DriftDetectionSchedule remove(int resourceId, DriftConfiguration config) {
        return removeAndExecute(resourceId, config, NO_OP);
    }

    @Override
    public DriftDetectionSchedule removeAndExecute(int resourceId, DriftConfiguration config, Runnable task) {
        try {
            lock.writeLock().lock();
            if (isActiveSchedule(resourceId, config)) {
                deactivationTask = task;
                DriftDetectionSchedule removedSchedule = activeSchedule;
                activeSchedule = null;
                return removedSchedule;
            }

            Iterator<DriftDetectionSchedule> iterator = queue.iterator();
            while (iterator.hasNext()) {
                DriftDetectionSchedule schedule = iterator.next();
                if (schedule.getResourceId() == resourceId
                    && schedule.getDriftConfiguration().getName().equals(config.getName())) {
                    iterator.remove();
                    task.run();
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
        schedule.getDriftConfiguration().setEnabled(config.isEnabled());
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

    /**
     * Generates a string representation of the schedules in the queue. The schedules that
     * appear in the string are in sorted order. If there is an active schedule it will
     * appear first. This method can be useful for debugging since it shows the contents of
     * the queue in sorted order. Use it cautiously however as writes to the queue are
     * blocked until this method returns.
     *
     * @return A string representation of the queue with the schedules appearing in sorted
     * order.
     */
    @Override
    public String toString() {
        try {
            lock.readLock().lock();

            if (activeSchedule == null && queue.isEmpty()) {
                return "ScheduleQueue[]";
            }

            DriftDetectionSchedule[] schedules = toArray();
            Arrays.sort(schedules);

            List<DriftDetectionSchedule> list = new ArrayList<DriftDetectionSchedule>(schedules.length + 1);
            if (activeSchedule != null) {
                list.add(activeSchedule);
            }
            list.addAll(Arrays.asList(schedules));

            StringBuilder buffer = new StringBuilder("ScheduleQueue[");
            for (DriftDetectionSchedule schedule : list) {
                buffer.append(schedule).append(", ");
            }
            int end = buffer.length();
            buffer.delete(end - 2, end);
            buffer.append("]");

            return buffer.toString();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public DriftDetectionSchedule[] toArray() {
        try {
            lock.readLock().lock();
            return queue.toArray(new DriftDetectionSchedule[queue.size()]);
        } finally {
            lock.readLock().unlock();
        }
    }
}
