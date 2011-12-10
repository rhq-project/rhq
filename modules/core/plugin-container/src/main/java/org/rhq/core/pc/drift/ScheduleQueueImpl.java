package org.rhq.core.pc.drift;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.drift.DriftDefinitionComparator;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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

    private Log log = LogFactory.getLog(ScheduleQueueImpl.class);

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

    private boolean isActiveSchedule(int resourceId, DriftDefinition driftDef) {
        return isActiveSchedule(resourceId, driftDef.getName());
    }

    private boolean isActiveSchedule(int resourceId, String defName) {
        try {
            lock.readLock().lock();
            return activeSchedule != null && activeSchedule.getResourceId() == resourceId
                && activeSchedule.getDriftDefinition().getName().equals(defName);
        } finally {
            lock.readLock().unlock();
        }
    }

    private boolean isActiveSchedule(int resourceId, DriftDefinition driftDef, DriftDefinitionComparator comparator) {
        try {
            lock.readLock().lock();
            return activeSchedule != null && activeSchedule.getResourceId() == resourceId
                && comparator.compare(activeSchedule.getDriftDefinition(), driftDef) == 0;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void deactivateSchedule(boolean updateSchedule) {
        try {
            lock.writeLock().lock();
            if (deactivationTask != null) {
                deactivationTask.run();
            }

            if (activeSchedule == null) {
                return;
            }

            if (updateSchedule) {
                activeSchedule.updateShedule();
            }

            if (log.isDebugEnabled()) {
                SimpleDateFormat formatter = new SimpleDateFormat();
                formatter.applyPattern("hh:mm:ss:SSS a");
                log.debug("The next drift detection run for " + activeSchedule + " is set for " +
                        formatter.format(new Date(activeSchedule.getNextScan())));
            }

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
    public boolean contains(int resourceId, DriftDefinition driftDef) {
        return find(resourceId, driftDef.getName()) != null;
    }

    @Override
    public boolean contains(int resourceId, DriftDefinition driftDef, DriftDefinitionComparator comparator) {
        if (isActiveSchedule(resourceId, driftDef, comparator)) {
            return true;
        }
        try {
            lock.readLock().lock();
            for (DriftDetectionSchedule schedule : queue) {
                if (schedule.getResourceId() == resourceId &&
                    comparator.compare(schedule.getDriftDefinition(), driftDef) == 0) {
                    return true;
                }
            }
            return false;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public DriftDetectionSchedule find(int resourceId, String defName) {
        if (isActiveSchedule(resourceId, defName)) {
            return activeSchedule.copy();
        }
        try {
            lock.readLock().lock();
            for (DriftDetectionSchedule schedule : queue) {
                if (schedule.getResourceId() == resourceId
                    && schedule.getDriftDefinition().getName().equals(defName)) {
                    return schedule.copy();
                }
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public DriftDetectionSchedule remove(int resourceId, DriftDefinition driftDef) {
        return remove(resourceId, driftDef.getName());
    }

    @Override
    public DriftDetectionSchedule remove(int resourceId, String defName) {
        return removeAndExecute(resourceId, defName, NO_OP);
    }

    @Override
    public DriftDetectionSchedule removeAndExecute(int resourceId, DriftDefinition driftDef, Runnable task) {
        return removeAndExecute(resourceId, driftDef.getName(), task);
    }

    @Override
    public DriftDetectionSchedule removeAndExecute(int resourceId, String defName, Runnable task) {
        try {
            lock.writeLock().lock();
            if (isActiveSchedule(resourceId, defName)) {
                deactivationTask = task;
                DriftDetectionSchedule removedSchedule = activeSchedule;
                activeSchedule = null;
                return removedSchedule;
            }

            Iterator<DriftDetectionSchedule> iterator = queue.iterator();
            while (iterator.hasNext()) {
                DriftDetectionSchedule schedule = iterator.next();
                if (schedule.getResourceId() == resourceId
                    && schedule.getDriftDefinition().getName().equals(defName)) {
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
    public DriftDetectionSchedule update(int resourceId, DriftDefinition driftDef) {
        DriftDetectionSchedule schedule = remove(resourceId, driftDef);
        if (schedule == null) {
            return null;
        }

        update(schedule, driftDef);

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

    private void update(DriftDetectionSchedule schedule, DriftDefinition driftDef) {
        schedule.getDriftDefinition().setEnabled(driftDef.isEnabled());
        schedule.getDriftDefinition().setInterval(driftDef.getInterval());
        schedule.getDriftDefinition().setDriftHandlingMode(driftDef.getDriftHandlingMode());
        schedule.getDriftDefinition().setPinned(driftDef.isPinned());
        schedule.getDriftDefinition().setComplianceStatus(driftDef.getComplianceStatus());
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
