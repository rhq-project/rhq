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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.smartgwt.client.types.Visibility;
import com.smartgwt.client.util.SC;
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
import com.smartgwt.client.widgets.form.validator.RegExpValidator;

import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;
import org.rhq.core.domain.common.JobTrigger;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * A SmartGWT widget that provides the ability to create a new {@link JobTrigger job trigger}, or view or edit an
 * existing job trigger.
 *
 * @author Ian Springer
 */
public class JobTriggerEditor extends LocatableVLayout {

    // Field Names
    private static final String FIELD_REPEAT_INTERVAL = "repeatInterval";
    private static final String FIELD_REPEAT_DURATION = "repeatDuration";
    private static final String FIELD_END_TIME = "endTime";
    private static final String FIELD_START_TYPE = "startType";
    private static final String FIELD_START_TIME = "startTime";
    private static final String FIELD_START_DELAY = "startDelay";
    private static final String FIELD_RECURRENCE_TYPE = "recurrenceType";
    private static final String FIELD_CRON_EXPRESSION = "cronExpression";

    private static final Map<String, Long> UNITS_TO_MILLIS_MULTIPLIER_MAP = new HashMap<String, Long>();
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
    private LocatableDynamicForm cronForm;

    // These flags allow us to determine the trigger type.
    private boolean isCronMode;
    private boolean isStartLater;
    private boolean isRecurring;
    private boolean isRepeatDuration;
    private boolean isEndTime;
    private boolean isStartDelay;
    private boolean isStartTime;

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

        final LocatableVLayout calendarModeLayout = new LocatableVLayout(extendLocatorId("CalendarModeLayout"));
        calendarModeLayout.setVisible(false);

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
        
        calendarModeLayout.addMember(calendarTypeForm);
        addMember(calendarModeLayout);

        final LocatableVLayout cronModeLayout = new LocatableVLayout(extendLocatorId("CronModeLayout"));
        cronModeLayout.setVisible(false);

        this.cronForm = new LocatableDynamicForm(cronModeLayout.extendLocatorId("Form"));

        TextItem cronExpressionItem = new TextItem(FIELD_CRON_EXPRESSION, "Cron Expression");
        cronExpressionItem.setRequired(true);
        cronExpressionItem.setWidth(340);

        this.cronForm.setFields(cronExpressionItem);

        cronModeLayout.addMember(this.cronForm);

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

