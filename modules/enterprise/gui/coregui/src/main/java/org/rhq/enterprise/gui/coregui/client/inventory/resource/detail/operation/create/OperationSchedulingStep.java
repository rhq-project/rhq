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

import org.rhq.enterprise.gui.coregui.client.components.wizard.Wizard;
import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardStep;

import com.smartgwt.client.types.TimeFormatter;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.FormItemIfFunction;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.DateItem;
import com.smartgwt.client.widgets.form.fields.DateTimeItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.RadioGroupItem;
import com.smartgwt.client.widgets.form.fields.TimeItem;

/**
 * @author Greg Hinkle
 */
public class OperationSchedulingStep implements WizardStep {

    private DynamicForm form;

    public Canvas getCanvas() {

        if (form == null) {
            form = new DynamicForm();
            form.setWidth100();
            form.setNumCols(4);
            form.setColWidths("10%", "25%", "10%", "*");

            final RadioGroupItem start = new RadioGroupItem("start", "Start");
            start.setColSpan(3);
            start.setValueMap("Immediately", "Schedule for Future");
            start.setRedrawOnChange(true);
            start.setValue("Immediately");

            DateItem startDate = new DateItem("startDate", "Start Date");
            startDate.setShowIfCondition(new FormItemIfFunction() {
                public boolean execute(FormItem formItem, Object o, DynamicForm dynamicForm) {
                    return !"Immediately".equals(form.getValueAsString("start"));
                }
            });


            TimeItem startTime = new TimeItem("startTime", "Start Time");
            startTime.setDisplayFormat(TimeFormatter.TOSHORTPADDEDTIME);
            startTime.setUseMask(true);
            startTime.setShowIfCondition(new FormItemIfFunction() {
                public boolean execute(FormItem formItem, Object o, DynamicForm dynamicForm) {
                    return !"Immediately".equals(form.getValueAsString("start"));
                }
            });


            RadioGroupItem recurr = new RadioGroupItem("recurr", "Recurrence");
            recurr.setValueMap("None", "Daily", "Weekly", "Monthly", "Yearly");
            recurr.setRedrawOnChange(true);
            recurr.setValue("None");
            recurr.setShowIfCondition(new FormItemIfFunction() {
                public boolean execute(FormItem formItem, Object o, DynamicForm dynamicForm) {
                    return !"Immediately".equals(form.getValueAsString("start"));
                }
            });

            CanvasItem daysItem = new CanvasItem("days", "Days");
            daysItem.setCanvas(getDaysForm());
            daysItem.setShowIfCondition(new FormItemIfFunction() {
                public boolean execute(FormItem formItem, Object o, DynamicForm dynamicForm) {
                    return "Weekly".equals(form.getValueAsString("recurr"));
                }
            });


            DateTimeItem endDate = new DateTimeItem("endDate", "End Date");
            endDate.setStartRow(true);
            endDate.setShowIfCondition(new FormItemIfFunction() {
                public boolean execute(FormItem formItem, Object o, DynamicForm dynamicForm) {
                    return !"None".equals(form.getValueAsString("recurr"));
                }
            });
            TimeItem endTime = new TimeItem("endTime", "End Time");
            endTime.setDisplayFormat(TimeFormatter.TOSHORTPADDEDTIME);
            endTime.setUseMask(true);
            endTime.setShowIfCondition(new FormItemIfFunction() {
                public boolean execute(FormItem formItem, Object o, DynamicForm dynamicForm) {
                    return !"None".equals(form.getValueAsString("recurr"));
                }
            });


            form.setItems(start, startDate, startTime, recurr, daysItem, endDate, endTime);
        }
        return form;
    }


    private DynamicForm getDaysForm() {
        DynamicForm form = new DynamicForm();
        form.setNumCols(2);
        String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        FormItem[] items = new FormItem[7];
        int i = 0;
        for (String day : days) {
            CheckboxItem dayItem = new CheckboxItem(day);
            dayItem.setShowTitle(false);
            items[i++] = dayItem;
        }
        form.setItems(items);
        return form;
    }


    public boolean valid() {
        return false;  // TODO: Implement this method.
    }

    public String getName() {
        return "Schedule";
    }
    
}
