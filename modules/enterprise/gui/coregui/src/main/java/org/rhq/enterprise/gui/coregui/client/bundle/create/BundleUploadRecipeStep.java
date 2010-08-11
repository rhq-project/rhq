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

import java.util.HashMap;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.LinkItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;

import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.upload.DynamicCallbackForm;
import org.rhq.enterprise.gui.coregui.client.components.upload.DynamicFormHandler;
import org.rhq.enterprise.gui.coregui.client.components.upload.DynamicFormSubmitCompleteEvent;
import org.rhq.enterprise.gui.coregui.client.components.upload.TextFileRetrieverForm;
import org.rhq.enterprise.gui.coregui.client.components.wizard.AbstractWizardStep;
import org.rhq.enterprise.gui.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

public class BundleUploadRecipeStep extends AbstractWizardStep {

    private DynamicCallbackForm form;
    private final AbstractBundleCreateWizard wizard;
    private TextAreaItem recipe;
    private CanvasItem validatingItem;

    public BundleUploadRecipeStep(AbstractBundleCreateWizard bundleCreationWizard) {
        this.wizard = bundleCreationWizard;
    }

    public Canvas getCanvas() {
        if (form == null) {
            form = new DynamicCallbackForm("uploadRecipeStepForm");
            form.setWidth100();
            form.setMargin(Integer.valueOf(20));
            form.setShowInlineErrors(false);

            final LinkItem showUpload = new LinkItem("showUpload");
            showUpload.setValue("Click To Upload A Recipe File");
            showUpload.setShowTitle(false);

            final CanvasItem upload = new CanvasItem("upload");
            upload.setShowTitle(false);
            upload.setVisible(false);

            final TextFileRetrieverForm textFileRetrieverForm = new TextFileRetrieverForm();
            upload.setCanvas(textFileRetrieverForm);

            showUpload.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    form.hideItem(showUpload.getName());
                    form.showItem(upload.getName());
                }
            });

            recipe = new TextAreaItem("recipe");
            recipe.setShowTitle(false);
            recipe.setRequired(true);
            recipe.setColSpan(2);
            recipe.setWidth("*");
            recipe.setHeight(220);

            textFileRetrieverForm.addFormHandler(new DynamicFormHandler() {
                public void onSubmitComplete(DynamicFormSubmitCompleteEvent event) {
                    wizard.setRecipe(event.getResults());
                    recipe.setValue(event.getResults());
                    textFileRetrieverForm.retrievalStatus(true);
                    form.showItem(showUpload.getName());
                    form.hideItem(upload.getName());
                }
            });

            validatingItem = new CanvasItem("validating", "Validating");
            validatingItem.setCanvas(new Img("ajax-loader.gif", 16, 16));
            validatingItem.setVisible(false);

            form.setItems(showUpload, upload, recipe, validatingItem);
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

        if (wizard.getBundleVersion() != null) {
            return true;
        } else {
            if (form.validate()) {
                validateAndCreateRecipe(); // this will move to the next step for us
            }
            return false;
        }

    }

    public String getName() {
        return "Provide Bundle Recipe";
    }

    private void validateAndCreateRecipe() {
        form.showItem(validatingItem.getName());
        setButtonsDisableMode(true);

        wizard.setRecipe(recipe.getValue().toString());

        BundleGWTServiceAsync bundleServer = GWTServiceLookup.getBundleService();
        bundleServer.createBundleVersionViaRecipe(this.wizard.getRecipe(), new AsyncCallback<BundleVersion>() {
            public void onSuccess(BundleVersion result) {
                form.hideItem(validatingItem.getName());

                CoreGUI.getMessageCenter().notify(
                    new Message("Created bundle [" + result.getName() + "] version [" + result.getVersion() + "]",
                        Message.Severity.Info));
                wizard.setBundleVersion(result);
                wizard.getView().incrementStep();
                setButtonsDisableMode(false);
                CoreGUI.refresh();
            }

            public void onFailure(Throwable caught) {
                form.hideItem(validatingItem.getName());

                HashMap<String, String> errors = new HashMap<String, String>();
                errors.put(recipe.getName(), "Invalid Recipe: " + caught.getMessage());
                form.setErrors(errors, true);
                CoreGUI.getErrorHandler().handleError("Failed to create bundle", caught);
                wizard.setBundleVersion(null);
                wizard.setRecipe("");
                setButtonsDisableMode(false);
            }
        });
    }

    private void setButtonsDisableMode(boolean disabled) {
        wizard.getView().getCancelButton().setDisabled(disabled);
        wizard.getView().getNextButton().setDisabled(disabled);
        wizard.getView().getPreviousButton().setDisabled(disabled);
    }
}
