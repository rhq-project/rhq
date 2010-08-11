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

import java.util.LinkedHashMap;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.FormSubmitFailedEvent;
import com.smartgwt.client.widgets.form.events.FormSubmitFailedHandler;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.LinkItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;

import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.criteria.BundleVersionCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.form.RadioGroupWithComponentsItem;
import org.rhq.enterprise.gui.coregui.client.components.upload.BundleDistributionFileUploadForm;
import org.rhq.enterprise.gui.coregui.client.components.upload.DynamicCallbackForm;
import org.rhq.enterprise.gui.coregui.client.components.upload.DynamicFormHandler;
import org.rhq.enterprise.gui.coregui.client.components.upload.DynamicFormSubmitCompleteEvent;
import org.rhq.enterprise.gui.coregui.client.components.upload.TextFileRetrieverForm;
import org.rhq.enterprise.gui.coregui.client.components.wizard.AbstractWizardStep;
import org.rhq.enterprise.gui.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;

public class BundleUploadDistroFileStep extends AbstractWizardStep {

    private final AbstractBundleCreateWizard wizard;

    private DynamicForm mainCanvasForm;
    private TextItem urlTextItem;
    private BundleDistributionFileUploadForm uploadDistroForm;
    private DynamicCallbackForm recipeForm;
    private RadioGroupWithComponentsItem radioGroup;

    public BundleUploadDistroFileStep(AbstractBundleCreateWizard bundleCreationWizard) {
        this.wizard = bundleCreationWizard;
    }

    public Canvas getCanvas() {
        if (mainCanvasForm == null) {
            LinkedHashMap<String, DynamicForm> radioItems = new LinkedHashMap<String, DynamicForm>();
            radioItems.put("URL", createUrlForm());
            radioItems.put("Upload", createUploadForm());
            radioItems.put("Recipe", createRecipeForm());

            mainCanvasForm = new DynamicForm();
            radioGroup = new RadioGroupWithComponentsItem("bundleDistroRadioGroup", "Bundle Distribution", radioItems,
                mainCanvasForm);
            radioGroup.setShowTitle(false);

            mainCanvasForm.setItems(radioGroup);
        }

        // If we've already created a bundle verison, don't allow the user to submit something else.
        // The user must hit the cancel button and start over if they want to use a different bundle distribution file.
        if (wizard.getBundleVersion() != null) {
            mainCanvasForm.setDisabled(true);
        }

        return mainCanvasForm;
    }

    public boolean nextPage() {
        wizard.getView().hideMessage();

        if (uploadDistroForm.isUploadInProgress()) {
            handleUploadError("Upload is in progress... This can take several minutes for large distribution files.",
                false);
            return false;
        }

        if (wizard.getBundleVersion() == null) {
            String selected = radioGroup.getSelected();

            if ("URL".equals(selected)) {
                processUrl();
            } else if ("Upload".equals(selected)) {
                uploadDistroForm.submitForm();
                // on certain errors the form may never be submitted, report these errors outside submit handlers
                handleUploadError(uploadDistroForm.getUploadError(), false);
            } else if ("Recipe".equals(selected)) {
                processRecipe();
            } else {
                wizard.getView().showMessage("You must choose one option for creating your Bundle!");
                return false;
            }
            return false;
        } else {
            // there is already a bundle version, so we must have created it already
            // and the user must have reached here after hitting the previous button earlier;
            // just move to the next step to let the user peruse the wizard steps
            return true;
        }
    }

    public String getName() {
        return "Provide A Bundle Distribution";
    }

    private DynamicForm createUrlForm() {
        urlTextItem = new TextItem("url", "URL");
        urlTextItem.setRequired(false);
        urlTextItem.setShowTitle(false);
        urlTextItem.setWidth(400);
        DynamicForm urlForm = new DynamicForm();
        urlForm.setPadding(20);
        urlForm.setWidth100();
        urlForm.setItems(urlTextItem);
        return urlForm;
    }

    private BundleDistributionFileUploadForm createUploadForm() {
        uploadDistroForm = new BundleDistributionFileUploadForm(false);
        uploadDistroForm.setPadding(20);
        uploadDistroForm.addFormHandler(new DynamicFormHandler() {
            public void onSubmitComplete(DynamicFormSubmitCompleteEvent event) {
                processUpload();
            }
        });
        uploadDistroForm.addFormSubmitFailedHandler(new FormSubmitFailedHandler() {
            public void onFormSubmitFailed(FormSubmitFailedEvent event) {
                return; // the distro form component will log an error for us
            }
        });
        return uploadDistroForm;
    }

