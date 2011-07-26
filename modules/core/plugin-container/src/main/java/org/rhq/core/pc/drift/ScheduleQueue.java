package org.rhq.core.pc.drift;

import org.rhq.core.domain.drift.DriftConfiguration;

/**
 * Manages the drift detection schedules that are processed by the drift detector. The
 * queue has a concept of the currently "active" schedule. This is identified simply as
 * the schedule returned from {@link #getNextSchedule()} which is the previous head of the
 * queue. A reference to the active schedule needs to be maintained because at any point in
 * time the server can send a request to update the drift configuration that is attached to
 * a schedule. That schedule will either be on the queue waiting to be processed or in the
 * "active" state meaning it is currently being processed by the drift detector.
 */
public interface ScheduleQueue {

    /**
     * Removes the head of the queue and returns a copy of the schedule that was removed.
     * That schedule is also marked as the active schedule.
     *
     * @return A copy of the schedule that is removed from the head of the queue.
     *
     * @see DriftDetectionSchedule
     *
     * @throws IllegalStateException if there is already an active schedule. The active
     * schedule must be deactivated before getting the next schedule.
     */
    DriftDetectionSchedule getNextSchedule();

    /**
     * This method does two things. First it updates the active schedule's nextScan
     * property. Then it adds the schedule back onto the queue, allowing the next schedule
     * at the head of the queue to become active. If there is no active schedule this
     * method simply does nothing and returns.
     */
    void deactivateSchedule();

    /**
     * Adds a schedule to the queue for processing by the drift detector
     *
     * @param schedule A {@link DriftDetectionSchedule} object
     * @return true if the schedule is added, falsed otherwise
     */
    boolean addSchedule(DriftDetectionSchedule schedule);

    /**
     * This method attempts to update the schedule identified by the resource id the and
     * the drift configuration. More specifically, the schedule is identified by a
     * combination of resource id and drift configuraiton name. If the schedule to be
     * updated is the active schedule, it is immediately updated and then placed back on
     * the queue the next time {@link #deactivateSchedule()} is called. If the schedule
     * is on the queue, it is removed, updated, and then added back onto the queue.
     *
     * @param resourceId The resource id
     * @param config A {@link DriftConfiguration} belonging the resource with the specified id
     * @return true if the schedule is updated, false otherwise.
     */
    boolean update(int resourceId, DriftConfiguration config);

    /**
     * Removes the schedule identified by the resource id and the drift configuration. More
     * specifically, the schedule is identified by a combination of resource id drift
     * configuration name. This method can remove either the active schedule or a schedule
     * on the queue.
     *
     * @param resourceId The resource id
     *
     * @param config A {@link DriftConfiguration} belonging the resource with the specified id
     *
     * @return The {@link DriftDetectionSchedule} that is removed or null if no matching
     * schedule is found.
     */
    DriftDetectionSchedule remove(int resourceId, DriftConfiguration config);

    /**
     * Removes all elements from the queue and deactivates the active schedule.
     */
    void clear();

}
