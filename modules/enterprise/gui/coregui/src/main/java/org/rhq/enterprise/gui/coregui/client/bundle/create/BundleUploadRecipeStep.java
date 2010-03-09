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
package org.rhq.enterprise.gui.coregui.client.bundle.create;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.FormItemIfFunction;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.RadioGroupItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;

import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardStep;

/**
 * @author Greg Hinkle
 */
public class BundleUploadRecipeStep implements WizardStep {

    private DynamicForm form;

    public Canvas getCanvas() {
        if (form == null) {
            form = new DynamicForm();
            form.setWidth100();
            form.setNumCols(2);
            form.setColWidths("50%", "*");

            final RadioGroupItem radioGroupItem = new RadioGroupItem("reciperadio");
            radioGroupItem.setTitle("");
            radioGroupItem.setValueMap("Upload Recipe", "Write Recipe");

            final TextItem recipeFileUploadItem = new TextItem("recipefile", "Recipe File");
            recipeFileUploadItem.setVisible(false);

            final TextAreaItem recipeTextAreaItem = new TextAreaItem("recipetext", "Recipe Text");
            recipeTextAreaItem.setVisible(false);

            recipeFileUploadItem.setShowIfCondition(new FormItemIfFunction() {
                public boolean execute(FormItem item, Object value, DynamicForm form) {
                    System.out.println("!!!!!!!!!!!!!!" + form.getValues());
                    return (((RadioGroupItem) form.getValue("reciperadio")).getValue().equals("recipefile"));
                }
            });

            recipeTextAreaItem.setShowIfCondition(new FormItemIfFunction() {
                public boolean execute(FormItem item, Object value, DynamicForm form) {
                    System.out.println("!!!!!!!!!!!!!!" + form.getValues());
                    return (((RadioGroupItem) form.getValue("reciperadio")).getValue().equals("recipetext"));
                }
            });

            form.setItems(radioGroupItem, recipeFileUploadItem, recipeTextAreaItem);
        }
        return form;
    }

    public boolean valid() {
        return false; // TODO: Implement this method.
    }

    public String getName() {
        return "Setup Bundle Recipe Information";
    }

}