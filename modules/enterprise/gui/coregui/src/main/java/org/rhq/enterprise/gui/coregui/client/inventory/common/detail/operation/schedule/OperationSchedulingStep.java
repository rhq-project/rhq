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
package org.rhq.enterprise.gui.coregui.client.inventory.common.detail.operation.schedule;

import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import com.smartgwt.client.types.FormErrorOrientation;
import com.smartgwt.client.types.TimeFormatter;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.FormItemIfFunction;
import com.smartgwt.client.widgets.form.ValuesManager;
import com.smartgwt.client.widgets.form.events.ItemChangedEvent;
import com.smartgwt.client.widgets.form.events.ItemChangedHandler;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.DateItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.HeaderItem;
import com.smartgwt.client.widgets.form.fields.IntegerItem;
import com.smartgwt.client.widgets.form.fields.RadioGroupItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TimeItem;
import com.smartgwt.client.widgets.form.validator.CustomValidator;

import org.rhq.enterprise.gui.coregui.client.components.wizard.AbstractWizardStep;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;

/**
 * @author Greg Hinkle
 */
public class OperationSchedulingStep extends AbstractWizardStep implements ItemChangedHandler {

    private DynamicForm form;
    private ValuesManager valuesManager;

    private ExecutionSchedule executionSchedule = new ExecutionSchedule();

