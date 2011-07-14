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
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;

import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.criteria.BundleVersionCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.components.form.RadioGroupWithComponentsItem;
import org.rhq.enterprise.gui.coregui.client.components.upload.BundleDistributionFileUploadForm;
import org.rhq.enterprise.gui.coregui.client.components.upload.DynamicCallbackForm;
import org.rhq.enterprise.gui.coregui.client.components.upload.DynamicFormHandler;
import org.rhq.enterprise.gui.coregui.client.components.upload.DynamicFormSubmitCompleteEvent;
import org.rhq.enterprise.gui.coregui.client.components.upload.TextFileRetrieverForm;
import org.rhq.enterprise.gui.coregui.client.components.wizard.AbstractWizardStep;
import org.rhq.enterprise.gui.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.StringUtility;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;
import org.rhq.enterprise.gui.coregui.client.util.selenium.Locatable;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;

public class BundleUploadDistroFileStep extends AbstractWizardStep {

    private final AbstractBundleCreateWizard wizard;

    private DynamicForm mainCanvasForm;
    private TextItem urlTextItem;
    private BundleDistributionFileUploadForm uploadDistroForm;
    private DynamicCallbackForm recipeForm;
    private RadioGroupWithComponentsItem radioGroup;

    private final String URL_OPTION = MSG.view_bundle_createWizard_urlOption();
    private final String UPLOAD_OPTION = MSG.view_bundle_createWizard_uploadOption();
    private final String RECIPE_OPTION = MSG.view_bundle_createWizard_recipeOption();

    public BundleUploadDistroFileStep(AbstractBundleCreateWizard bundleCreationWizard) {
        this.wizard = bundleCreationWizard;
    }

    public Canvas getCanvas(Locatable parent) {
        if (mainCanvasForm == null) {
            LinkedHashMap<String, DynamicForm> radioItems = new LinkedHashMap<String, DynamicForm>();
            radioItems.put(URL_OPTION, createUrlForm());
            radioItems.put(UPLOAD_OPTION, createUploadForm());
            radioItems.put(RECIPE_OPTION, createRecipeForm());

            if (parent != null) {
                mainCanvasForm = new RadioDynamicForm(parent.extendLocatorId("mainCanvasForm"));
            } else {
                mainCanvasForm = new RadioDynamicForm("mainCanvasForm");
            }

            radioGroup = new RadioGroupWithComponentsItem("bundleDistRadioGroup", MSG
                .view_bundle_createWizard_bundleDistro(), radioItems, mainCanvasForm);
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
            handleUploadError(MSG.view_bundle_createWizard_uploadInProgress(), false);
            return false;
        }

        if (wizard.getBundleVersion() == null) {
            String selected = radioGroup.getSelected();

            if (URL_OPTION.equals(selected)) {
                processUrl();
            } else if (UPLOAD_OPTION.equals(selected)) {
                uploadDistroForm.submitForm();
                // on certain errors the form may never be submitted, report these errors outside submit handlers
                handleUploadError(uploadDistroForm.getUploadError(), false);
            } else if (RECIPE_OPTION.equals(selected)) {
                processRecipe();
            } else {
                wizard.getView().showMessage(MSG.view_bundle_createWizard_youMustChooseOne());
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
        return MSG.view_bundle_createWizard_provideBundleDistro();
    }

    private DynamicForm createUrlForm() {
        urlTextItem = new TextItem("url", URL_OPTION);
        urlTextItem.setRequired(false);
        urlTextItem.setShowTitle(false);
        urlTextItem.setWidth(400);
        DynamicForm urlForm = new LocatableDynamicForm("BundleDistUrl");
        urlForm.setPadding(20);
        urlForm.setWidth100();
        urlForm.setItems(urlTextItem);
        return urlForm;
    }

    private BundleDistributionFileUploadForm createUploadForm() {
        uploadDistroForm = new BundleDistributionFileUploadForm("BundleCreateUploadDistFile", false);
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
        recipeForm = new DynamicCallbackForm("BundleRecipe", "recipeForm");
        recipeForm.setMargin(Integer.valueOf(20));
        recipeForm.setShowInlineErrors(false);

        final ButtonItem showUpload = new ButtonItem("recipeUploadLink", MSG
            .view_bundle_createWizard_clickToUploadRecipe());
        showUpload.setIcon(ImageManager.getUploadIcon());

        final CanvasItem upload = new CanvasItem("recipeUploadCanvas");
        upload.setShowTitle(false);
        upload.setVisible(false);

        final TextFileRetrieverForm textFileRetrieverForm = new TextFileRetrieverForm("BundleCreateRecipeUpload");
        upload.setCanvas(textFileRetrieverForm);

        showUpload.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                showUpload.hide();
                upload.show();
                recipeForm.markForRedraw();
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
                showUpload.show();
                upload.hide();
                recipeForm.markForRedraw();
            }
        });

