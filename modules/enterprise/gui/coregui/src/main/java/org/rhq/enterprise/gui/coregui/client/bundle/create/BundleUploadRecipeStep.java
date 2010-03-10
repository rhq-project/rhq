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
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.HiddenItem;
import com.smartgwt.client.widgets.form.fields.LinkItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;

import org.rhq.enterprise.gui.coregui.client.components.upload.DynamicCallbackForm;
import org.rhq.enterprise.gui.coregui.client.components.upload.DynamicFormHandler;
import org.rhq.enterprise.gui.coregui.client.components.upload.DynamicFormSubmitCompleteEvent;
import org.rhq.enterprise.gui.coregui.client.components.upload.TextFileRetrieverForm;
import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardStep;

/**
 * @author Greg Hinkle
 */
public class BundleUploadRecipeStep implements WizardStep {

    private DynamicCallbackForm form;
    private final BundleCreationWizard wizard;

    public BundleUploadRecipeStep(BundleCreationWizard bundleCreationWizard) {
        this.wizard = bundleCreationWizard;
    }
    public Canvas getCanvas() {

        if (form == null) {


            form = new DynamicCallbackForm("test");
            form.setWidth100();
            form.setNumCols(1);
//            form.setColWidths("25%","75%");
            HiddenItem idField = new HiddenItem("id");
            idField.setValue(1);


            final LinkItem showUpload = new LinkItem("showUpload");
            showUpload.setValue("Upload a file");
            showUpload.setShowTitle(false);


            final CanvasItem upload = new CanvasItem("upload");
            upload.setShowTitle(false);
            TextFileRetrieverForm textFileRetrieverForm = new TextFileRetrieverForm();
            upload.setCanvas(textFileRetrieverForm);



            showUpload.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    form.hideItem("showUpload");
                    form.showItem("upload");
                }
            });


            final TextAreaItem recipe = new TextAreaItem("recipe","Recipe");
            recipe.setShowTitle(false);
            recipe.setRequired(true);

            recipe.setWidth("*");
            recipe.setHeight("*");


            textFileRetrieverForm.addFormHandler(new DynamicFormHandler() {
                public void onSubmitComplete(DynamicFormSubmitCompleteEvent event) {
                    recipe.setValue(event.getResults());
                    form.showItem("showUpload");
                    form.hideItem("upload");
                }
            });

            form.setItems(idField,showUpload,upload, recipe);

            form.hideItem("upload");

        }
        return form;
    }

    public boolean nextPage() {
        return form.validate();
    }

    public String getName() {
        return "Setup Bundle Recipe Information";
    }

}