    public Canvas getCanvas() {

        if (form == null) {
            valuesManager = new ValuesManager();
            form = new LocatableDynamicForm("OperationScheduling");
            form.setValuesManager(valuesManager);
            form.setWrapItemTitles(false);
            form.setErrorOrientation(FormErrorOrientation.RIGHT);
            form.setWidth100();
            form.setPadding(10);
            form.setNumCols(3);
            //            form.setColWidths("15%", "35%", "15%", "*");
            form.setValidateOnChange(true);

            final RadioGroupItem start = new RadioGroupItem("start",
                MSG.view_operationCreateWizard_schedulingStep_name());
            start.setColSpan(3);
            start.setValueMap(enumValueMap(ExecutionSchedule.Start.class)); // "Immediately", "Future"
            start.setRedrawOnChange(true);
            start.setValue("Immediately");

            HeaderItem scheduleHeader = new HeaderItem("scheduleHeader");
            scheduleHeader.setValue(MSG.view_operationCreateWizard_schedulingStep_label_schedule());
            scheduleHeader.setShowIfCondition(new FormItemIfFunction() {
                public boolean execute(FormItem formItem, Object o, DynamicForm dynamicForm) {
                    return !"Immediately".equals(form.getValueAsString("start"));
                }
            });

            RadioGroupItem recurr = new RadioGroupItem("recurr",
                MSG.view_operationCreateWizard_schedulingStep_label_recurrence());
            recurr.setValueMap(enumValueMap(ExecutionSchedule.Recurr.class)); // "Once", "EveryNMinutes", "Hourly", "Daily", "Weekly", "Monthly");
            recurr.setRedrawOnChange(true);
            recurr.setValue(ExecutionSchedule.Recurr.Once.name());
            recurr.setShowIfCondition(new FormItemIfFunction() {
                public boolean execute(FormItem formItem, Object o, DynamicForm dynamicForm) {
                    return !ExecutionSchedule.Start.Immediately.name().equals(form.getValueAsString("start"));
                }
            });

            CanvasItem onceForm = new CanvasItem("once", MSG.view_operationCreateWizard_schedulingStep_label_onceAt());
            onceForm.setShowTitle(false);
            onceForm.setCanvas(getOnceForm());
            onceForm.setShowIfCondition(new FormItemIfFunction() {
                public boolean execute(FormItem formItem, Object o, DynamicForm dynamicForm) {
                    return !ExecutionSchedule.Start.Immediately.name().equals(form.getValueAsString("start"))
                        && ExecutionSchedule.Recurr.Once.name().equals(form.getValueAsString("recurr"));
                }
            });

            CanvasItem everyNMinuteForm = new CanvasItem("everyNMinutesForm",
                MSG.view_operationCreateWizard_schedulingStep_label_everyNMinutes());
            everyNMinuteForm.setShowTitle(false);
            everyNMinuteForm.setCanvas(getEveryNMinutesForm());
            everyNMinuteForm.setShowIfCondition(new FormItemIfFunction() {
                public boolean execute(FormItem formItem, Object o, DynamicForm dynamicForm) {
                    return !ExecutionSchedule.Start.Immediately.name().equals(form.getValueAsString("start"))
                        && ExecutionSchedule.Recurr.EveryNMinutes.name().equals(form.getValueAsString("recurr"));
                }
            });

            CanvasItem hourlyForm = new CanvasItem("hourlyForm", MSG.view_operationCreateWizard_schedulingStep_label_hourly());
            hourlyForm.setShowTitle(false);
            hourlyForm.setCanvas(getHourlyForm());
            hourlyForm.setShowIfCondition(new FormItemIfFunction() {
                public boolean execute(FormItem formItem, Object o, DynamicForm dynamicForm) {
                    return !"Immediately".equals(form.getValueAsString("start"))
                        && ExecutionSchedule.Recurr.Hourly.name().equals(form.getValueAsString("recurr"));
                }
            });

            CanvasItem dailyForm = new CanvasItem("daily", MSG.view_operationCreateWizard_schedulingStep_label_daily());
            dailyForm.setShowTitle(false);
            dailyForm.setCanvas(getDailyForm());
            dailyForm.setShowIfCondition(new FormItemIfFunction() {
                public boolean execute(FormItem formItem, Object o, DynamicForm dynamicForm) {
                    return !ExecutionSchedule.Start.Immediately.name().equals(form.getValueAsString("start"))
                        && ExecutionSchedule.Recurr.Daily.name().equals(form.getValueAsString("recurr"));
                }
            });

            CanvasItem weeklyForm = new CanvasItem("weekly", MSG.view_operationCreateWizard_schedulingStep_label_weekly());
            weeklyForm.setShowTitle(false);
            weeklyForm.setCanvas(getWeeklyForm());
            weeklyForm.setShowIfCondition(new FormItemIfFunction() {
                public boolean execute(FormItem formItem, Object o, DynamicForm dynamicForm) {
                    return !ExecutionSchedule.Start.Immediately.name().equals(form.getValueAsString("start"))
                        && ExecutionSchedule.Recurr.Weekly.name().equals(form.getValueAsString("recurr"));
                }
            });

            CanvasItem monthlyForm = new CanvasItem("monthly", MSG.view_operationCreateWizard_schedulingStep_label_monthly());
            monthlyForm.setShowTitle(false);
            monthlyForm.setCanvas(getMonthlyForm());
            monthlyForm.setShowIfCondition(new FormItemIfFunction() {
                public boolean execute(FormItem formItem, Object o, DynamicForm dynamicForm) {
                    return !ExecutionSchedule.Start.Immediately.name().equals(form.getValueAsString("start"))
                        && ExecutionSchedule.Recurr.Monthly.name().equals(form.getValueAsString("recurr"));
                }
            });

            HeaderItem rangeHeader = new HeaderItem("timePeriod");
            rangeHeader.setValue(MSG.view_operationCreateWizard_schedulingStep_label_timePeriod());
            rangeHeader.setShowIfCondition(new FormItemIfFunction() {
                public boolean execute(FormItem formItem, Object o, DynamicForm dynamicForm) {
                    return !ExecutionSchedule.Start.Immediately.name().equals(form.getValueAsString("start"))
                        && !ExecutionSchedule.Recurr.Once.name().equals(form.getValueAsString("recurr"));
                }
            });

            DateItem startDate = new DateItem("startDate", MSG.view_operationCreateWizard_schedulingStep_label_startDate());
            startDate.setStartRow(true);
            startDate.setStartDate(new Date());
            startDate.setShowIfCondition(new FormItemIfFunction() {
                public boolean execute(FormItem formItem, Object o, DynamicForm dynamicForm) {
                    return !ExecutionSchedule.Start.Immediately.name().equals(form.getValueAsString("start"))
                        && !"Once".equals(form.getValueAsString("recurr"));
                }
            });

            final RadioGroupItem end = new RadioGroupItem("endType", MSG.view_operationCreateWizard_schedulingStep_label_endDate());
            end.setShowIfCondition(new FormItemIfFunction() {
                public boolean execute(FormItem formItem, Object o, DynamicForm dynamicForm) {
                    return !ExecutionSchedule.Start.Immediately.name().equals(form.getValueAsString("start"))
                        && !"Once".equals(form.getValueAsString("recurr"));
                }
            });
            end.setStartRow(true);
            // TODO: i18n
            end.setValueMap("Never", "End On");
            end.setRedrawOnChange(true);
            end.setValue("Never");

            DateItem endDate = new DateItem("endDate", MSG.view_operationCreateWizard_schedulingStep_label_endDate());
            endDate.setStartRow(true);
            endDate.setStartDate(new Date());
            endDate.setShowIfCondition(new FormItemIfFunction() {
                public boolean execute(FormItem formItem, Object o, DynamicForm dynamicForm) {
                    return !ExecutionSchedule.Start.Immediately.name().equals(form.getValueAsString("start"))
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

            StaticTextItem messageItem = new StaticTextItem("message");
            messageItem.setValue(MSG.view_operationCreateWizard_schedulingStep_label_willExecuteImmediately());
            messageItem.setStartRow(true);
            messageItem.setColSpan(3);
            messageItem.setShowTitle(false);
            messageItem.setCellStyle("HeaderLabel");

            form.setItems(start, scheduleHeader, recurr, onceForm, everyNMinuteForm, hourlyForm, dailyForm, weeklyForm,
                monthlyForm, rangeHeader, startDate, end, endDate, new SpacerItem(), messageItem);

            form.addItemChangedHandler(this);
        }

        return form;
    }

    private DynamicForm getOnceForm() {
        DynamicForm form = new DynamicForm();
        form.setValuesManager(valuesManager);
        form.setPadding(10);
        form.setIsGroup(true);
        form.setGroupTitle(MSG.view_operationCreateWizard_schedulingStep_label_onceAt());
        form.addItemChangedHandler(this);

        form.setWrapItemTitles(false);
        form.setNumCols(2);

        DateItem startDate = new DateItem("onceStartDate", MSG.view_operationCreateWizard_schedulingStep_label_startDate());
        startDate.setValue(new Date());

        TimeItem startTime = new TimeItem("onceStartDate", MSG.view_operationCreateWizard_schedulingStep_label_startDate());
        startTime.setValue(new Date());
        startTime.setDisplayFormat(TimeFormatter.TOSHORTPADDEDTIME);
        startTime.setUseMask(true);
        form.setItems(startDate, startTime);
        return form;
    }

    private DynamicForm getEveryNMinutesForm() {
        DynamicForm form = new DynamicForm();
        form.setValuesManager(valuesManager);
        form.setPadding(10);
        form.setIsGroup(true);
        form.setGroupTitle(MSG.view_operationCreateWizard_schedulingStep_label_everyNMinutes());
        form.addItemChangedHandler(this);

        form.setWrapItemTitles(false);
        form.setNumCols(2);

        IntegerItem everyNMinutes = new IntegerItem();
        everyNMinutes.setName("minuteInterval");
        everyNMinutes.setTitle(MSG.view_operationCreateWizard_schedulingStep_label_minuteInterval());
        everyNMinutes.setValue(60);

        form.setItems(everyNMinutes);
        return form;
    }

    private DynamicForm getHourlyForm() {
        DynamicForm form = new DynamicForm();
        form.setValuesManager(valuesManager);
        form.setPadding(10);
        form.setIsGroup(true);
        form.setGroupTitle(MSG.view_operationCreateWizard_schedulingStep_label_hourly());
        form.addItemChangedHandler(this);

        form.setWrapItemTitles(false);
        form.setNumCols(2);

        IntegerItem minuteOfHour = new IntegerItem();
        minuteOfHour.setName("minuteOfHour");
        minuteOfHour.setTitle(MSG.view_operationCreateWizard_schedulingStep_label_minuteOfHour());
        minuteOfHour.setValue(5);

        form.setItems(minuteOfHour);
        return form;
    }

    private DynamicForm getDailyForm() {
        DynamicForm form = new DynamicForm();
        form.setValuesManager(valuesManager);
        form.setPadding(10);
        form.setWrapItemTitles(false);
        form.setIsGroup(true);
        form.setGroupTitle(MSG.view_operationCreateWizard_schedulingStep_label_daily());
        form.addItemChangedHandler(this);

        TimeItem timeOfDay = new TimeItem("timeOfDay", MSG.view_operationCreateWizard_schedulingStep_label_timeOfDay());
        timeOfDay.setValue(new Date());
        timeOfDay.setDisplayFormat(TimeFormatter.TOSHORTPADDEDTIME);
        timeOfDay.setUseMask(true);

        form.setItems(timeOfDay);
        return form;
    }

    private DynamicForm getWeeklyForm() {
        DynamicForm form = new DynamicForm();
        form.setValuesManager(valuesManager);
        form.setPadding(10);
        form.setIsGroup(true);
        form.setGroupTitle(MSG.view_operationCreateWizard_schedulingStep_label_weekly());
        form.addItemChangedHandler(this);

        form.setWrapItemTitles(false);

        TimeItem timeOfDay = new TimeItem("dayOfWeek", MSG.view_operationCreateWizard_schedulingStep_label_dayOfWeek());
        timeOfDay.setValue(new Date());
        timeOfDay.setDisplayFormat(TimeFormatter.TOSHORTPADDEDTIME);
        timeOfDay.setUseMask(true);

        ExecutionSchedule.DayOfWeek[] daysOfWeek = ExecutionSchedule.DayOfWeek.values();
        List<String> dayList = new ArrayList<String>();
        for (ExecutionSchedule.DayOfWeek dayOfWeek : daysOfWeek) {
            dayList.add(dayOfWeek.name());
        }
        FormItem[] items = new FormItem[8];
        items[0] = timeOfDay;
        int i = 1;
        for (String day : dayList) {
            CheckboxItem dayItem = new CheckboxItem(day);
            dayItem.setShowTitle(false);
            items[i++] = dayItem;
        }
        form.setItems(items);
        return form;
    }

    private DynamicForm getMonthlyForm() {
        DynamicForm form = new DynamicForm();
        form.setValuesManager(valuesManager);
        form.setPadding(10);
        form.setWrapItemTitles(false);
        form.setIsGroup(true);
        form.setGroupTitle(MSG.view_operationCreateWizard_schedulingStep_label_monthly());
        form.addItemChangedHandler(this);

        IntegerItem dayItem = new IntegerItem();
        dayItem.setName("dayOfMonth");
        dayItem.setTitle(MSG.view_operationCreateWizard_schedulingStep_label_dayOfMonth());
        dayItem.setValue(1);

        TimeItem timeOfDay = new TimeItem("timeOfDay", MSG.view_operationCreateWizard_schedulingStep_label_timeOfDay());
        timeOfDay.setValue(new Date());
        timeOfDay.setDisplayFormat(TimeFormatter.TOSHORTPADDEDTIME);
        timeOfDay.setUseMask(true);

        form.setItems(dayItem, timeOfDay);
        return form;
    }

    public boolean nextPage() {
        return true; // TODO: Implement this method.
    }

    public String getName() {
        return MSG.view_operationCreateWizard_schedulingStep_name();
    }

    public void onItemChanged(ItemChangedEvent itemChangeEvent) {

        executionSchedule.setStart(ExecutionSchedule.Start.valueOf(valuesManager.getValueAsString("start")));
        executionSchedule.setRecurr(ExecutionSchedule.Recurr.valueOf(valuesManager.getValueAsString("recurr")));

        Date onceDate = (Date) valuesManager.getValues().get("onceStartDate");
        executionSchedule.setOnceDateTime(onceDate);

        executionSchedule.setMinuteInterval(Integer.parseInt(valuesManager.getValueAsString("minuteInterval")));

        executionSchedule.setMinuteInHour(Integer.parseInt(valuesManager.getValueAsString("minuteOfHour")));

        executionSchedule.setTimeOfDay((Date) valuesManager.getValues().get("timeOfDay"));

        HashSet<ExecutionSchedule.DayOfWeek> selectedDays = new HashSet<ExecutionSchedule.DayOfWeek>(); //.noneOf(ExecutionSchedule.DayOfWeek.class);
        for (ExecutionSchedule.DayOfWeek d : EnumSet.allOf(ExecutionSchedule.DayOfWeek.class)) {
            if (valuesManager.getValues().containsKey(d.name())) {
                selectedDays.add(d);
            }
        }
        executionSchedule.setDaysOfWeek(selectedDays);

        executionSchedule.setDayOfMonth(Integer.parseInt(valuesManager.getValueAsString("dayOfMonth")));

        executionSchedule.setStartDate((Date) valuesManager.getValues().get("startDate"));

        if ("Never".equals(valuesManager.getValues().get("endType"))) {
            executionSchedule.setEndDate(null);
        } else {
            executionSchedule.setEndDate((Date) valuesManager.getValues().get("endDate"));
        }

        form.setValue("message", executionSchedule.getMessage());
    }

    private LinkedHashMap<String, String> enumValueMap(Class<? extends Enum> e) {
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        for (Object o : EnumSet.allOf(e)) {
            Enum v = (Enum) o;
            map.put(v.name(), v.name()); // localize
        }
        return map;
    }

    public ExecutionSchedule getExecutionSchedule() {
        return executionSchedule;
    }

}
