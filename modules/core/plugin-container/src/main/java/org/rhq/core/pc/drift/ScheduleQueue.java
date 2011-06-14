package org.rhq.core.pc.drift;

public interface ScheduleQueue {

    DriftDetectionSchedule nextSchedule();

}
