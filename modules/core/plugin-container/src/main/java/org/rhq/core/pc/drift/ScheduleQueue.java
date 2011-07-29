package org.rhq.core.pc.drift;

public interface ScheduleQueue {

    DriftDetectionSchedule dequeue();

    boolean enqueue(DriftDetectionSchedule schedule);

    void clear();

}
