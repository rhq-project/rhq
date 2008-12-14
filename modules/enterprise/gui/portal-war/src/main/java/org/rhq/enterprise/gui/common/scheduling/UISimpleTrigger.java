/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.gui.common.scheduling;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;

import org.quartz.SimpleTrigger;

import org.rhq.enterprise.gui.common.scheduling.supporting.TimeUnits;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.legacy.WebUserPreferences.DateTimeDisplayPreferences;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;

public class UISimpleTrigger extends UIInput {
    /* to be a valid component */
    public static final String COMPONENT_FAMILY = "org.jboss.on.Trigger";

    public UISimpleTrigger() {
    }

    public UISimpleTrigger(SimpleTrigger trigger) {
        // all scheduled triggers are deferred
        this.setDeferred(true);
        this.setStartDateTime(trigger.getStartTime());

        int repeatCount = trigger.getRepeatCount();
        if (repeatCount != 0) {
            this.setRepeat(true);
            if (repeatCount == SimpleTrigger.REPEAT_INDEFINITELY) {
                this.setRepeatCount(-1);
            } else {
                this.setRepeatCount(repeatCount);
            }
        }

        long repeatMillis = trigger.getRepeatInterval();
        if (repeatMillis != 0) {
            this.setRepeat(true);
            long repeatSecs = repeatMillis / 1000;
            this.setRepeatInterval((int) repeatSecs);
            this.setRepeatUnits(TimeUnits.Seconds);
        }

        // null endDate implies it will trigger on the interval for repeatCount (which includes indefinitely)
        Date endDateTime = trigger.getEndTime();
        if (endDateTime != null) {
            this.setTerminate(true);
            this.setEndDateTime(trigger.getEndTime());
        }
    }

    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    private String dateFormat; // = "MM/dd/yyyy HH:mm:ss";

    /*
     * if no start date/time is specified, this trigger will fire immediately
     */
    private boolean deferred;
    private Date startDateTime;

    /*
     * repeatInterval is a simple integer and repeat units represents the time-based magnitude of this integer
     */
    private boolean repeat;
    private int repeatInterval;
    private TimeUnits repeatUnits;

    /*
     * repeatCount and endDateTime are a mutually exclusive pair. if the repeatInterval is non-null: - if repeatCount is
     * not null specifies how many times the trigger will fire - if endDateTime is not null specifies when the
     * recurrence will end - if both of them are null, this trigger will repeat until deleted - if both are non-null,
     * this trigger will repeat until one of them is true
     */
    private boolean terminate;
    private int repeatCount = -1; // TODO: implement
    private Date endDateTime;

    /*
     * If the date format is never set, it will default to "MM/dd/yyyy HH:mm:ss"
     */
    public String getDateFormat() {
        if (dateFormat == null) {
            WebUser user = EnterpriseFacesContextUtility.getWebUser();
            WebUserPreferences preferences = user.getWebPreferences();
            DateTimeDisplayPreferences dateTimePreferences = preferences.getDateTimeDisplayPreferences();
            dateFormat = dateTimePreferences.dateTimeFormatTrigger;
        }
        return dateFormat;
    }

    /*
     * Follows the same date formatting rules as in
     */
    public void setDateFormat(String dateFormat) {
        Locale locale = FacesContext.getCurrentInstance().getViewRoot().getLocale();

        try {
            // will throw an IllegalArgumentException if the dateFormat is wrong
            new SimpleDateFormat(dateFormat, locale);
        } catch (IllegalArgumentException iae) {
            throw new InvalidTriggerException("dateFormat does not adhere to guideline in java.text.SimpleDateFormat",
                iae);
        }

        // otherwise it's good - save it
        this.dateFormat = dateFormat;
    }

    public boolean getDeferred() {
        return deferred;
    }

    public void setDeferred(boolean deferred) {
        this.deferred = deferred;
    }

    public Date getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(Date startDateTime) {
        this.startDateTime = startDateTime;
    }

    public boolean getRepeat() {
        return repeat;
    }

    public void setRepeat(boolean repeat) {
        this.repeat = repeat;
    }

    public int getRepeatInterval() {
        return repeatInterval;
    }

    public void setRepeatInterval(int repeatInterval) {
        this.repeatInterval = repeatInterval;
    }

    public long getRepeatMillis() {
        return repeatInterval * repeatUnits.getMillis();
    }

    public TimeUnits getRepeatUnits() {
        return repeatUnits;
    }

    public void setRepeatUnits(TimeUnits repeatUnits) {
        this.repeatUnits = repeatUnits;
    }

    public boolean getTerminate() {
        return terminate;
    }

    public void setTerminate(boolean terminate) {
        this.terminate = terminate;
    }

    public int getRepeatCount() {
        return repeatCount;
    }

    public void setRepeatCount(int repeatCount) {
        this.repeatCount = repeatCount;
    }

    public Date getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(Date endDateTime) {
        this.endDateTime = endDateTime;
    }

    public SimpleTrigger getQuartzSimpleTrigger() {
        SimpleTrigger trigger = new SimpleTrigger();
        if (deferred) {
            trigger.setStartTime(startDateTime);
        } else {
            // not deferred, so execute immediately
            Date now = new Date(System.currentTimeMillis());
            trigger.setStartTime(now);
            return trigger;
        }

        if (repeat) {
            if (repeatCount == -1) {
                // repeat forever
                trigger.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
            } else {
                // repeat 'repeatCount' times
                trigger.setRepeatCount(repeatCount);
            }

            trigger.setRepeatInterval(getRepeatMillis());
        } else {
            // not repeating, so execute deferred once
            return trigger;
        }

        if (terminate) {
            if (endDateTime != null) {
                trigger.setEndTime(endDateTime);
            }
        } else {
            // not terminating, so execute deferred forever
            return trigger;
        }

        return trigger;
    }

    @Override
    public Object saveState(FacesContext context) {
        Object[] values = new Object[9];
        values[0] = super.saveState(context);
        values[1] = deferred;
        values[2] = startDateTime;
        values[3] = repeat;
        values[4] = repeatInterval;
        values[5] = repeatUnits;
        values[6] = terminate;
        values[7] = repeatCount;
        values[8] = endDateTime;
        return values;
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object[] values = (Object[]) state;
        super.restoreState(context, values[0]);
        deferred = (Boolean) values[1];
        startDateTime = (Date) values[2];
        repeat = (Boolean) values[3];
        repeatInterval = (Integer) values[4];
        repeatUnits = (TimeUnits) values[5];
        terminate = (Boolean) values[6];
        repeatCount = (Integer) values[7];
        endDateTime = (Date) values[8];
    }
}