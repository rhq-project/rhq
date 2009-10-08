package org.rhq.enterprise.gui.common.scheduling;

import java.util.Date;

import org.quartz.SimpleTrigger;

import org.rhq.enterprise.gui.common.scheduling.supporting.TimeUnits;

public class OperationDetailsScheduleComponent {
    private String start = "immediate";
    private Date startDate;
    private Date endDate;
    private String recur = "never";
    private int repeatInterval = 1;
    private TimeUnits unit;
    private String end = "none";
    private boolean readOnly;
    private boolean deferred;
    private boolean repeat;
    private boolean terminate;

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getRecur() {
        return recur;
    }

    public void setRecur(String recur) {
        this.recur = recur;
    }

    public int getRepeatInterval() {
        return repeatInterval;
    }

    public void setRepeatInterval(int repeatInterval) {
        this.repeatInterval = repeatInterval;
    }

    public TimeUnits getUnit() {
        return unit;
    }

    public void setUnit(TimeUnits unit) {
        this.unit = unit;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public void setDeferred(boolean deferred) {
        this.deferred = deferred;
    }

    public boolean getDeferred() {
        this.deferred = start.equals("startDate");
        return this.deferred;
    }

    public void setRepeat(boolean repeat) {
        this.repeat = repeat;
    }

    public boolean getRepeat() {
        this.repeat = recur.equals("recur");
        return this.repeat;
    }

    public void setTerminate(boolean terminate) {
        this.terminate = terminate;
    }

    public boolean getTerminate() {
        this.terminate = end.equals("endDate");
        return this.terminate;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public boolean getReadOnly() {
        return this.readOnly;
    }

    public OperationDetailsScheduleComponent(SimpleTrigger trigger) {
        // all scheduled triggers are deferred
        this.setDeferred(true);
        this.setStartDate(trigger.getStartTime());
        this.setStart("startDate");

        int repeatCount = trigger.getRepeatCount();
        if (repeatCount != 0) {
            this.setRepeat(true);
            if (repeatCount == SimpleTrigger.REPEAT_INDEFINITELY) {
                this.setRecur("never");
                this.setRepeatInterval(1);
            } else {
                this.setRecur("recur");
                this.setRepeatInterval(repeatCount);
            }
        }

        long repeatMillis = trigger.getRepeatInterval();
        if (repeatMillis != 0) {
            this.setRecur("recur");
            this.setRepeat(true);
            long repeatSecs = repeatMillis / 1000;
            this.setRepeatInterval((int) repeatSecs);
            this.setUnit(TimeUnits.Seconds);
        }

        // null endDate implies it will trigger on the interval for repeatCount (which includes indefinitely)
        Date endDateTime = trigger.getEndTime();
        if (endDateTime != null) {
            this.setTerminate(true);
            this.setEndDate(endDateTime);
            this.setEnd("endDate");
        } else {
            this.setEnd("none");
        }
    }

    public OperationDetailsScheduleComponent() {
    }
}
