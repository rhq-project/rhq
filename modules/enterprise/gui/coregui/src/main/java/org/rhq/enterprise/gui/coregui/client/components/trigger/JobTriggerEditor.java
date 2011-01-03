/*
 * RHQ Management Platform
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.rhq.enterprise.gui.coregui.client.components.trigger;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.smartgwt.client.types.Visibility;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.DateTimeItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.RadioGroupItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.BlurEvent;
import com.smartgwt.client.widgets.form.fields.events.BlurHandler;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.fields.events.FocusEvent;
import com.smartgwt.client.widgets.form.fields.events.FocusHandler;
import com.smartgwt.client.widgets.form.validator.RegExpValidator;

import org.rhq.core.domain.common.JobTrigger;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * A SmartGWT widget that provides the ability to create a new {@link JobTrigger job trigger}, or view or edit an
 * existing job trigger.
 *
 * @author Ian Springer
 */
public class JobTriggerEditor extends LocatableVLayout {

    private static final String FIELD_REPEAT_INTERVAL = "repeatInterval";
    private static final String FIELD_REPEAT_DURATION = "repeatDuration";
    private static final String FIELD_END_TIME = "endTime";

    private static final Map<String, Long> UNITS_TO_MILLIS_MULTIPLIER_MAP = new HashMap();
    static {
        UNITS_TO_MILLIS_MULTIPLIER_MAP.put("seconds", 1000L);
        UNITS_TO_MILLIS_MULTIPLIER_MAP.put("s", 1000L);
        UNITS_TO_MILLIS_MULTIPLIER_MAP.put("minutes", 1000L * 60);
        UNITS_TO_MILLIS_MULTIPLIER_MAP.put("m", 1000L * 60);
        UNITS_TO_MILLIS_MULTIPLIER_MAP.put("hours", 1000L * 60 * 60);
        UNITS_TO_MILLIS_MULTIPLIER_MAP.put("h", 1000L * 60 * 60);
        UNITS_TO_MILLIS_MULTIPLIER_MAP.put("days", 1000L * 60 * 60 * 24);
        UNITS_TO_MILLIS_MULTIPLIER_MAP.put("d", 1000L * 60 * 60 * 24);
        UNITS_TO_MILLIS_MULTIPLIER_MAP.put("weeks", 1000L * 60 * 60 * 24 * 7);
        UNITS_TO_MILLIS_MULTIPLIER_MAP.put("w", 1000L * 60 * 60 * 24 * 7);
        UNITS_TO_MILLIS_MULTIPLIER_MAP.put("months", 1000L * 60 * 60 * 24 * 7 * 30);
        UNITS_TO_MILLIS_MULTIPLIER_MAP.put("M", 1000L * 60 * 60 * 24 * 7 * 30);
        UNITS_TO_MILLIS_MULTIPLIER_MAP.put("quarters", 1000L * 60 * 60 * 24 * 7 * 90);
        UNITS_TO_MILLIS_MULTIPLIER_MAP.put("q", 1000L * 60 * 60 * 24 * 7 * 90);
        UNITS_TO_MILLIS_MULTIPLIER_MAP.put("years", 1000L * 60 * 60 * 24 * 7 * 30 * 365);
        UNITS_TO_MILLIS_MULTIPLIER_MAP.put("y", 1000L * 60 * 60 * 24 * 7 * 30 * 365);
    }

    private boolean isReadOnly;

    private DynamicForm laterForm;
    private DynamicForm repeatForm;

    // These flags allow us to determine the trigger type.
    private boolean isCronMode;
    private boolean isStartLater;
    private boolean isRecurring;
    private boolean isRepeatDuration;
    private boolean isEndTime;
    private boolean isStartDelay;
    private boolean isStartTime;
    private static final String FIELD_START_TYPE = "startType";
    private static final String FIELD_START_TIME = "startTime";
    private static final String FIELD_START_DELAY = "startDelay";
    private static final String FIELD_RECURRENCE_TYPE = "recurrenceType";

    /**
     * Create a new job trigger.
     *
     * @param locatorId
     */
    public JobTriggerEditor(String locatorId) {
        super(locatorId);
    }