        Tab formatTab = new Tab("Format");
        HTMLFlow formatPane = new HTMLFlow();
        formatPane.setWidth100();
        formatPane.setContents("<p>A cron expression is a string comprised of 6 or 7 fields separated by white space. Fields can contain any of the\n" +
                "allowed values, along with various combinations of the allowed special characters for that field. The fields are as\n" +
                "follows:</p>\n" +
                "<table cellpadding=\"3\" cellspacing=\"1\">\n" +
                "    <tbody>\n" +
                "\n" +
                "        <tr>\n" +
                "            <th>Field Name</th>\n" +
                "            <th>Mandatory</th>\n" +
                "            <th>Allowed Values</th>\n" +
                "            <th>Allowed Special Characters</th>\n" +
                "        </tr>\n" +
                "        <tr>\n" +
                "\n" +
                "            <td>Seconds</td>\n" +
                "            <td>YES</td>\n" +
                "\n" +
                "            <td>0-59</td>\n" +
                "            <td>, - * /</td>\n" +
                "        </tr>\n" +
                "        <tr>\n" +
                "\n" +
                "            <td>Minutes</td>\n" +
                "            <td>YES</td>\n" +
                "            <td>0-59</td>\n" +
                "\n" +
                "            <td>, - * /</td>\n" +
                "        </tr>\n" +
                "        <tr>\n" +
                "\n" +
                "            <td>Hours</td>\n" +
                "            <td>YES</td>\n" +
                "            <td>0-23</td>\n" +
                "            <td>, - * /</td>\n" +
                "\n" +
                "        </tr>\n" +
                "        <tr>\n" +
                "\n" +
                "            <td>Day of month</td>\n" +
                "            <td>YES</td>\n" +
                "            <td>1-31</td>\n" +
                "            <td>, - * ? / L W<br clear=\"all\" />\n" +
                "            </td>\n" +
                "        </tr>\n" +
                "        <tr>\n" +
                "\n" +
                "            <td>Month</td>\n" +
                "            <td>YES</td>\n" +
                "            <td>1-12 or JAN-DEC</td>\n" +
                "            <td>, - * /</td>\n" +
                "        </tr>\n" +
                "        <tr>\n" +
                "\n" +
                "            <td>Day of week</td>\n" +
                "\n" +
                "            <td>YES</td>\n" +
                "            <td>1-7 or SUN-SAT</td>\n" +
                "            <td>, - * ? / L #</td>\n" +
                "        </tr>\n" +
                "        <tr>\n" +
                "\n" +
                "            <td>Year</td>\n" +
                "            <td>NO</td>\n" +
                "\n" +
                "            <td>empty, 1970-2099</td>\n" +
                "            <td>, - * /</td>\n" +
                "        </tr>\n" +
                "    </tbody>\n" +
                "\n" +
                "</table>\n" +
                "<p>So cron expressions can be as simple as this: <tt>&#42; * * * ? *</tt><br />\n" +
                "or more complex, like this: <tt>0/5 14,18,3-39,52 * ? JAN,MAR,SEP MON-FRI 2002-2010</tt></p>\n" +
                "\n" +
                "<h2><a name=\"CronTriggersTutorial-Specialcharacters\"></a>Special Characters</h2>\n" +
                "\n" +
                "<ul>\n" +
                "    <li><tt><b>&#42;</b></tt> (<em>\"all values\"</em>) - used to select all values within a field. For example, \"*\"\n" +
                "    in the minute field means <em>\"every minute\"</em>.</li>\n" +
                "\n" +
                "</ul>\n" +
                "\n" +
                "\n" +
                "<ul>\n" +
                "    <li><tt><b>?</b></tt> (<em>\"no specific value\"</em>) - useful when you need to specify something in one of the\n" +
                "    two fields in which the character is allowed, but not the other. For example, if I want my trigger to fire on a\n" +
                "    particular day of the month (say, the 10th), but don't care what day of the week that happens to be, I would put\n" +
                "    \"10\" in the day-of-month field, and \"?\" in the day-of-week field. See the examples below for clarification.</li>\n" +
                "\n" +
                "</ul>\n" +
                "\n" +
                "\n" +
                "<ul>\n" +
                "    <li><tt><b>&#45;</b></tt> &#45; used to specify ranges. For example, \"10-12\" in the hour field means <em>\"the\n" +
                "    hours 10, 11 and 12\"</em>.</li>\n" +
                "\n" +
                "</ul>\n" +
                "\n" +
                "\n" +
                "<ul>\n" +
                "    <li><tt><b>,</b></tt> &#45; used to specify additional values. For example, \"MON,WED,FRI\" in the day-of-week\n" +
                "    field means <em>\"the days Monday, Wednesday, and Friday\"</em>.</li>\n" +
                "\n" +
                "</ul>\n" +
                "\n" +
                "\n" +
                "<ul>\n" +
                "\n" +
                "    <li><tt><b>/</b></tt> &#45; used to specify increments. For example, \"0/15\" in the seconds field means <em>\"the\n" +
                "    seconds 0, 15, 30, and 45\"</em>. And \"5/15\" in the seconds field means <em>\"the seconds 5, 20, 35, and 50\"</em>. You can\n" +
                "    also specify '/' after the '<b>' character - in this case '</b>' is equivalent to having '0' before the '/'. '1/3'\n" +
                "    in the day-of-month field means <em>\"fire every 3 days starting on the first day of the month\"</em>.</li>\n" +
                "\n" +
                "</ul>\n" +
                "\n" +
                "<ul>\n" +
                "    <li><tt><b>L</b></tt> (<em>\"last\"</em>) - has different meaning in each of the two fields in which it is\n" +
                "    allowed. For example, the value \"L\" in the day-of-month field means <em>\"the last day of the month\"</em> &#45; day\n" +
                "    31 for January, day 28 for February on non-leap years. If used in the day-of-week field by itself, it simply means\n" +
                "    \"7\" or \"SAT\". But if used in the day-of-week field after another value, it means <em>\"the last xxx day of the\n" +
                "    month\"</em> &#45; for example \"6L\" means <em>\"the last friday of the month\"</em>. When using the 'L' option, it is\n" +
                "    important not to specify lists, or ranges of values, as you'll get confusing results.</li>\n" +
                "\n" +
                "</ul>\n" +
                "\n" +
                "\n" +
                "<ul>\n" +
                "    <li><tt><b>W</b></tt> (<em>\"weekday\"</em>) - used to specify the weekday (Monday-Friday) nearest the given day.\n" +
                "    As an example, if you were to specify \"15W\" as the value for the day-of-month field, the meaning is: <em>\"the\n" +
                "    nearest weekday to the 15th of the month\"</em>. So if the 15th is a Saturday, the trigger will fire on Friday the 14th.\n" +
                "    If the 15th is a Sunday, the trigger will fire on Monday the 16th. If the 15th is a Tuesday, then it will fire on\n" +
                "    Tuesday the 15th. However if you specify \"1W\" as the value for day-of-month, and the 1st is a Saturday, the trigger\n" +
                "    will fire on Monday the 3rd, as it will not 'jump' over the boundary of a month's days. The 'W' character can only\n" +
                "    be specified when the day-of-month is a single day, not a range or list of days.\n" +
                "        <div class=\"tip\">\n" +
                "            The 'L' and 'W' characters can also be combined in the day-of-month field to yield 'LW', which\n" +
                "            translates to <em>\"last weekday of the month\"</em>.\n" +
                "        </div>\n" +
                "\n" +
                "    </li>\n" +
                "\n" +
                "    <li><tt><b>&#35;</b></tt> &#45; used to specify \"the nth\" XXX day of the month. For example, the value of \"6#3\"\n" +
                "    in the day-of-week field means <em>\"the third Friday of the month\"</em> (day 6 = Friday and \"#3\" = the 3rd one in\n" +
                "    the month). Other examples: \"2#1\" = the first Monday of the month and \"4#5\" = the fifth Wednesday of the month. Note\n" +
                "    that if you specify \"#5\" and there is not 5 of the given day-of-week in the month, then no firing will occur that\n" +
                "    month.\n" +
                "        <div class=\"tip\">\n" +
                "            The legal characters and the names of months and days of the week are not case sensitive. <tt>MON</tt>\n" +
                "            is the same as <tt>mon</tt>.\n" +
                "        </div>\n" +
                "\n" +
                "    </li>\n" +
                "</ul>" +
                "<h2><a name=\"CronTriggersTutorial-Notes\"></a>Notes</h2>\n" +
                "\n" +
                "<ul>\n" +
                "    <li>Support for specifying both a day-of-week and a day-of-month value is not complete (you must currently use\n" +
                "    the '?' character in one of these fields).</li>\n" +
                "    <li>Be careful when setting fire times between mid-night and 1:00 AM - \"daylight savings\" can cause a skip or a\n" +
                "    repeat depending on whether the time moves back or jumps forward.</li>\n" +
                "\n" +
                "</ul>");
        formatTab.setPane(formatPane);