    private DynamicForm createRecipeForm() {
        recipeForm = new DynamicCallbackForm("recipeForm");
        recipeForm.setMargin(Integer.valueOf(20));
        recipeForm.setShowInlineErrors(false);

        final LinkItem showUpload = new LinkItem("recipeUploadLink");
        showUpload.setValue("Click To Upload A Recipe File");
        showUpload.setShowTitle(false);

        final CanvasItem upload = new CanvasItem("recipeUploadCanvas");
        upload.setShowTitle(false);
        upload.setVisible(false);

        final TextFileRetrieverForm textFileRetrieverForm = new TextFileRetrieverForm();
        upload.setCanvas(textFileRetrieverForm);

        showUpload.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                recipeForm.hideItem(showUpload.getName());
                recipeForm.showItem(upload.getName());
            }
        });

        final TextAreaItem recipe = new TextAreaItem("recipeText");
        recipe.setShowTitle(false);
        recipe.setRequired(false);
        recipe.setColSpan(4);
        recipe.setWidth(400);
        recipe.setHeight(150);

        textFileRetrieverForm.addFormHandler(new DynamicFormHandler() {
            public void onSubmitComplete(DynamicFormSubmitCompleteEvent event) {
                wizard.setRecipe(event.getResults());
                recipe.setValue(event.getResults());
                textFileRetrieverForm.retrievalStatus(true);
                recipeForm.showItem(showUpload.getName());
                recipeForm.hideItem(upload.getName());
            }
        });

        recipeForm.setItems(showUpload, upload, recipe);

        return recipeForm;
    }

    private void processUrl() {
        String urlString = (String) this.urlTextItem.getValue();

        BundleGWTServiceAsync bundleServer = GWTServiceLookup.getBundleService();
        bundleServer.createBundleVersionViaURL(urlString, new AsyncCallback<BundleVersion>() {
            public void onSuccess(BundleVersion result) {
                CoreGUI.getMessageCenter().notify(
                    new Message("Created bundle [" + result.getName() + "] version [" + result.getVersion() + "]",
                        Message.Severity.Info));
                wizard.setBundleVersion(result);
                setButtonsDisableMode(false);
                wizard.getView().incrementStep(); // go to the next step
            }

            public void onFailure(Throwable caught) {
                wizard.getView().showMessage(caught.getMessage());
                CoreGUI.getErrorHandler().handleError("Failed to create bundle", caught);
                wizard.setBundleVersion(null);
                setButtonsDisableMode(false);
            }
        });
    }

    private void processUpload() {
        if (Boolean.TRUE.equals(uploadDistroForm.getUploadResult())) {
            int bvId = uploadDistroForm.getBundleVersionId();
            BundleVersionCriteria criteria = new BundleVersionCriteria();
            criteria.addFilterId(bvId);
            criteria.fetchBundle(true);
            BundleGWTServiceAsync bundleServer = GWTServiceLookup.getBundleService();
            bundleServer.findBundleVersionsByCriteria(criteria, new AsyncCallback<PageList<BundleVersion>>() {
                public void onSuccess(PageList<BundleVersion> result) {
                    BundleVersion bv = result.get(0);
                    CoreGUI.getMessageCenter().notify(
                        new Message("Created bundle [" + bv.getName() + "] version [" + bv.getVersion() + "]",
                            Message.Severity.Info));
                    wizard.setBundleVersion(bv);
                    setButtonsDisableMode(false);
                    wizard.getView().incrementStep(); // go to the next step
                }

                public void onFailure(Throwable caught) {
                    wizard.getView().showMessage(caught.getMessage());
                    CoreGUI.getErrorHandler().handleError("Failed to create bundle", caught);
                    wizard.setBundleVersion(null);
                    setButtonsDisableMode(false);
                }
            });
        } else {
            String errorMessage = uploadDistroForm.getUploadError();
            handleUploadError(errorMessage, true);
            wizard.setBundleVersion(null);
            setButtonsDisableMode(false);
        }
    }

    private void processRecipe() {
        this.wizard.setRecipe((String) this.recipeForm.getItem("recipeText").getValue());
        BundleGWTServiceAsync bundleServer = GWTServiceLookup.getBundleService();
        bundleServer.createBundleVersionViaRecipe(this.wizard.getRecipe(), new AsyncCallback<BundleVersion>() {
            public void onSuccess(BundleVersion result) {
                CoreGUI.getMessageCenter().notify(
                    new Message("Created bundle [" + result.getName() + "] version [" + result.getVersion() + "]",
                        Message.Severity.Info));
                wizard.setBundleVersion(result);
                setButtonsDisableMode(false);
                wizard.getView().incrementStep(); // go to the next step
            }

            public void onFailure(Throwable caught) {
                wizard.getView().showMessage(caught.getMessage());
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
    }

    private void handleUploadError(String errorMessage, boolean sendToMessageCenter) {
        if (null != errorMessage) {
            wizard.getView().showMessage(errorMessage);
        } else {
            errorMessage = "";
        }

        if (sendToMessageCenter) {
            CoreGUI.getMessageCenter().notify(
                new Message("Failed to upload bundle distribution file. " + errorMessage, Severity.Error));
        }
    }
}
