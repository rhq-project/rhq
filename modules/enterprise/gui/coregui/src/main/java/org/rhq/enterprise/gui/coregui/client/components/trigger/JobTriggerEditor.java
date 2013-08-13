/*
 * RHQ Management Platform
 * Copyright 2010-2011, Red Hat Middleware LLC, and individual contributors
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
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.TreeSet;

import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.DateTimeItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.FormItemIcon;
import com.smartgwt.client.widgets.form.fields.RadioGroupItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.fields.events.IconClickEvent;
import com.smartgwt.client.widgets.form.fields.events.IconClickHandler;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;

import org.rhq.core.domain.common.JobTrigger;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.form.DurationItem;
import org.rhq.enterprise.gui.coregui.client.components.form.EnhancedDynamicForm;
import org.rhq.enterprise.gui.coregui.client.components.form.TimeUnit;
import org.rhq.enterprise.gui.coregui.client.components.form.UnitType;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * A SmartGWT widget that provides the ability to create a new {@link JobTrigger job trigger}, or view or edit an
 * existing job trigger.
 *
 * @author Ian Springer
 */
public class JobTriggerEditor extends EnhancedVLayout {

    // Field Names
    private static final String FIELD_MODE = "mode";
    private static final String FIELD_REPEAT_INTERVAL = "repeatInterval";
    private static final String FIELD_REPEAT_DURATION = "repeatDuration";
    private static final String FIELD_END_TIME = "endTime";
    private static final String FIELD_START_TYPE = "startType";
    private static final String FIELD_START_TIME = "startTime";
    private static final String FIELD_START_DELAY = "startDelay";
    private static final String FIELD_RECURRENCE_TYPE = "recurrenceType";
    private static final String FIELD_CRON_EXPRESSION = "cronExpression";

    private JobTrigger jobTrigger;
    private boolean isReadOnly;

    private EnhancedVLayout calendarModeLayout;
    private EnhancedVLayout cronModeLayout;

    private EnhancedDynamicForm modeForm;
    private EnhancedDynamicForm calendarTypeForm;
    private DynamicForm laterForm;
    private DynamicForm repeatForm;
    private DynamicForm cronForm;

    // These flags allow us to determine the trigger type.
    private boolean isStartLater;
    private boolean isRecurring;
    private boolean isRepeatDuration;
    private boolean isEndTime;
    private boolean isStartDelay;
    private boolean isStartTime;

    public JobTriggerEditor(boolean isReadOnly) {
        super();

        this.isReadOnly = isReadOnly;
    }

    /**
     * View or edit an existing job trigger.
     *
     * @param jobTrigger
     */
    public JobTriggerEditor(JobTrigger jobTrigger) {
        super();

        this.jobTrigger = jobTrigger;
        this.isReadOnly = true;
    }

    public void setJobTrigger(JobTrigger jobTrigger) {
        this.jobTrigger = jobTrigger;
        this.isReadOnly = true;
        if (isDrawn()) {
            refresh();
        }
    }