        Tab examplesTab = new Tab("Examples");
        HTMLFlow examplesPane = new HTMLFlow();
        examplesPane.setWidth100();
        examplesPane.setContents("<table cellpadding=\"3\" cellspacing=\"1\">\n" +
                "    <tbody>\n" +
                "        <tr>\n" +
                "            <th>Expression</th>\n" +
                "\n" +
                "            <th>Meaning</th>\n" +
                "        </tr>\n" +
                "        <tr>\n" +
                "            <td><tt>0 0 12 * * ?</tt></td>\n" +
                "\n" +
                "            <td>Fire at 12pm (noon) every day</td>\n" +
                "        </tr>\n" +
                "        <tr>\n" +
                "\n" +
                "            <td><tt>0 15 10 ? * *</tt></td>\n" +
                "            <td>Fire at 10:15am every day</td>\n" +
                "        </tr>\n" +
                "        <tr>\n" +
                "            <td><tt>0 15 10 * * ?</tt></td>\n" +
                "\n" +
                "            <td>Fire at 10:15am every day</td>\n" +
                "\n" +
                "        </tr>\n" +
                "        <tr>\n" +
                "            <td><tt>0 15 10 * * ? *</tt></td>\n" +
                "            <td>Fire at 10:15am every day</td>\n" +
                "        </tr>\n" +
                "        <tr>\n" +
                "            <td><tt>0 15 10 * * ? 2005</tt></td>\n" +
                "\n" +
                "            <td>Fire at 10:15am every day during the year 2005</td>\n" +
                "        </tr>\n" +
                "        <tr>\n" +
                "            <td><tt>0 * 14 * * ?</tt></td>\n" +
                "            <td>Fire every minute starting at 2pm and ending at 2:59pm, every day</td>\n" +
                "        </tr>\n" +
                "        <tr>\n" +
                "\n" +
                "            <td><tt>0 0/5 14 * * ?</tt></td>\n" +
                "\n" +
                "            <td>Fire every 5 minutes starting at 2pm and ending at 2:55pm, every day</td>\n" +
                "        </tr>\n" +
                "        <tr>\n" +
                "            <td><tt>0 0/5 14,18 * * ?</tt></td>\n" +
                "            <td>Fire every 5 minutes starting at 2pm and ending at 2:55pm, AND fire every 5\n" +
                "            minutes starting at 6pm and ending at 6:55pm, every day</td>\n" +
                "\n" +
                "        </tr>\n" +
                "        <tr>\n" +
                "            <td><tt>0 0-5 14 * * ?</tt></td>\n" +
                "\n" +
                "            <td>Fire every minute starting at 2pm and ending at 2:05pm, every day</td>\n" +
                "        </tr>\n" +
                "        <tr>\n" +
                "            <td><tt>0 10,44 14 ? 3 WED</tt></td>\n" +
                "\n" +
                "            <td>Fire at 2:10pm and at 2:44pm every Wednesday in the month of March.</td>\n" +
                "        </tr>\n" +
                "        <tr>\n" +
                "            <td><tt>0 15 10 ? * MON-FRI</tt></td>\n" +
                "\n" +
                "            <td>Fire at 10:15am every Monday, Tuesday, Wednesday, Thursday and Friday</td>\n" +
                "        </tr>\n" +
                "        <tr>\n" +
                "\n" +
                "            <td><tt>0 15 10 15 * ?</tt></td>\n" +
                "            <td>Fire at 10:15am on the 15th day of every month</td>\n" +
                "        </tr>\n" +
                "        <tr>\n" +
                "            <td><tt>0 15 10 L * ?</tt></td>\n" +
                "\n" +
                "            <td>Fire at 10:15am on the last day of every month</td>\n" +
                "\n" +
                "        </tr>\n" +
                "        <tr>\n" +
                "            <td><tt>0 15 10 ? * 6L</tt></td>\n" +
                "            <td>Fire at 10:15am on the last Friday of every month</td>\n" +
                "        </tr>\n" +
                "        <tr>\n" +
                "            <td><tt>0 15 10 ? * 6L</tt></td>\n" +
                "\n" +
                "            <td>Fire at 10:15am on the last Friday of every month</td>\n" +
                "        </tr>\n" +
                "        <tr>\n" +
                "            <td><tt>0 15 10 ? * 6L 2002-2005</tt></td>\n" +
                "            <td>Fire at 10:15am on every last friday of every month during the years 2002,\n" +
                "            2003, 2004 and 2005</td>\n" +
                "        </tr>\n" +
                "        <tr>\n" +
                "\n" +
                "            <td><tt>0 15 10 ? * 6#3</tt></td>\n" +
                "\n" +
                "            <td>Fire at 10:15am on the third Friday of every month</td>\n" +
                "        </tr>\n" +
                "        <tr>\n" +
                "            <td><tt>0 0 12 1/5 * ?</tt></td>\n" +
                "            <td>Fire at 12pm (noon) every 5 days every month, starting on the first day of the\n" +
                "            month.</td>\n" +
                "\n" +
                "        </tr>\n" +
                "        <tr>\n" +
                "            <td><tt>0 11 11 11 11 ?</tt></td>\n" +
                "\n" +
                "            <td>Fire every November 11th at 11:11am.</td>\n" +
                "        </tr>\n" +
                "    </tbody>\n" +
                "</table>");
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

