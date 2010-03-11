/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.create;

import java.util.Date;

import com.smartgwt.client.types.FormErrorOrientation;
import com.smartgwt.client.types.TimeFormatter;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.FormItemIfFunction;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.DateItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.HeaderItem;
import com.smartgwt.client.widgets.form.fields.IntegerItem;
import com.smartgwt.client.widgets.form.fields.RadioGroupItem;
import com.smartgwt.client.widgets.form.fields.TimeItem;
import com.smartgwt.client.widgets.form.validator.CustomValidator;

import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardStep;

/**
 * @author Greg Hinkle
 */
public class OperationSchedulingStep implements WizardStep {

    private DynamicForm form;

    public Canvas getCanvas() {

        if (form == null) {
            form = new DynamicForm();
            form.setWrapItemTitles(false);
            form.setErrorOrientation(FormErrorOrientation.RIGHT);
            form.setWidth100();
            form.setNumCols(4);
            form.setColWidths("15%", "35%", "15%", "*");
            form.setValidateOnChange(true);

            final RadioGroupItem start = new RadioGroupItem("start", "Start");
            start.setColSpan(3);
            start.setValueMap("Immediately", "Schedule for Future");
            start.setRedrawOnChange(true);
            start.setValue("Immediately");

            HeaderItem scheduleHeader = new HeaderItem("scheduleHeader");
            scheduleHeader.setValue("Schedule");
            scheduleHeader.setShowIfCondition(new FormItemIfFunction() {
                public boolean execute(FormItem formItem, Object o, DynamicForm dynamicForm) {
                    return !"Immediately".equals(form.getValueAsString("start"));
                }
            });

            RadioGroupItem recurr = new RadioGroupItem("recurr", "Recurrence");
            recurr.setValueMap("Once", "Daily", "Weekly", "Monthly");
            recurr.setRedrawOnChange(true);
            recurr.setValue("Once");
            recurr.setShowIfCondition(new FormItemIfFunction() {
                public boolean execute(FormItem formItem, Object o, DynamicForm dynamicForm) {
                    return !"Immediately".equals(form.getValueAsString("start"));
                }
            });

            CanvasItem onceItem = new CanvasItem("once", "Run At");
            onceItem.setShowTitle(false);
            onceItem.setCanvas(getOnceForm());
            onceItem.setShowIfCondition(new FormItemIfFunction() {
                public boolean execute(FormItem formItem, Object o, DynamicForm dynamicForm) {
                    return !"Immediately".equals(form.getValueAsString("start"))
                        && "Once".equals(form.getValueAsString("recurr"));
                }
            });

            CanvasItem dailyItem = new CanvasItem("daily", "Days");
            dailyItem.setShowTitle(false);
            dailyItem.setCanvas(getDailyForm());
            dailyItem.setShowIfCondition(new FormItemIfFunction() {
                public boolean execute(FormItem formItem, Object o, DynamicForm dynamicForm) {
                    return !"Immediately".equals(form.getValueAsString("start"))
                        && "Daily".equals(form.getValueAsString("recurr"));
                }
            });

            CanvasItem weeklyItem = new CanvasItem("weekly", "Weekly");
            weeklyItem.setShowTitle(false);
            weeklyItem.setCanvas(getWeeklyForm());
            weeklyItem.setShowIfCondition(new FormItemIfFunction() {
                public boolean execute(FormItem formItem, Object o, DynamicForm dynamicForm) {
                    return !"Immediately".equals(form.getValueAsString("start"))
                        && "Weekly".equals(form.getValueAsString("recurr"));
                }
            });

            CanvasItem monthlyItem = new CanvasItem("monthly", "Monthly");
            monthlyItem.setShowTitle(false);
            monthlyItem.setCanvas(getMonthlyForm());
            monthlyItem.setShowIfCondition(new FormItemIfFunction() {
                public boolean execute(FormItem formItem, Object o, DynamicForm dynamicForm) {
                    return !"Immediately".equals(form.getValueAsString("start"))
                        && "Monthly".equals(form.getValueAsString("recurr"));
                }
            });

            HeaderItem rangeHeader = new HeaderItem("timePeriod");
            rangeHeader.setValue("Time Period");
            rangeHeader.setShowIfCondition(new FormItemIfFunction() {
                public boolean execute(FormItem formItem, Object o, DynamicForm dynamicForm) {
                    return !"Immediately".equals(form.getValueAsString("start"))
                        && !"Once".equals(form.getValueAsString("recurr"));
                }
            });

            DateItem startDate = new DateItem("startDate", "Start Date");
            startDate.setStartRow(true);
            startDate.setStartDate(new Date());
            startDate.setShowIfCondition(new FormItemIfFunction() {
                public boolean execute(FormItem formItem, Object o, DynamicForm dynamicForm) {
                    return !"Immediately".equals(form.getValueAsString("start"))
                        && !"Once".equals(form.getValueAsString("recurr"));
                }
            });

            final RadioGroupItem end = new RadioGroupItem("endType", "Recurrence End");
            end.setShowIfCondition(new FormItemIfFunction() {
                public boolean execute(FormItem formItem, Object o, DynamicForm dynamicForm) {
                    return !"Immediately".equals(form.getValueAsString("start"))
                        && !"Once".equals(form.getValueAsString("recurr"));
                }
            });
            end.setStartRow(true);
            end.setValueMap("Never", "End On");
            end.setRedrawOnChange(true);
            end.setValue("Never");

            DateItem endDate = new DateItem("endDate", "End Date");
            endDate.setStartRow(true);
            endDate.setStartDate(new Date());
            endDate.setShowIfCondition(new FormItemIfFunction() {
                public boolean execute(FormItem formItem, Object o, DynamicForm dynamicForm) {
                    return !"Immediately".equals(form.getValueAsString("start"))
                        && !"Once".equals(form.getValueAsString("recurr"))
                        && !"Never".equals(form.getValueAsString("endType"));
                }
            });
            endDate.setValidators(new CustomValidator() {
                @Override
                protected boolean condition(Object o) {
                    return (((Date) form.getValue("startDate")).before((Date) o));
                }
            });

            form.setItems(start, scheduleHeader, recurr, onceItem, dailyItem, weeklyItem, monthlyItem, rangeHeader,
                startDate, end, endDate);
        }
        return form;
    }

