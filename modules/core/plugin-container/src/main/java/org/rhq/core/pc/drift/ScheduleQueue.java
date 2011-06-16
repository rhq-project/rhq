package org.rhq.core.pc.drift;

public interface ScheduleQueue {

    DriftDetectionSchedule nextSchedule();

    boolean offer(DriftDetectionSchedule schedule);

}