    /**
     * View or edit an existing job trigger.
     *
     * @param locatorId
     * @param jobTrigger
     * @param isReadOnly
     */
    public JobTriggerEditor(String locatorId, JobTrigger jobTrigger, boolean isReadOnly) {
        super(locatorId);

        this.isReadOnly = isReadOnly;
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        DynamicForm modeForm = new DynamicForm();

        RadioGroupItem modeItem = new RadioGroupItem("mode", "Schedule using");
        modeItem.setWidth(220);
        LinkedHashMap<String, String> modeValueMap = new LinkedHashMap<String, String>();
        modeValueMap.put("calendar", "Calendar");
        modeValueMap.put("cron", "Cron Mode");
        modeItem.setValueMap(modeValueMap);
        modeItem.setVertical(false);

        modeForm.setFields(modeItem);
        addMember(modeForm);


        final DynamicForm calendarTypeForm = new DynamicForm();

        RadioGroupItem calendarTypeItem = new RadioGroupItem("calendarType");
        calendarTypeItem.setWidth(440);
        calendarTypeItem.setShowTitle(false);
        LinkedHashMap<String, String> calendarTypeValueMap = new LinkedHashMap<String, String>();
        calendarTypeValueMap.put("now", "Now");
        calendarTypeValueMap.put("nowAndRepeat", "Now &amp; Repeat");
        calendarTypeValueMap.put("later", "Later");
        calendarTypeValueMap.put("laterAndRepeat", "Later &amp; Repeat");
        calendarTypeItem.setValueMap(calendarTypeValueMap);
        calendarTypeItem.setVertical(false);

        calendarTypeForm.setFields(calendarTypeItem);
        calendarTypeForm.setVisible(false);
        
        addMember(calendarTypeForm);

        modeItem.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent event) {
                if (event.getValue().equals("calendar")) {
                    JobTriggerEditor.this.isCronMode = false;
                    calendarTypeForm.show();
                }
            }
        });

        this.laterForm = createLaterForm();
        addMember(this.laterForm);

        this.repeatForm = createRepeatForm();
        addMember(this.repeatForm);

        calendarTypeItem.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent event) {
                String value = (String)event.getValue();
                if (value.equals("now")) {
                    JobTriggerEditor.this.isStartLater = false;
                    JobTriggerEditor.this.isRecurring = false;
                } else if (value.equals("nowAndRepeat")) {
                    JobTriggerEditor.this.isStartLater = false;
                    JobTriggerEditor.this.isRecurring = true;

                    FormItem repeatIntervalItem = repeatForm.getItem(FIELD_REPEAT_INTERVAL);
                    repeatIntervalItem.setTitle("Run now and every");
                    repeatIntervalItem.redraw();
                } else if (value.equals("later")) {
                    JobTriggerEditor.this.isStartLater = true;
                    JobTriggerEditor.this.isRecurring = false;
                } else {
                    // value.equals("laterAndRepeat")
                    JobTriggerEditor.this.isStartLater = true;
                    JobTriggerEditor.this.isRecurring = true;

                    FormItem repeatIntervalItem = repeatForm.getItem(FIELD_REPEAT_INTERVAL);
                    repeatIntervalItem.setTitle("Repeat every");
                    repeatIntervalItem.redraw();
                }
                laterForm.setVisibility(JobTriggerEditor.this.isStartLater ? Visibility.VISIBLE : Visibility.HIDDEN);
                repeatForm.setVisibility(JobTriggerEditor.this.isRecurring ? Visibility.VISIBLE : Visibility.HIDDEN);
            }
        });
    }

    private DynamicForm createRepeatForm() {
        final DynamicForm repeatForm = new DynamicForm();
        repeatForm.setNumCols(6);
        repeatForm.setColWidths(130, 130, 130, 130, 130);

        TextItem repeatIntervalItem = new TextItem(FIELD_REPEAT_INTERVAL, "Run now and every");
        repeatIntervalItem.setRequired(true);

        // Configure hint.
        repeatIntervalItem.setHint("N UNITS (where N is a positive integer and UNITS is \"seconds\", \"minutes\", \"hours\", \"days\", \"weeks\", \"months\", \"quarters\", or \"years\", e.g. \"30 seconds\" or \"6 weeks\")");
        repeatIntervalItem.setShowHint(false);
        repeatIntervalItem.addFocusHandler(new FocusHandler() {
            public void onFocus(FocusEvent event) {
                repeatForm.setColWidths(130, 400, 130, 130, 130);
                event.getItem().setShowHint(true);
                repeatForm.markForRedraw();
            }
        });
        repeatIntervalItem.addBlurHandler(new BlurHandler() {
            public void onBlur(BlurEvent event) {
                repeatForm.setColWidths(130, 130, 130, 130, 130);
                event.getItem().setShowHint(false);                
                repeatForm.markForRedraw();
            }
        });

        // Configure validation.
        RegExpValidator repeatIntervalValidator = new RegExpValidator("[1-9][0-9]*[ ]*(seconds|s|minutes|m|hours|h|days|d|weeks|w|months|M|quarters|q|years|y)");
        repeatIntervalItem.setValidators(repeatIntervalValidator);
        repeatIntervalItem.setValidateOnExit(true);

        RadioGroupItem recurrenceTypeItem = new RadioGroupItem(FIELD_RECURRENCE_TYPE);
        recurrenceTypeItem.setShowTitle(false);
        LinkedHashMap<String, String> recurrenceTypeValueMap = new LinkedHashMap<String, String>();
        recurrenceTypeValueMap.put("for", "For");
        recurrenceTypeValueMap.put("until", "Until");
        recurrenceTypeValueMap.put("indefinitely", "Indefinitely");
        recurrenceTypeItem.setValueMap(recurrenceTypeValueMap);

        final TextItem repeatDurationItem = new TextItem(FIELD_REPEAT_DURATION);
        repeatDurationItem.setShowTitle(false);
        repeatDurationItem.setVisible(false);

        // Configure hint.
        repeatDurationItem.setHint("N UNITS (where N is a positive integer and UNITS is \"times\", \"seconds\", \"minutes\", \"hours\", \"days\", \"weeks\", \"months\", \"quarters\", or \"years\", e.g. \"30 seconds\" or \"5 repetitions\")");
        repeatDurationItem.setShowHint(false);
        repeatDurationItem.addFocusHandler(new FocusHandler() {
            public void onFocus(FocusEvent event) {
                repeatForm.setColWidths(130, 130, 400, 130, 130);
                event.getItem().setShowHint(true);
                repeatForm.markForRedraw();
            }
        });
        repeatDurationItem.addBlurHandler(new BlurHandler() {
            public void onBlur(BlurEvent event) {
                repeatForm.setColWidths(130, 130, 130, 130, 130);
                event.getItem().setShowHint(false);
                repeatForm.markForRedraw();
            }
        });

        // Configure validation.
        RegExpValidator repeatDurationValidator = new RegExpValidator("[1-9][0-9]*[ ]*(times|repetitions|seconds|s|minutes|m|hours|h|days|d|weeks|w|months|M|quarters|q|years|y)");
        repeatDurationItem.setValidators(repeatDurationValidator);
        repeatDurationItem.setValidateOnExit(true);

        final DateTimeItem endTimeItem = createDateTimeItem(FIELD_END_TIME);
        endTimeItem.setShowTitle(false);
        endTimeItem.setVisible(false);

        SpacerItem spacerItem = new SpacerItem();

        recurrenceTypeItem.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent event) {
                String value = (String)event.getValue();
                if (value.equals("for")) {
                    JobTriggerEditor.this.isEndTime = false;
                    JobTriggerEditor.this.isRepeatDuration = true;
                } else if (value.equals("until")) {
                    JobTriggerEditor.this.isEndTime = true;
                    JobTriggerEditor.this.isRepeatDuration = false;
                } else {
                    // indefinite
                    JobTriggerEditor.this.isEndTime = false;
                    JobTriggerEditor.this.isRepeatDuration = false;
                }

                endTimeItem.setRequired(JobTriggerEditor.this.isEndTime);
                if (JobTriggerEditor.this.isEndTime) {
                    endTimeItem.show();
                } else {
                    endTimeItem.hide();
                }
                repeatDurationItem.setRequired(JobTriggerEditor.this.isRepeatDuration);
                if (JobTriggerEditor.this.isRepeatDuration) {
                    repeatDurationItem.show();
                } else {
                    repeatDurationItem.hide();
                }
            }
        });

        repeatForm.setFields(repeatIntervalItem, recurrenceTypeItem, repeatDurationItem, endTimeItem, spacerItem);
        repeatForm.setVisible(false);

        return repeatForm;
    }

    private DynamicForm createLaterForm() {
        final DynamicForm laterForm = new DynamicForm();
        laterForm.setNumCols(4);
        laterForm.setColWidths(130, 130, 130);

        RadioGroupItem startTypeItem = new RadioGroupItem(FIELD_START_TYPE, "Run");
        LinkedHashMap<String, String> startTypeValueMap = new LinkedHashMap<String, String>();
        startTypeValueMap.put("on", "on");
        startTypeValueMap.put("in", "in");
        startTypeItem.setValueMap(startTypeValueMap);

        final DateTimeItem startTimeItem = createDateTimeItem(FIELD_START_TIME);

        final TextItem startDelayItem = new TextItem(FIELD_START_DELAY);
        startDelayItem.setShowTitle(false);
        startDelayItem.setVisible(false);

        // Configure hint.
        startDelayItem.setHint("N UNITS (where N is a positive integer and UNITS is \"seconds\", \"minutes\", \"hours\", \"days\", \"weeks\", \"months\", \"quarters\", or \"years\", e.g. \"30 seconds\" or \"6 weeks\")");
        startDelayItem.setShowHint(false);
        startDelayItem.addFocusHandler(new FocusHandler() {
            public void onFocus(FocusEvent event) {
                laterForm.setColWidths(130, 130, 400);
                event.getItem().setShowHint(true);
                laterForm.markForRedraw();
            }
        });
        startDelayItem.addBlurHandler(new BlurHandler() {
            public void onBlur(BlurEvent event) {
                laterForm.setColWidths(130, 130, 130);
                event.getItem().setShowHint(false);
                laterForm.markForRedraw();
            }
        });

        // Configure validation.
        RegExpValidator startDelayValidator = new RegExpValidator("[1-9][0-9]*([ ]+(seconds|minutes|hours|days|weeks|months|quarters|years)|(s|m|h|d|w|M|q|y))");
        startDelayItem.setValidators(startDelayValidator);
        startDelayItem.setValidateOnExit(true);

        SpacerItem spacerItem = new SpacerItem();

        startTypeItem.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent event) {
                String value = (String)event.getValue();
                if (value.equals("on")) {
                    JobTriggerEditor.this.isStartDelay = false;
                    JobTriggerEditor.this.isStartTime = true;
                } else {
                    // value.equals("in")
                    JobTriggerEditor.this.isStartDelay = true;
                    JobTriggerEditor.this.isStartTime = false;
                }
                startDelayItem.setRequired(JobTriggerEditor.this.isStartDelay);
                if (JobTriggerEditor.this.isStartDelay) {
                    startDelayItem.show();
                } else {
                    startDelayItem.hide();
                }
                startTimeItem.setRequired(JobTriggerEditor.this.isStartTime);
                if (JobTriggerEditor.this.isStartTime) {
                    startTimeItem.show();
                } else {
                    startTimeItem.hide();
                }
            }
        });

        laterForm.setFields(startTypeItem, startTimeItem, startDelayItem, spacerItem);
        laterForm.setVisible(false);

        return laterForm;
    }

    public JobTrigger getJobTrigger() throws Exception {
        JobTrigger jobTrigger = null;

        if (this.isCronMode) {
            // TODO
        } else {
            // calendar mode

            // Validate first.
            boolean isValid = true;
            if (this.isStartLater) {
                isValid = isValid && this.laterForm.validate();
            }
            if (this.isRecurring) {
                isValid = isValid && this.repeatForm.validate();
            }
            if (!isValid) {
                throw new Exception("The specified schedule is not valid.");
            }

            if (this.isStartLater) {
                Date startDate;
                if (this.isStartDelay) {
                    // start delay - computer start time
                    String startDelay = this.laterForm.getValueAsString(FIELD_START_DELAY);
                    Duration startDelayDuration = parseDurationString(startDelay);
                    long delay = startDelayDuration.count * startDelayDuration.multiplier;
                    long startTime = System.currentTimeMillis() + delay;
                    startDate = new Date(startTime);
                } else {
                    // start time
                    DateTimeItem startTimeItem = (DateTimeItem)this.laterForm.getField(FIELD_START_TIME);
                    startDate = startTimeItem.getValueAsDate();
                }

                if (this.isRecurring) {
                    // LATER AND REPEAT

                    String repeatInterval = this.repeatForm.getValueAsString(FIELD_REPEAT_INTERVAL);
                    Duration intervalDuration = parseDurationString(repeatInterval);
                    long intervalMillis = intervalDuration.count * intervalDuration.multiplier;

                    if (this.isRepeatDuration) {
                        String repeatDurationString = this.repeatForm.getValueAsString(FIELD_REPEAT_DURATION);
                        Duration repeatDuration = parseDurationString(repeatDurationString);
                        if (repeatDuration.multiplier == null) {
                            // n repetitions
                            int repetitions = repeatDuration.count;
                            jobTrigger = JobTrigger.createLaterAndRepeatTrigger(startDate, intervalMillis, repetitions);
                        } else {
                            // n units of time - compute end time
                            long delay = repeatDuration.count * repeatDuration.multiplier;
                            long endTime = System.currentTimeMillis() + delay;
                            Date endDate = new Date(endTime);
                            jobTrigger = JobTrigger.createLaterAndRepeatTrigger(startDate, intervalMillis, endDate);
                        }
                    } else if (this.isEndTime) {
                        DateTimeItem endTimeItem = (DateTimeItem)this.repeatForm.getField(FIELD_END_TIME);
                        Date endDate = endTimeItem.getValueAsDate();
                        jobTrigger = JobTrigger.createLaterAndRepeatTrigger(startDate, intervalMillis, endDate);
                    }
                } else {
                    // LATER

                    jobTrigger = JobTrigger.createLaterTrigger(startDate);
                }
            } else {
                if (this.isRecurring) {
                    // NOW AND REPEAT

                    String repeatInterval = this.repeatForm.getValueAsString(FIELD_REPEAT_INTERVAL);
                    Duration intervalDuration = parseDurationString(repeatInterval);
                    long intervalMillis = intervalDuration.count * intervalDuration.multiplier;

                    if (this.isRepeatDuration) {
                        String repeatDurationString = this.repeatForm.getValueAsString(FIELD_REPEAT_DURATION);
                        Duration repeatDuration = parseDurationString(repeatDurationString);
                        if (repeatDuration.multiplier == null) {
                            // n repetitions
                            int repetitions = repeatDuration.count;
                            jobTrigger = JobTrigger.createNowAndRepeatTrigger(intervalMillis, repetitions);
                        } else {
                            // n units of time - compute end time
                            long delay = repeatDuration.count * repeatDuration.multiplier;
                            long endTime = System.currentTimeMillis() + delay;
                            Date endDate = new Date(endTime);
                            jobTrigger = JobTrigger.createNowAndRepeatTrigger(intervalMillis, endDate);
                        }
                    } else if (this.isEndTime) {
                        DateTimeItem endTimeItem = (DateTimeItem)this.repeatForm.getField(FIELD_END_TIME);
                        Date endDate = endTimeItem.getValueAsDate();
                        jobTrigger = JobTrigger.createNowAndRepeatTrigger(intervalMillis, endDate);
                    }
                } else {
                    // NOW

                    jobTrigger = JobTrigger.createNowTrigger();
                }
            }
        }

        return jobTrigger;
    }

    private static Duration parseDurationString(String durationString) {
        String countString = "";
        int index;
        for (index = 0; index < durationString.length(); index++) {
            char c = durationString.charAt(index);
            if (Character.isDigit(c)) {
                countString += c;
            } else {
                break;
            }
        }
        int count = Integer.valueOf(countString);

        // Skip optional whitespace.
        while (index < durationString.length()) {
            char c = durationString.charAt(index);
            if (c != ' ') {
                break;
            }
            index++;
        }

        String units = durationString.substring(index);
        Long multiplier = UNITS_TO_MILLIS_MULTIPLIER_MAP.get(units.toLowerCase());

        return new Duration(count, multiplier);
    }


    private static DateTimeItem createDateTimeItem(String name) {
        final DateTimeItem dateTimeItem = new DateTimeItem(name);
        dateTimeItem.setEnforceDate(true);
        dateTimeItem.setCenturyThreshold(99);
        dateTimeItem.setShowTitle(false);
        dateTimeItem.setVisible(false);
        dateTimeItem.setStartDate(new Date());
        dateTimeItem.setUseMask(true);
        dateTimeItem.setShowHint(true);
        return dateTimeItem;
    }

    public boolean isReadOnly() {
        return isReadOnly;
    }

    private static class Duration {
        public int count;
        // a null multiplier means the count refers repetitions, rather than units of time
        public Long multiplier;

        private Duration(int count, Long multiplier) {
            this.count = count;
            this.multiplier = multiplier;
        }
    }
}