        cronModeLayout.addMember(cronHelpTabSet);
        addMember(cronModeLayout);

        modeItem.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent event) {
                if (event.getValue().equals("calendar")) {
                    JobTriggerEditor.this.isCronMode = false;
                    calendarModeLayout.show();
                    cronModeLayout.hide();
                } else {
                    // cron mode
                    JobTriggerEditor.this.isCronMode = true;
                    calendarModeLayout.hide();
                    cronModeLayout.show();
                }
            }
        });

        this.laterForm = createLaterForm();
        calendarModeLayout.addMember(this.laterForm);

        this.repeatForm = createRepeatForm();
        calendarModeLayout.addMember(this.repeatForm);

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

        // Configure context-sensitive help.
        FormItemIcon helpIcon = new FormItemIcon();
        helpIcon.setSrc("[SKIN]/actions/help.png");
        repeatIntervalItem.setIcons(helpIcon);
        repeatIntervalItem.addIconClickHandler(new IconClickHandler() {
            public void onIconClick(IconClickEvent event) {
                SC.say("N UNITS (where N is a positive integer and UNITS is \"seconds\", \"minutes\", \"hours\", \"days\", \"weeks\", \"months\", \"quarters\", or \"years\", e.g. \"30 seconds\" or \"6 weeks\")");
            }
        });

        // Configure validation.
        RegExpValidator repeatIntervalValidator = new RegExpValidator("[1-9][0-9]*[ ]*(seconds|s|minutes|m|hours|h|days|d|weeks|w|months|M|quarters|q|years|y)");
        repeatIntervalItem.setValidators(repeatIntervalValidator);
        repeatIntervalItem.setValidateOnExit(true);

        RadioGroupItem recurrenceTypeItem = new RadioGroupItem(FIELD_RECURRENCE_TYPE);
        recurrenceTypeItem.setRequired(true);
        recurrenceTypeItem.setShowTitle(false);
        LinkedHashMap<String, String> recurrenceTypeValueMap = new LinkedHashMap<String, String>();
        recurrenceTypeValueMap.put("for", "For");
        recurrenceTypeValueMap.put("until", "Until");
        recurrenceTypeValueMap.put("indefinitely", "Indefinitely");
        recurrenceTypeItem.setValueMap(recurrenceTypeValueMap);

        final TextItem repeatDurationItem = new TextItem(FIELD_REPEAT_DURATION);
        repeatDurationItem.setShowTitle(false);
        repeatDurationItem.setVisible(false);

        // Configure context-sensitive help.
        helpIcon = new FormItemIcon();
        helpIcon.setSrc("[SKIN]/actions/help.png");
        repeatDurationItem.setIcons(helpIcon);
        repeatDurationItem.addIconClickHandler(new IconClickHandler() {
            public void onIconClick(IconClickEvent event) {
                SC.say("N UNITS (where N is a positive integer and UNITS is \"times\", \"seconds\", \"minutes\", \"hours\", \"days\", \"weeks\", \"months\", \"quarters\", or \"years\", e.g. \"30 seconds\" or \"5 repetitions\")");
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

        // Configure context-sensitive help.
        FormItemIcon icon = new FormItemIcon();
        icon.setSrc("[SKIN]/actions/help.png");
        startDelayItem.setIcons(icon);
        startDelayItem.addIconClickHandler(new IconClickHandler() {
            public void onIconClick(IconClickEvent event) {
                SC.say("N UNITS (where N is a positive integer and UNITS is \"seconds\", \"minutes\", \"hours\", \"days\", \"weeks\", \"months\", \"quarters\", or \"years\", e.g. \"30 seconds\" or \"6 weeks\")");
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

    public Date getStartTime() {
        Date startTime;
        if (this.isStartDelay) {
            // start delay - computer start time
            String startDelay = this.laterForm.getValueAsString(FIELD_START_DELAY);
            Duration startDelayDuration = parseDurationString(startDelay);
            long delay = startDelayDuration.count * startDelayDuration.multiplier;
            long startTimestamp = System.currentTimeMillis() + delay;
            startTime = new Date(startTimestamp);
        } else {
            // start time
            DateTimeItem startTimeItem = (DateTimeItem)this.laterForm.getField(FIELD_START_TIME);
            startTime = startTimeItem.getValueAsDate();
        }
        return startTime;
    }

    public Long getRepeatInterval() {
        Long intervalMillis;
        if (this.isRecurring) {
            String repeatInterval = this.repeatForm.getValueAsString(FIELD_REPEAT_INTERVAL);
            Duration intervalDuration = parseDurationString(repeatInterval);
            intervalMillis = intervalDuration.count * intervalDuration.multiplier;
        } else {
            intervalMillis = null;
        }
        return intervalMillis;
    }

    public Integer getRepeatCount() {
        Integer repetitions;
        if (this.isRecurring) {
            if (this.isRepeatDuration) {
                String repeatDurationString = this.repeatForm.getValueAsString(FIELD_REPEAT_DURATION);
                Duration repeatDuration = parseDurationString(repeatDurationString);
                if (repeatDuration.multiplier == null) {
                    // n repetitions
                    repetitions = repeatDuration.count;
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
                String repeatDurationString = this.repeatForm.getValueAsString(FIELD_REPEAT_DURATION);
                Duration repeatDuration = parseDurationString(repeatDurationString);
                if (repeatDuration.multiplier == null) {
                    // n repetitions
                    endTime = null;
                } else {
                    // n units of time - compute end time
                    long delay = repeatDuration.count * repeatDuration.multiplier;
                    long endTimestamp = System.currentTimeMillis() + delay;
                    endTime = new Date(endTimestamp);
                }
            } else if (this.isEndTime) {
                DateTimeItem endTimeItem = (DateTimeItem)this.repeatForm.getField(FIELD_END_TIME);
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
                    Message message = new Message("Start time must be in the future.", Message.Severity.Error,
                            EnumSet.of(Message.Option.Transient));
                    CoreGUI.getMessageCenter().notify(message);
                    isValid = false;
                }
                if (this.isRecurring && endTime != null) {
                    if (endTime.before(startTime)) {
                        Message message = new Message("End time must be after start time.", Message.Severity.Error,
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
                    Message message = new Message("End time must be in the future.", Message.Severity.Error,
                            EnumSet.of(Message.Option.Transient));
                    CoreGUI.getMessageCenter().notify(message);
                    isValid = false;
                }
            }
        }
        return isValid;
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