        recipeForm.setItems(showUpload, upload, recipe);

        return recipeForm;
    }

    private void processUrl() {
        String urlString = (String) this.urlTextItem.getValue();

        if (urlString == null || urlString.trim().length() == 0) {
            wizard.getView().showMessage(MSG.view_bundle_createWizard_enterUrl());
            wizard.setBundleVersion(null);
            setButtonsDisableMode(false);
            return;
        }

        BundleGWTServiceAsync bundleServer = GWTServiceLookup.getBundleService(10 * 60 * 1000); // if upload takes more than 10m, you have other things to worry about
        bundleServer.createBundleVersionViaURL(urlString, new AsyncCallback<BundleVersion>() {
            public void onSuccess(BundleVersion result) {
                CoreGUI.getMessageCenter().notify(
                    new Message(MSG.view_bundle_createWizard_createSuccessful(result.getName(), result.getVersion()),
                        Message.Severity.Info));
                wizard.setBundleVersion(result);
                setButtonsDisableMode(false);
                wizard.getView().incrementStep(); // go to the next step
            }

            public void onFailure(Throwable caught) {
                // Escape it, since it contains the URL, which the user entered.
                String message = StringUtility.escapeHtml(caught.getMessage());
                wizard.getView().showMessage(message);
                CoreGUI.getErrorHandler().handleError(MSG.view_bundle_createWizard_createFailure(), caught);
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
                        new Message(MSG.view_bundle_createWizard_createSuccessful(bv.getName(), bv.getVersion()),
                            Message.Severity.Info));
                    wizard.setBundleVersion(bv);
                    setButtonsDisableMode(false);
                    wizard.getView().incrementStep(); // go to the next step
                }

                public void onFailure(Throwable caught) {
                    wizard.getView().showMessage(caught.getMessage());
                    CoreGUI.getErrorHandler().handleError(MSG.view_bundle_createWizard_createFailure(), caught);
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
        String recipeString = (String) this.recipeForm.getItem("recipeText").getValue();

        if (recipeString == null || recipeString.trim().length() == 0) {
            wizard.getView().showMessage(MSG.view_bundle_createWizard_enterRecipe());
            wizard.setBundleVersion(null);
            setButtonsDisableMode(false);
            return;
        }

        this.wizard.setRecipe(recipeString);
        BundleGWTServiceAsync bundleServer = GWTServiceLookup.getBundleService();
        bundleServer.createBundleVersionViaRecipe(this.wizard.getRecipe(), new AsyncCallback<BundleVersion>() {
            public void onSuccess(BundleVersion result) {
                CoreGUI.getMessageCenter().notify(
                    new Message(MSG.view_bundle_createWizard_createSuccessful(result.getName(), result.getVersion()),
                        Message.Severity.Info));
                wizard.setBundleVersion(result);
                setButtonsDisableMode(false);
                wizard.getView().incrementStep(); // go to the next step
            }

            public void onFailure(Throwable caught) {
                wizard.getView().showMessage(caught.getMessage());
                CoreGUI.getErrorHandler().handleError(MSG.view_bundle_createWizard_createFailure(), caught);
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
                new Message(MSG.view_bundle_createWizard_failedToUploadDistroFile(), errorMessage, Severity.Error));
        }
    }

    private class RadioDynamicForm extends LocatableDynamicForm {

        public RadioDynamicForm(String locatorId) {
            super(locatorId);
        }

        @Override
        public void destroy() {
            radioGroup.destroyComponents();
            super.destroy();
        }

    }
}
