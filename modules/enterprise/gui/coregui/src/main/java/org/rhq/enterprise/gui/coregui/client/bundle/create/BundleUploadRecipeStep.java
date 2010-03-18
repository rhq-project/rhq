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
import com.smartgwt.client.widgets.form.fields.LinkItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;

import org.rhq.enterprise.gui.coregui.client.components.upload.DynamicCallbackForm;
import org.rhq.enterprise.gui.coregui.client.components.upload.DynamicFormHandler;
import org.rhq.enterprise.gui.coregui.client.components.upload.DynamicFormSubmitCompleteEvent;
import org.rhq.enterprise.gui.coregui.client.components.upload.TextFileRetrieverForm;
import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardStep;

public class BundleUploadRecipeStep implements WizardStep {

    private DynamicCallbackForm form;
    private final BundleCreationWizard wizard;
    private TextAreaItem recipe;

    public BundleUploadRecipeStep(BundleCreationWizard bundleCreationWizard) {
        this.wizard = bundleCreationWizard;
    }

    public Canvas getCanvas() {
        if (form == null) {
            form = new DynamicCallbackForm("uploadRecipeStepForm");
            form.setWidth100();
            form.setNumCols(1);
            form.setMargin(Integer.valueOf(20));

            final LinkItem showUpload = new LinkItem("showUpload");
            showUpload.setValue("Click To Upload A Recipe File");
            showUpload.setShowTitle(false);

            final CanvasItem upload = new CanvasItem("upload");
            upload.setShowTitle(false);

            final TextFileRetrieverForm textFileRetrieverForm = new TextFileRetrieverForm();
            upload.setCanvas(textFileRetrieverForm);

            showUpload.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    form.hideItem("showUpload");
                    form.showItem("upload");
                }
            });

            recipe = new TextAreaItem("recipe");
            recipe.setShowTitle(false);
            recipe.setRequired(true);
            recipe.setWidth("*");
            recipe.setHeight("*");
            recipe.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent event) {
                    Object value = event.getValue();
                    if (value == null) {
                        value = "";
                    }
                    wizard.setRecipe(value.toString());
                    enableNextButtonWhenAppropriate();
                }
            });

            textFileRetrieverForm.addFormHandler(new DynamicFormHandler() {
                public void onSubmitComplete(DynamicFormSubmitCompleteEvent event) {
                    wizard.setRecipe(event.getResults());
                    recipe.setValue(event.getResults());
                    textFileRetrieverForm.retrievalStatus(true);
                    form.showItem("showUpload");
                    form.hideItem("upload");
                    enableNextButtonWhenAppropriate();
                }
            });

            form.setItems(showUpload, upload, recipe);
            form.hideItem("upload");
        } else {
            // we are traversing back to this step - don't allow the recipe to change if we've already created the bundle version
            if (wizard.getBundleVersion() != null) {
                recipe.setValue(wizard.getBundleVersion().getRecipe());
                recipe.setDisabled(Boolean.TRUE);
                form.hideItem("showUpload");
                form.hideItem("upload");
            }
        }
        return form;
    }

    public boolean nextPage() {
        return form.validate();
    }

    public String getName() {
        return "Provide Bundle Recipe";
    }

    public boolean isNextEnabled() {
        return this.wizard.getRecipe() != null && this.wizard.getRecipe().trim().length() > 0;
    }

    public boolean isPreviousEnabled() {
        return true;
    }

    private void enableNextButtonWhenAppropriate() {
        this.wizard.getView().getNextButton().setDisabled(!isNextEnabled());
    }
}
