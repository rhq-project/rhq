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
import java.util.LinkedHashMap;

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

    private boolean isReadOnly;
    private boolean isValid;

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
                     calendarTypeForm.show();
                }
            }
        });

        final DynamicForm laterForm = createLaterForm();
        addMember(laterForm);

        final DynamicForm repeatForm = createRepeatForm();
        addMember(repeatForm);

        calendarTypeItem.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent event) {
                String value = (String)event.getValue();
                if (value.equals("now")) {
                    isValid = true;
                } else if (value.equals("nowAndRepeat")) {
                    laterForm.hide();
                    FormItem repeatIntervalItem = repeatForm.getItem("repeatInterval");
                    repeatIntervalItem.setTitle("Run now and every");
                    repeatIntervalItem.redraw();
                    repeatForm.show();
                } else if (value.equals("later")) {
                    repeatForm.hide();
                    laterForm.show();
                } else {
                    // later and repeat
                    laterForm.show();
                    FormItem repeatIntervalItem = repeatForm.getItem("repeatInterval");
                    repeatIntervalItem.setTitle("Repeat every");
                    repeatIntervalItem.redraw();
                    repeatForm.show();
                }
            }
        });
    }

    private DynamicForm createRepeatForm() {
        final DynamicForm nowAndRepeatForm = new DynamicForm();
        nowAndRepeatForm.setNumCols(6);
        nowAndRepeatForm.setColWidths(130, 130, 130, 130, 130);

        TextItem repeatIntervalItem = new TextItem("repeatInterval", "Run now and every");

        // Configure hint.
        repeatIntervalItem.setHint("[1-9][0-9]* (seconds|minutes|hours|days|weeks|months|quarters|years)");
        repeatIntervalItem.setShowHint(false);
        repeatIntervalItem.addFocusHandler(new FocusHandler() {
            public void onFocus(FocusEvent event) {
                event.getItem().setShowHint(true);
            }
        });
        repeatIntervalItem.addBlurHandler(new BlurHandler() {
            public void onBlur(BlurEvent event) {
                event.getItem().setShowHint(false);
            }
        });

        // Configure validation.
        RegExpValidator repeatIntervalValidator = new RegExpValidator("[1-9][0-9]*[ ]+(seconds|minutes|hours|days|weeks|months|quarters|years)");
        repeatIntervalItem.setValidators(repeatIntervalValidator);
        repeatIntervalItem.setValidateOnExit(true);

        RadioGroupItem recurrenceTypeItem = new RadioGroupItem("recurrenceType");
        recurrenceTypeItem.setShowTitle(false);
        LinkedHashMap<String, String> recurrenceTypeValueMap = new LinkedHashMap<String, String>();
        recurrenceTypeValueMap.put("for", "For");
        recurrenceTypeValueMap.put("until", "Until");
        recurrenceTypeValueMap.put("indefinitely", "Indefinitely");
        recurrenceTypeItem.setValueMap(recurrenceTypeValueMap);

        final TextItem repeatDurationItem = new TextItem("repeatDuration");
        repeatDurationItem.setShowTitle(false);
        repeatDurationItem.setVisible(false);

        // Configure hint.
        repeatDurationItem.setHint("[1-9][0-9]* (repetitions|seconds|minutes|hours|days|weeks|months|quarters|years)");
        repeatDurationItem.setShowHint(false);
        repeatDurationItem.addFocusHandler(new FocusHandler() {
            public void onFocus(FocusEvent event) {
                event.getItem().setShowHint(true);
            }
        });
        repeatDurationItem.addBlurHandler(new BlurHandler() {
            public void onBlur(BlurEvent event) {
                event.getItem().setShowHint(false);
            }
        });

        // Configure validation.
        RegExpValidator repeatDurationValidator = new RegExpValidator("[1-9][0-9]*[ ]+(repetitions|seconds|minutes|hours|days|weeks|months|quarters|years)");
        repeatDurationItem.setValidators(repeatDurationValidator);
        repeatDurationItem.setValidateOnExit(true);


        final DateTimeItem endTimeItem = createDateTimeItem("endTime");
        endTimeItem.setShowTitle(false);
        endTimeItem.setVisible(false);

        SpacerItem spacerItem = new SpacerItem();

        recurrenceTypeItem.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent event) {
                String value = (String)event.getValue();
                if (value.equals("for")) {
                    endTimeItem.hide();
                    repeatDurationItem.show();
                } else if (value.equals("until")) {
                    repeatDurationItem.hide();
                    endTimeItem.show();
                } else {
                    repeatDurationItem.hide();
                    endTimeItem.hide();
                }
            }
        });

        nowAndRepeatForm.setFields(repeatIntervalItem, recurrenceTypeItem, repeatDurationItem, endTimeItem, spacerItem);
        nowAndRepeatForm.setVisible(false);

        return nowAndRepeatForm;
    }

    private DynamicForm createLaterForm() {
        final DynamicForm laterForm = new DynamicForm();
        laterForm.setNumCols(4);
        laterForm.setColWidths(130, 130, 130);

        RadioGroupItem startTypeItem = new RadioGroupItem("startType", "Run");
        LinkedHashMap<String, String> startTypeValueMap = new LinkedHashMap<String, String>();
        startTypeValueMap.put("on", "on");
        startTypeValueMap.put("in", "in");
        startTypeItem.setValueMap(startTypeValueMap);

        final DateTimeItem startTimeItem = createDateTimeItem("startTime");

        final TextItem startDelayItem = new TextItem("startDelay");
        startDelayItem.setShowTitle(false);
        startDelayItem.setVisible(false);

        // Configure hint.
        startDelayItem.setHint("[1-9][0-9]* (seconds|minutes|hours|days|weeks|months|quarters|years)");
        startDelayItem.setShowHint(false);
        startDelayItem.addFocusHandler(new FocusHandler() {
            public void onFocus(FocusEvent event) {
                event.getItem().setShowHint(true);
            }
        });
        startDelayItem.addBlurHandler(new BlurHandler() {
            public void onBlur(BlurEvent event) {
                event.getItem().setShowHint(false);
            }
        });

        // Configure validation.
        RegExpValidator startDelayValidator = new RegExpValidator("[1-9][0-9]*[ ]+(seconds|minutes|hours|days|weeks|months|quarters|years)");
        startDelayItem.setValidators(startDelayValidator);
        startDelayItem.setValidateOnExit(true);

        SpacerItem spacerItem = new SpacerItem();

        startTypeItem.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent event) {
                String value = (String)event.getValue();
                if (value.equals("on")) {
                    startDelayItem.hide();
                    startTimeItem.show();
                } else {
                    startTimeItem.hide();
                    startDelayItem.show();
                }
            }
        });

        laterForm.setFields(startTypeItem, startTimeItem, startDelayItem, spacerItem);
        laterForm.setVisible(false);

        return laterForm;
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

    public boolean isValid() {
        return isValid;
    }
    
}