    private void refresh() {
        if (this.jobTrigger != null) {
            FormItem modeItem = this.modeForm.getItem(FIELD_MODE);
            if (this.jobTrigger.getRecurrenceType() == JobTrigger.RecurrenceType.CRON_EXPRESSION) {
                modeItem.setValue("cron");
                changeMode("cron");
                this.cronForm.setValue("cronExpression", this.jobTrigger.getCronExpression());
            } else {
                modeItem.setValue("calendar");
                this.calendarTypeForm.hide();
                changeMode("calendar");

                FormItem startTypeItem = this.laterForm.getItem(FIELD_START_TYPE);
                startTypeItem.setValue("on");
                DurationItem startDelayItem = (DurationItem) this.laterForm.getItem(FIELD_START_DELAY);
                FormItem startTimeItem = this.laterForm.getField(FIELD_START_TIME);
                changeStartType("on", startDelayItem, startTimeItem);
                startTimeItem.setValue(this.jobTrigger.getStartDate());

                FormItem calendarTypeItem = this.calendarTypeForm.getField("calendarType");
                if (this.jobTrigger.getRecurrenceType() == JobTrigger.RecurrenceType.REPEAT_INTERVAL) {
                    calendarTypeItem.setValue("laterAndRepeat");
                    changeCalendarType("laterAndRepeat");

                    DurationItem repeatIntervalItem = (DurationItem) this.repeatForm.getItem(FIELD_REPEAT_INTERVAL);
                    repeatIntervalItem.setAndFormatValue(this.jobTrigger.getRepeatInterval());

                    FormItem endTimeItem = this.repeatForm.getField(FIELD_END_TIME);
                    DurationItem repeatDurationItem = (DurationItem) this.repeatForm.getItem(FIELD_REPEAT_DURATION);
                    FormItem recurrenceTypeItem = this.repeatForm.getField(FIELD_RECURRENCE_TYPE);
                    if (this.jobTrigger.getRepeatCount() != null) {
                        recurrenceTypeItem.setValue("for");
                        changeRecurrenceType("for", endTimeItem, repeatDurationItem);
                        repeatDurationItem.setValue(this.jobTrigger.getRepeatCount(), UnitType.ITERATIONS);
                    } else if (this.jobTrigger.getEndDate() != null) {
                        recurrenceTypeItem.setValue("until");
                        changeRecurrenceType("until", endTimeItem, repeatDurationItem);
                        endTimeItem.setValue(this.jobTrigger.getEndDate());
                    } else {
                        recurrenceTypeItem.setValue("indefinitely");
                        changeRecurrenceType("indefinitely", endTimeItem, repeatDurationItem);
                    }
                } else {
                    calendarTypeItem.setValue("later");
                    changeCalendarType("later");
                }
            }
        }
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        this.modeForm = new EnhancedDynamicForm(this.isReadOnly);
        this.modeForm.setNumCols(3);
        this.modeForm.setColWidths("140", "220", "*");

        RadioGroupItem modeItem = new RadioGroupItem(FIELD_MODE, MSG.widget_jobTriggerEditor_field_mode());
        LinkedHashMap<String, String> modeValueMap = new LinkedHashMap<String, String>();
        modeValueMap.put("calendar", MSG.widget_jobTriggerEditor_value_calendar());
        modeValueMap.put("cron", MSG.widget_jobTriggerEditor_value_cronExpression());
        modeItem.setValueMap(modeValueMap);
        modeItem.setVertical(false);
        modeItem.setShowTitle(true);
        modeItem.setValue("calendar");

        this.modeForm.setFields(modeItem);
        addMember(this.modeForm);

        this.calendarModeLayout = new EnhancedVLayout();

        this.calendarTypeForm = new EnhancedDynamicForm(this.isReadOnly);

        RadioGroupItem calendarTypeItem = new RadioGroupItem("calendarType");
        calendarTypeItem.setWidth(440);
        calendarTypeItem.setShowTitle(false);
        LinkedHashMap<String, String> calendarTypeValueMap = new LinkedHashMap<String, String>();
        calendarTypeValueMap.put("now", MSG.widget_jobTriggerEditor_value_now());
        calendarTypeValueMap.put("nowAndRepeat", MSG.widget_jobTriggerEditor_value_nowAndRepeat());
        calendarTypeValueMap.put("later", MSG.widget_jobTriggerEditor_value_later());
        calendarTypeValueMap.put("laterAndRepeat", MSG.widget_jobTriggerEditor_value_laterAndRepeat());
        calendarTypeItem.setValueMap(calendarTypeValueMap);
        calendarTypeItem.setVertical(false);
        calendarTypeItem.setValue("now");

        this.calendarTypeForm.setFields(calendarTypeItem);

        this.calendarModeLayout.addMember(this.calendarTypeForm);
        addMember(this.calendarModeLayout);

        this.cronModeLayout = new EnhancedVLayout();
        this.cronModeLayout.setVisible(false);

        this.cronForm = new DynamicForm();

        TextItem cronExpressionItem = new TextItem(FIELD_CRON_EXPRESSION,
            MSG.widget_jobTriggerEditor_field_cronExpression());
        cronExpressionItem.setRequired(true);
        cronExpressionItem.setWidth(340);

        this.cronForm.setFields(cronExpressionItem);

        this.cronModeLayout.addMember(this.cronForm);

        final TabSet cronHelpTabSet = new TabSet();
        cronHelpTabSet.setWidth100();
        cronHelpTabSet.setHeight(200);
        Img closeIcon = new Img("[SKIN]/headerIcons/close.png", 16, 16);
        closeIcon.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                cronHelpTabSet.hide();
            }
        });
        cronHelpTabSet.setTabBarControls(closeIcon);

        Tab formatTab = new Tab(MSG.widget_jobTriggerEditor_tab_format());
        HTMLFlow formatPane = new HTMLFlow();
        formatPane.setWidth100();
        formatPane
            .setContents("<p>A cron expression is a string comprised of 6 or 7 fields separated by white space. Fields can contain any of the\n"
                + "allowed values, along with various combinations of the allowed special characters for that field. The fields are as\n"
                + "follows:</p>\n"
                + "<table cellpadding=\"3\" cellspacing=\"1\">\n"
                + "    <tbody>\n"
                + "\n"
                + "        <tr>\n"
                + "            <th>Field Name</th>\n"
                + "            <th>Mandatory</th>\n"
                + "            <th>Allowed Values</th>\n"
                + "            <th>Allowed Special Characters</th>\n"
                + "        </tr>\n"
                + "        <tr>\n"
                + "\n"
                + "            <td>Seconds</td>\n"
                + "            <td>YES</td>\n"
                + "\n"
                + "            <td>0-59</td>\n"
                + "            <td>, - * /</td>\n"
                + "        </tr>\n"
                + "        <tr>\n"
                + "\n"
                + "            <td>Minutes</td>\n"
                + "            <td>YES</td>\n"
                + "            <td>0-59</td>\n"
                + "\n"
                + "            <td>, - * /</td>\n"
                + "        </tr>\n"
                + "        <tr>\n"
                + "\n"
                + "            <td>Hours</td>\n"
                + "            <td>YES</td>\n"
                + "            <td>0-23</td>\n"
                + "            <td>, - * /</td>\n"
                + "\n"
                + "        </tr>\n"
                + "        <tr>\n"
                + "\n"
                + "            <td>Day of month</td>\n"
                + "            <td>YES</td>\n"
                + "            <td>1-31</td>\n"
                + "            <td>, - * ? / L W<br clear=\"all\" />\n"
                + "            </td>\n"
                + "        </tr>\n"
                + "        <tr>\n"
                + "\n"
                + "            <td>Month</td>\n"
                + "            <td>YES</td>\n"
                + "            <td>1-12 or JAN-DEC</td>\n"
                + "            <td>, - * /</td>\n"
                + "        </tr>\n"
                + "        <tr>\n"
                + "\n"
                + "            <td>Day of week</td>\n"
                + "\n"
                + "            <td>YES</td>\n"
                + "            <td>1-7 or SUN-SAT</td>\n"
                + "            <td>, - * ? / L #</td>\n"
                + "        </tr>\n"
                + "        <tr>\n"
                + "\n"
                + "            <td>Year</td>\n"
                + "            <td>NO</td>\n"
                + "\n"
                + "            <td>empty, 1970-2099</td>\n"
                + "            <td>, - * /</td>\n"
                + "        </tr>\n"
                + "    </tbody>\n"
                + "\n"
                + "</table>\n"
                + "<p>So cron expressions can be as simple as this: <tt>0 * * ? * *</tt> to run every minute on the minute<br />\n"
                + "or more complex, like this: <tt>0/5 14,18,3-39,52 * ? JAN,MAR,SEP MON-FRI 2002-2015</tt></p>\n"
                + "\n"
                + "<h2><a name=\"CronTriggersTutorial-Specialcharacters\"></a>Special Characters</h2>\n"
                + "\n"
                + "<ul>\n"
                + "    <li><tt><b>&#42;</b></tt> (<em>\"all values\"</em>) - used to select all values within a field. For example, \"*\"\n"
                + "    in the minute field means <em>\"every minute\"</em>.</li>\n"
                + "\n"
                + "</ul>\n"
                + "\n"
                + "\n"
                + "<ul>\n"
                + "    <li><tt><b>?</b></tt> (<em>\"no specific value\"</em>) - useful when you need to specify something in one of the\n"
                + "    two fields in which the character is allowed, but not the other. For example, if I want my trigger to fire on a\n"
                + "    particular day of the month (say, the 10th), but don't care what day of the week that happens to be, I would put\n"
                + "    \"10\" in the day-of-month field, and \"?\" in the day-of-week field. See the examples below for clarification.</li>\n"
                + "\n"
                + "</ul>\n"
                + "\n"
                + "\n"
                + "<ul>\n"
                + "    <li><tt><b>&#45;</b></tt> &#45; used to specify ranges. For example, \"10-12\" in the hour field means <em>\"the\n"
                + "    hours 10, 11 and 12\"</em>.</li>\n"
                + "\n"
                + "</ul>\n"
                + "\n"
                + "\n"
                + "<ul>\n"
                + "    <li><tt><b>,</b></tt> &#45; used to specify additional values. For example, \"MON,WED,FRI\" in the day-of-week\n"
                + "    field means <em>\"the days Monday, Wednesday, and Friday\"</em>.</li>\n"
                + "\n"
                + "</ul>\n"
                + "\n"
                + "\n"
                + "<ul>\n"
                + "\n"
                + "    <li><tt><b>/</b></tt> &#45; used to specify increments. For example, \"0/15\" in the seconds field means <em>\"the\n"
                + "    seconds 0, 15, 30, and 45\"</em>. And \"5/15\" in the seconds field means <em>\"the seconds 5, 20, 35, and 50\"</em>. You can\n"
                + "    also specify '/' after the '<b>' character - in this case '</b>' is equivalent to having '0' before the '/'. '1/3'\n"
                + "    in the day-of-month field means <em>\"fire every 3 days starting on the first day of the month\"</em>.</li>\n"
                + "\n"
                + "</ul>\n"
                + "\n"
                + "<ul>\n"
                + "    <li><tt><b>L</b></tt> (<em>\"last\"</em>) - has different meaning in each of the two fields in which it is\n"
                + "    allowed. For example, the value \"L\" in the day-of-month field means <em>\"the last day of the month\"</em> &#45; day\n"
                + "    31 for January, day 28 for February on non-leap years. If used in the day-of-week field by itself, it simply means\n"
                + "    \"7\" or \"SAT\". But if used in the day-of-week field after another value, it means <em>\"the last xxx day of the\n"
                + "    month\"</em> &#45; for example \"6L\" means <em>\"the last friday of the month\"</em>. When using the 'L' option, it is\n"
                + "    important not to specify lists, or ranges of values, as you'll get confusing results.</li>\n"
                + "\n"
                + "</ul>\n"
                + "\n"
                + "\n"
                + "<ul>\n"
                + "    <li><tt><b>W</b></tt> (<em>\"weekday\"</em>) - used to specify the weekday (Monday-Friday) nearest the given day.\n"
                + "    As an example, if you were to specify \"15W\" as the value for the day-of-month field, the meaning is: <em>\"the\n"
                + "    nearest weekday to the 15th of the month\"</em>. So if the 15th is a Saturday, the trigger will fire on Friday the 14th.\n"
                + "    If the 15th is a Sunday, the trigger will fire on Monday the 16th. If the 15th is a Tuesday, then it will fire on\n"
                + "    Tuesday the 15th. However if you specify \"1W\" as the value for day-of-month, and the 1st is a Saturday, the trigger\n"
                + "    will fire on Monday the 3rd, as it will not 'jump' over the boundary of a month's days. The 'W' character can only\n"
                + "    be specified when the day-of-month is a single day, not a range or list of days.\n"
                + "        <div class=\"tip\">\n"
                + "            The 'L' and 'W' characters can also be combined in the day-of-month field to yield 'LW', which\n"
                + "            translates to <em>\"last weekday of the month\"</em>.\n"
                + "        </div>\n"
                + "\n"
                + "    </li>\n"
                + "\n"
                + "    <li><tt><b>&#35;</b></tt> &#45; used to specify \"the nth\" XXX day of the month. For example, the value of \"6#3\"\n"
                + "    in the day-of-week field means <em>\"the third Friday of the month\"</em> (day 6 = Friday and \"#3\" = the 3rd one in\n"
                + "    the month). Other examples: \"2#1\" = the first Monday of the month and \"4#5\" = the fifth Wednesday of the month. Note\n"
                + "    that if you specify \"#5\" and there is not 5 of the given day-of-week in the month, then no firing will occur that\n"
                + "    month.\n"
                + "        <div class=\"tip\">\n"
                + "            The legal characters and the names of months and days of the week are not case sensitive. <tt>MON</tt>\n"
                + "            is the same as <tt>mon</tt>.\n"
                + "        </div>\n"
                + "\n"
                + "    </li>\n"
                + "</ul>"
                + "<h2><a name=\"CronTriggersTutorial-Notes\"></a>Notes</h2>\n"
                + "\n"
                + "<ul>\n"
                + "    <li>Support for specifying both a day-of-week and a day-of-month value is not complete (you must currently use\n"
                + "    the '?' character in one of these fields).</li>\n"
                + "    <li>Be careful when setting fire times between mid-night and 1:00 AM - \"daylight savings\" can cause a skip or a\n"
                + "    repeat depending on whether the time moves back or jumps forward.</li>\n" + "\n" + "</ul>");
        formatTab.setPane(formatPane);

        Tab examplesTab = new Tab(MSG.widget_jobTriggerEditor_tab_examples());
        HTMLFlow examplesPane = new HTMLFlow();
        examplesPane.setWidth100();
        examplesPane.setContents("<table cellpadding=\"3\" cellspacing=\"1\">\n" + "    <tbody>\n" + "        <tr>\n"
            + "            <th>Expression</th>\n" + "\n" + "            <th>Meaning</th>\n" + "        </tr>\n"
            + "        <tr>\n" + "            <td><tt>0 0 12 * * ?</tt></td>\n" + "\n"
            + "            <td>Fire at 12pm (noon) every day</td>\n" + "        </tr>\n" + "        <tr>\n" + "\n"
            + "            <td><tt>0 15 10 ? * *</tt></td>\n" + "            <td>Fire at 10:15am every day</td>\n"
            + "        </tr>\n" + "        <tr>\n" + "            <td><tt>0 15 10 * * ?</tt></td>\n" + "\n"
            + "            <td>Fire at 10:15am every day</td>\n" + "\n" + "        </tr>\n" + "        <tr>\n"
            + "            <td><tt>0 15 10 * * ? *</tt></td>\n" + "            <td>Fire at 10:15am every day</td>\n"
            + "        </tr>\n" + "        <tr>\n" + "            <td><tt>0 15 10 * * ? 2005</tt></td>\n" + "\n"
            + "            <td>Fire at 10:15am every day during the year 2005</td>\n" + "        </tr>\n"
            + "        <tr>\n" + "            <td><tt>0 * 14 * * ?</tt></td>\n"
            + "            <td>Fire every minute starting at 2pm and ending at 2:59pm, every day</td>\n"
            + "        </tr>\n" + "        <tr>\n" + "\n" + "            <td><tt>0 0/5 14 * * ?</tt></td>\n" + "\n"
            + "            <td>Fire every 5 minutes starting at 2pm and ending at 2:55pm, every day</td>\n"
            + "        </tr>\n" + "        <tr>\n" + "            <td><tt>0 0/5 14,18 * * ?</tt></td>\n"
            + "            <td>Fire every 5 minutes starting at 2pm and ending at 2:55pm, AND fire every 5\n"
            + "            minutes starting at 6pm and ending at 6:55pm, every day</td>\n" + "\n" + "        </tr>\n"
            + "        <tr>\n" + "            <td><tt>0 0-5 14 * * ?</tt></td>\n" + "\n"
            + "            <td>Fire every minute starting at 2pm and ending at 2:05pm, every day</td>\n"
            + "        </tr>\n" + "        <tr>\n" + "            <td><tt>0 10,44 14 ? 3 WED</tt></td>\n" + "\n"
            + "            <td>Fire at 2:10pm and at 2:44pm every Wednesday in the month of March.</td>\n"
            + "        </tr>\n" + "        <tr>\n" + "            <td><tt>0 15 10 ? * MON-FRI</tt></td>\n" + "\n"
            + "            <td>Fire at 10:15am every Monday, Tuesday, Wednesday, Thursday and Friday</td>\n"
            + "        </tr>\n" + "        <tr>\n" + "\n" + "            <td><tt>0 15 10 15 * ?</tt></td>\n"
            + "            <td>Fire at 10:15am on the 15th day of every month</td>\n" + "        </tr>\n"
            + "        <tr>\n" + "            <td><tt>0 15 10 L * ?</tt></td>\n" + "\n"
            + "            <td>Fire at 10:15am on the last day of every month</td>\n" + "\n" + "        </tr>\n"
            + "        <tr>\n" + "            <td><tt>0 15 10 ? * 6L</tt></td>\n"
            + "            <td>Fire at 10:15am on the last Friday of every month</td>\n" + "        </tr>\n"
            + "        <tr>\n" + "            <td><tt>0 15 10 ? * 6L</tt></td>\n" + "\n"
            + "            <td>Fire at 10:15am on the last Friday of every month</td>\n" + "        </tr>\n"
            + "        <tr>\n" + "            <td><tt>0 15 10 ? * 6L 2002-2005</tt></td>\n"
            + "            <td>Fire at 10:15am on every last friday of every month during the years 2002,\n"
            + "            2003, 2004 and 2005</td>\n" + "        </tr>\n" + "        <tr>\n" + "\n"
            + "            <td><tt>0 15 10 ? * 6#3</tt></td>\n" + "\n"
            + "            <td>Fire at 10:15am on the third Friday of every month</td>\n" + "        </tr>\n"
            + "        <tr>\n" + "            <td><tt>0 0 12 1/5 * ?</tt></td>\n"
            + "            <td>Fire at 12pm (noon) every 5 days every month, starting on the first day of the\n"
            + "            month.</td>\n" + "\n" + "        </tr>\n" + "        <tr>\n"
            + "            <td><tt>0 11 11 11 11 ?</tt></td>\n" + "\n"
            + "            <td>Fire every November 11th at 11:11am.</td>\n" + "        </tr>\n" + "    </tbody>\n"
            + "</table>");
        examplesTab.setPane(examplesPane);

        cronHelpTabSet.addTab(formatTab);
        cronHelpTabSet.addTab(examplesTab);

        cronHelpTabSet.setVisible(false);

        FormItemIcon helpIcon = new FormItemIcon();
        helpIcon.setSrc("[SKIN]/actions/help.png");
        cronExpressionItem.setIcons(helpIcon);
        cronExpressionItem.addIconClickHandler(new IconClickHandler() {
            public void onIconClick(IconClickEvent event) {
                cronHelpTabSet.show();
            }
        });

        this.cronModeLayout.addMember(cronHelpTabSet);
        addMember(this.cronModeLayout);

        modeItem.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent event) {
                String mode = (String) event.getValue();
                changeMode(mode);
            }
        });

        this.laterForm = createLaterForm();
        this.calendarModeLayout.addMember(this.laterForm);

        this.repeatForm = createRepeatForm();
        this.calendarModeLayout.addMember(this.repeatForm);

        calendarTypeItem.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent event) {
                String calendarType = (String) event.getValue();
                changeCalendarType(calendarType);
            }
        });

        refresh();
    }

    private void changeCalendarType(String calendarType) {
        if (calendarType.equals("now")) {
            this.isStartLater = false;
            this.isRecurring = false;
        } else if (calendarType.equals("nowAndRepeat")) {
            this.isStartLater = false;
            this.isRecurring = true;

            FormItem repeatIntervalItem = repeatForm.getItem(FIELD_REPEAT_INTERVAL);
            repeatIntervalItem.setTitle(MSG.widget_jobTriggerEditor_field_repeatInterval_now());
            repeatIntervalItem.redraw();
        } else if (calendarType.equals("later")) {
            this.isStartLater = true;
            this.isRecurring = false;
        } else {
            // value.equals("laterAndRepeat")
            this.isStartLater = true;
            this.isRecurring = true;

            FormItem repeatIntervalItem = repeatForm.getItem(FIELD_REPEAT_INTERVAL);
            repeatIntervalItem.setTitle(MSG.widget_jobTriggerEditor_field_repeatInterval_later());
            repeatIntervalItem.redraw();
        }
        if (isStartLater) {
            laterForm.show();
        } else {
            laterForm.hide();
        }
        if (isRecurring) {
            repeatForm.show();
        } else {
            repeatForm.hide();
        }
    }

    private void changeMode(String mode) {
        if (mode.equals("calendar")) {
            calendarModeLayout.show();
            cronModeLayout.hide();
        } else {
            // cron expression mode
            calendarModeLayout.hide();
            cronModeLayout.show();
        }
    }

    private DynamicForm createRepeatForm() {
        final EnhancedDynamicForm repeatForm = new EnhancedDynamicForm(this.isReadOnly);
        repeatForm.setNumCols(6);
        repeatForm.setColWidths(140, 130, 130, 130, 130);

        TreeSet<TimeUnit> supportedUnits = new TreeSet<TimeUnit>();
        supportedUnits.add(TimeUnit.SECONDS);
        supportedUnits.add(TimeUnit.MINUTES);
        supportedUnits.add(TimeUnit.HOURS);
        supportedUnits.add(TimeUnit.DAYS);
        supportedUnits.add(TimeUnit.WEEKS);
        supportedUnits.add(TimeUnit.MONTHS);
        supportedUnits.add(TimeUnit.YEARS);
        DurationItem repeatIntervalItem = new DurationItem(FIELD_REPEAT_INTERVAL,
            MSG.widget_jobTriggerEditor_field_repeatInterval_now(), supportedUnits, false, this.isReadOnly);
        repeatIntervalItem.setRequired(true);
        repeatIntervalItem.setContextualHelp(MSG.widget_jobTriggerEditor_fieldHelp_repeatInterval());

        RadioGroupItem recurrenceTypeItem = new RadioGroupItem(FIELD_RECURRENCE_TYPE);
        recurrenceTypeItem.setRequired(true);
        recurrenceTypeItem.setShowTitle(false);
        LinkedHashMap<String, String> recurrenceTypeValueMap = new LinkedHashMap<String, String>();
        recurrenceTypeValueMap.put("for", MSG.widget_jobTriggerEditor_value_for());
        recurrenceTypeValueMap.put("until", MSG.widget_jobTriggerEditor_value_until());
        recurrenceTypeValueMap.put("indefinitely", MSG.widget_jobTriggerEditor_value_indefinitely());
        recurrenceTypeItem.setValueMap(recurrenceTypeValueMap);

        supportedUnits = new TreeSet<TimeUnit>();
        supportedUnits.add(TimeUnit.SECONDS);
        supportedUnits.add(TimeUnit.MINUTES);
        supportedUnits.add(TimeUnit.HOURS);
        supportedUnits.add(TimeUnit.DAYS);
        supportedUnits.add(TimeUnit.WEEKS);
        supportedUnits.add(TimeUnit.MONTHS);
        supportedUnits.add(TimeUnit.YEARS);
        final DurationItem repeatDurationItem = new DurationItem(FIELD_REPEAT_DURATION, null, supportedUnits, true,
            this.isReadOnly);
        repeatDurationItem.setShowTitle(false);
        repeatDurationItem.setVisible(false);
        repeatDurationItem.setContextualHelp(MSG.widget_jobTriggerEditor_fieldHelp_repeatDuration());

        final DateTimeItem endTimeItem = createDateTimeItem(FIELD_END_TIME);
        endTimeItem.setShowTitle(false);
        endTimeItem.setVisible(false);

        SpacerItem spacerItem = new SpacerItem();

        recurrenceTypeItem.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent event) {
                String recurrenceType = (String) event.getValue();
                changeRecurrenceType(recurrenceType, endTimeItem, repeatDurationItem);
            }
        });

        repeatForm.setFields(repeatIntervalItem, recurrenceTypeItem, repeatDurationItem, endTimeItem, spacerItem);
        repeatForm.setVisible(false);

        return repeatForm;
    }

    private void changeRecurrenceType(String recurrenceType, FormItem endTimeItem, DurationItem repeatDurationItem) {
        if (recurrenceType.equals("for")) {
            this.isEndTime = false;
            this.isRepeatDuration = true;
        } else if (recurrenceType.equals("until")) {
            this.isEndTime = true;
            this.isRepeatDuration = false;
        } else {
            // indefinite
            this.isEndTime = false;
            this.isRepeatDuration = false;
        }

        endTimeItem.setRequired(this.isEndTime);
        if (this.isEndTime) {
            endTimeItem.show();
        } else {
            endTimeItem.hide();
        }
        repeatDurationItem.setRequired(this.isRepeatDuration);
        if (this.isRepeatDuration) {
            repeatDurationItem.show();
        } else {
            repeatDurationItem.hide();
        }
    }

    private DynamicForm createLaterForm() {
        final EnhancedDynamicForm laterForm = new EnhancedDynamicForm(this.isReadOnly);
        laterForm.setNumCols(4);
        laterForm.setColWidths(140, 130, 130);

        RadioGroupItem startTypeItem = new RadioGroupItem(FIELD_START_TYPE,
            MSG.widget_jobTriggerEditor_field_startType());
        LinkedHashMap<String, String> startTypeValueMap = new LinkedHashMap<String, String>();
        startTypeValueMap.put("on", MSG.widget_jobTriggerEditor_value_on());
        startTypeValueMap.put("in", MSG.widget_jobTriggerEditor_value_in());
        startTypeItem.setValueMap(startTypeValueMap);
        startTypeItem.setShowTitle(true);

        final DateTimeItem startTimeItem = createDateTimeItem(FIELD_START_TIME);

        TreeSet<TimeUnit> supportedUnits = new TreeSet<TimeUnit>();
        supportedUnits.add(TimeUnit.SECONDS);
        supportedUnits.add(TimeUnit.MINUTES);
        supportedUnits.add(TimeUnit.HOURS);
        supportedUnits.add(TimeUnit.DAYS);
        supportedUnits.add(TimeUnit.WEEKS);
        supportedUnits.add(TimeUnit.MONTHS);
        supportedUnits.add(TimeUnit.YEARS);
        final DurationItem startDelayItem = new DurationItem(FIELD_START_DELAY, null, supportedUnits, false,
            this.isReadOnly);
        startDelayItem.setShowTitle(false);
        startDelayItem.setVisible(false);
        startDelayItem.setContextualHelp(MSG.widget_jobTriggerEditor_fieldHelp_startDelay());

        SpacerItem spacerItem = new SpacerItem();

        startTypeItem.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent event) {
                String startType = (String) event.getValue();
                changeStartType(startType, startDelayItem, startTimeItem);
            }
        });

        laterForm.setFields(startTypeItem, startTimeItem, startDelayItem, spacerItem);
        laterForm.setVisible(false);

        return laterForm;
    }

    private void changeStartType(String startType, DurationItem startDelayItem, FormItem startTimeItem) {
        if (startType.equals("on")) {
            this.isStartDelay = false;
            this.isStartTime = true;
        } else {
            // value.equals("in")
            this.isStartDelay = true;
            this.isStartTime = false;
        }
        startDelayItem.setRequired(this.isStartDelay);
        if (this.isStartDelay) {
            startDelayItem.show();
        } else {
            startDelayItem.hide();
        }
        startTimeItem.setRequired(this.isStartTime);
        if (this.isStartTime) {
            startTimeItem.show();
        } else {
            startTimeItem.hide();
        }
    }

    /**
     * Returns the start time, if one was specified, otherwise returns null to indicate an immediate start.
     *
     * @return the start time, if one was specified, otherwise returns null to indicate an immediate start
     */
    public Date getStartTime() {
        if (!this.isStartLater) {
            return null;
        }
        Date startTime;
        if (this.isStartDelay) {
            // start delay - compute start time
            DurationItem startDelayItem = (DurationItem) this.laterForm.getItem(FIELD_START_DELAY);
            long delay = startDelayItem.getValueAsLong() * 1000;
            long startTimestamp = System.currentTimeMillis() + delay;
            startTime = new Date(startTimestamp);
        } else {
            // start time
            DateTimeItem startTimeItem = (DateTimeItem) this.laterForm.getField(FIELD_START_TIME);
            startTime = startTimeItem.getValueAsDate();
        }
        return startTime;
    }

    public Long getRepeatInterval() {
        Long intervalMillis;
        if (this.isRecurring) {
            DurationItem repeatInterval = (DurationItem) this.repeatForm.getItem(FIELD_REPEAT_INTERVAL);
            intervalMillis = repeatInterval.getValueAsLong() * 1000;
        } else {
            intervalMillis = null;
        }
        return intervalMillis;
    }

    public Integer getRepeatCount() {
        Integer repetitions;
        if (this.isRecurring) {
            if (this.isRepeatDuration) {
                DurationItem repeatDurationItem = (DurationItem) this.repeatForm.getItem(FIELD_REPEAT_DURATION);
                if (repeatDurationItem.getUnitType() == UnitType.ITERATIONS) {
                    // n repetitions
                    repetitions = repeatDurationItem.getValueAsInteger();
                } else {
                    // n units of time - compute end time
                    repetitions = null;
                }
            } else {
                repetitions = null;
            }
        } else {
            repetitions = null;
        }
        return repetitions;
    }

    public Date getEndTime() {
        Date endTime;
        if (this.isRecurring) {
            if (this.isRepeatDuration) {
                DurationItem repeatDurationItem = (DurationItem) this.repeatForm.getItem(FIELD_REPEAT_DURATION);
                if (repeatDurationItem.getUnitType() == UnitType.ITERATIONS) {
                    // n repetitions
                    endTime = null;
                } else {
                    // n units of time - compute end time
                    long delay = repeatDurationItem.getValueAsLong() * 1000;
                    long endTimestamp = System.currentTimeMillis() + delay;
                    endTime = new Date(endTimestamp);
                }
            } else if (this.isEndTime) {
                DateTimeItem endTimeItem = (DateTimeItem) this.repeatForm.getField(FIELD_END_TIME);
                endTime = endTimeItem.getValueAsDate();
            } else {
                endTime = null;
            }
        } else {
            endTime = null;
        }
        return endTime;
    }

    public String getCronExpression() {
        return this.cronForm.getValueAsString(FIELD_CRON_EXPRESSION);
    }

    public boolean validate() {
        // TODO (ips, 01/12/11): Use custom validators to do the startTime / endTime validation instead of the code
        //                       below; that way field-specific validation errors will be used, rather than messages in
        //                       the message bar.
        boolean isValid = true;
        Date currentTime = new Date();
        Date startTime = getStartTime();
        Date endTime = getEndTime();
        if (this.isStartLater) {
            isValid = isValid && this.laterForm.validate();
            if (startTime != null) {
                if (startTime.before(currentTime)) {
                    Message message = new Message(MSG.widget_jobTriggerEditor_message_startTimeMustBeInFuture(),
                        Message.Severity.Error, EnumSet.of(Message.Option.Transient));
                    CoreGUI.getMessageCenter().notify(message);
                    isValid = false;
                }
                if (this.isRecurring && endTime != null) {
                    if (endTime.before(startTime)) {
                        Message message = new Message(
                            MSG.widget_jobTriggerEditor_message_endTimeMustBeAfterStartTime(), Message.Severity.Error,
                            EnumSet.of(Message.Option.Transient));
                        CoreGUI.getMessageCenter().notify(message);
                        isValid = false;
                    }
                }
            }
        }
        if (this.isRecurring) {
            isValid = isValid && this.repeatForm.validate();
            if (endTime != null) {
                if (endTime.before(currentTime)) {
                    Message message = new Message(MSG.widget_jobTriggerEditor_message_endTimeMustBeAfterStartTime(),
                        Message.Severity.Error, EnumSet.of(Message.Option.Transient));
                    CoreGUI.getMessageCenter().notify(message);
                    isValid = false;
                }
            }
        }
        return isValid;
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

}