    private DynamicForm getOnceForm() {
        DynamicForm form = new DynamicForm();
        form.setWrapItemTitles(false);
        form.setNumCols(2);

        DateItem startDate = new DateItem("startDate", "Date");

        TimeItem startTime = new TimeItem("startTime", "Time");
        startTime.setDisplayFormat(TimeFormatter.TOSHORTPADDEDTIME);
        startTime.setUseMask(true);
        form.setItems(startDate, startTime);
        return form;
    }

    private DynamicForm getWeeklyForm() {
        DynamicForm form = new DynamicForm();
        form.setWrapItemTitles(false);

        TimeItem timeOfDay = new TimeItem("timeOfDay", "Time Of Day");
        timeOfDay.setDisplayFormat(TimeFormatter.TOSHORTPADDEDTIME);
        timeOfDay.setUseMask(true);

        String[] days = { "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday" };
        FormItem[] items = new FormItem[8];
        items[0] = timeOfDay;
        int i = 1;
        for (String day : days) {
            CheckboxItem dayItem = new CheckboxItem(day);
            dayItem.setShowTitle(false);
            items[i++] = dayItem;
        }
        form.setItems(items);
        return form;
    }

    private DynamicForm getDailyForm() {
        DynamicForm form = new DynamicForm();
        form.setWrapItemTitles(false);

        TimeItem timeOfDay = new TimeItem("timeOfDay", "Time Of Day");
        timeOfDay.setDisplayFormat(TimeFormatter.TOSHORTPADDEDTIME);
        timeOfDay.setUseMask(true);

        form.setItems(timeOfDay);
        return form;
    }

    private DynamicForm getMonthlyForm() {
        DynamicForm form = new DynamicForm();
        form.setWrapItemTitles(false);

        IntegerItem dayItem = new IntegerItem();
        dayItem.setTitle("Day of Month");

        TimeItem timeOfDay = new TimeItem("timeOfDay", "Time Of Day");
        timeOfDay.setDisplayFormat(TimeFormatter.TOSHORTPADDEDTIME);
        timeOfDay.setUseMask(true);

        form.setItems(dayItem, timeOfDay);
        return form;
    }

    public boolean nextPage() {
        return true; // TODO: Implement this method.
    }

    public String getName() {
        return "Schedule";
    }

    public boolean isNextEnabled() {
        return true;
    }

    public boolean isPreviousEnabled() {
        return true;
    }
}
