/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.coregui.client.bundle.create;

import java.util.LinkedHashMap;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.FormLayoutType;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.FormSubmitFailedEvent;
import com.smartgwt.client.widgets.form.events.FormSubmitFailedHandler;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.PasswordItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;

import org.rhq.core.domain.bundle.BundleNotFoundException;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.bundle.composite.BundleGroupAssignmentComposite;
import org.rhq.core.domain.criteria.BundleVersionCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.components.form.RadioGroupWithComponentsItem;
import org.rhq.coregui.client.components.upload.BundleDistributionFileUploadForm;
import org.rhq.coregui.client.components.upload.DynamicCallbackForm;
import org.rhq.coregui.client.components.upload.DynamicFormHandler;
import org.rhq.coregui.client.components.upload.DynamicFormSubmitCompleteEvent;
import org.rhq.coregui.client.components.upload.TextFileRetrieverForm;
import org.rhq.coregui.client.components.wizard.AbstractWizardStep;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.StringUtility;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.coregui.client.util.message.Message.Severity;

public class BundleUploadDistroFileStep extends AbstractWizardStep {

    private final AbstractBundleCreateWizard wizard;

    private DynamicForm mainCanvasForm;
    private TextItem urlTextItem;
    private TextItem urlUserNameItem;
    private PasswordItem urlPasswordItem;
    private BundleDistributionFileUploadForm uploadDistroForm;
    private DynamicCallbackForm recipeForm;
    private RadioGroupWithComponentsItem radioGroup;

    private static final String URL_OPTION = MSG.view_bundle_createWizard_urlOption();
    private static final String UPLOAD_OPTION = MSG.view_bundle_createWizard_uploadOption();
    private static final String RECIPE_OPTION = MSG.view_bundle_createWizard_recipeOption();
    private static final String URL_OPTION_USERNAME = MSG.common_title_username();
    private static final String URL_OPTION_PASSWORD = MSG.common_title_password();
    private static final String URL_OPTION_TOOLTIP = MSG.view_bundle_createWizard_urlTooltip();

    final String BUNDLE_NOT_FOUND_EXCEPTION_PATTERN_START = "org.rhq.core.domain.bundle.BundleNotFoundException:[";
    final String BUNDLE_NOT_FOUND_EXCEPTION_PATTERN_END = "]";

    public BundleUploadDistroFileStep(AbstractBundleCreateWizard bundleCreationWizard) {
        this.wizard = bundleCreationWizard;
    }

    @Override
    public Canvas getCanvas() {
        if (mainCanvasForm == null) {
            LinkedHashMap<String, DynamicForm> radioItems = new LinkedHashMap<String, DynamicForm>();
            radioItems.put(URL_OPTION, createUrlForm());
            radioItems.put(UPLOAD_OPTION, createUploadForm());
            radioItems.put(RECIPE_OPTION, createRecipeForm());

            mainCanvasForm = new RadioDynamicForm();

            radioGroup = new RadioGroupWithComponentsItem("bundleDistRadioGroup",
                MSG.view_bundle_createWizard_bundleDistro(), radioItems, mainCanvasForm);
            radioGroup.setShowTitle(false);

            mainCanvasForm.setItems(radioGroup);
        }

        // If we've already created a bundle version, don't allow the user to submit something else.
        // The user must hit the cancel button and start over if they want to use a different bundle distribution file.
        if (wizard.getBundleVersion() != null) {
            mainCanvasForm.setDisabled(true);
        }

        return mainCanvasForm;
    }

    @Override
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

    @Override
    public String getName() {
        return MSG.view_bundle_createWizard_provideBundleDistro();
    }

    private DynamicForm createUrlForm() {
        urlTextItem = new TextItem("url", URL_OPTION);
        urlTextItem.setTooltip(URL_OPTION_TOOLTIP);
        urlTextItem.setRequired(false);
        urlTextItem.setShowTitle(false);
        urlTextItem.setWidth(400);
        urlTextItem.setColSpan(4);
        urlUserNameItem = new TextItem("username", URL_OPTION_USERNAME);
        urlUserNameItem.setTooltip(URL_OPTION_TOOLTIP);
        urlUserNameItem.setRequired(false);
        urlUserNameItem.setShowTitle(true);
        urlUserNameItem.setWidth(100);
        urlUserNameItem.setColSpan(1);
        urlUserNameItem.setAlign(Alignment.LEFT);
        urlPasswordItem = new PasswordItem("password", URL_OPTION_PASSWORD);
        urlPasswordItem.setAttribute("autocomplete", "off");
        urlPasswordItem.setTooltip(URL_OPTION_TOOLTIP);
        urlPasswordItem.setRequired(false);
        urlPasswordItem.setShowTitle(true);
        urlPasswordItem.setWidth(100);
        urlPasswordItem.setColSpan(1);
        urlUserNameItem.setAlign(Alignment.RIGHT);

        DynamicForm urlForm = new DynamicForm();
        urlForm.setItemLayout(FormLayoutType.TABLE);
        urlForm.setNumCols(4);
        urlForm.setPadding(20);
        urlForm.setWidth100();

        urlForm.setItems(urlTextItem, urlUserNameItem, urlPasswordItem);
        return urlForm;
    }

    private BundleDistributionFileUploadForm createUploadForm() {
        uploadDistroForm = new BundleDistributionFileUploadForm(false);
        uploadDistroForm.setPadding(20);
        uploadDistroForm.setWidth(400);
        uploadDistroForm.addFormHandler(new DynamicFormHandler() {
            @Override
            public void onSubmitComplete(DynamicFormSubmitCompleteEvent event) {
                processUpload();
            }
        });
        uploadDistroForm.addFormSubmitFailedHandler(new FormSubmitFailedHandler() {
            @Override
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

        final ButtonItem showUpload = new ButtonItem("recipeUploadLink",
            MSG.view_bundle_createWizard_clickToUploadRecipe());
        showUpload.setIcon(ImageManager.getUploadIcon());

        final CanvasItem upload = new CanvasItem("recipeUploadCanvas");
        upload.setShowTitle(false);
        upload.setVisible(false);

        final TextFileRetrieverForm textFileRetrieverForm = new TextFileRetrieverForm();
        upload.setCanvas(textFileRetrieverForm);

        showUpload.addClickHandler(new ClickHandler() {
            @Override
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
            /*
             * Helper method to unescape a string which has been escaped for inclusion in HTML tags
             */
            public String htmlUnescape(String escapedHtml) {
                Element e = Document.get().createDivElement();
                e.setInnerHTML(escapedHtml);
                return e.getInnerText();
            }

            @Override
            public void onSubmitComplete(DynamicFormSubmitCompleteEvent event) {
                wizard.setRecipe(htmlUnescape(event.getResults()));
                recipe.setValue(htmlUnescape(event.getResults()));
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
        String urlUserName = (String) this.urlUserNameItem.getValue();
        String urlPassword = (String) this.urlPasswordItem.getValue();

        if (urlString == null || urlString.trim().length() == 0) {
            wizard.getView().showMessage(MSG.view_bundle_createWizard_enterUrl());
            wizard.setBundleVersion(null);
            setButtonsDisableMode(false);
            return;
        }

        // if upload takes more than 10m, you have other things to worry about
        GWTServiceLookup.getBundleService(10 * 60 * 1000).createOrStoreBundleVersionViaURL(urlString, urlUserName,
            urlPassword, new AsyncCallback<BundleVersion>() {
                @Override
                public void onSuccess(BundleVersion result) {
                    CoreGUI.getMessageCenter().notify(
                        new Message(
                            MSG.view_bundle_createWizard_createSuccessful(result.getName(), result.getVersion()),
                            Message.Severity.Info));
                    wizard.setBundleVersion(result);
                    setButtonsDisableMode(false);
                    incrementStep(); // go to the next step
                }

                @Override
                public void onFailure(Throwable caught) {
                    // This signals that the bundle does not yet exist
                    BundleNotFoundException bnfe = unpackBundleNotFoundException(caught);
                    if (bnfe != null) {
                        handleBundleNotFoundException(bnfe);

                    } else {
                        // Escape it, since it contains the URL, which the user entered.
                        String message = StringUtility.escapeHtml(caught.getMessage());
                        wizard.getView().showMessage(message);
                        CoreGUI.getErrorHandler().handleError(MSG.view_bundle_createWizard_createFailure(), caught);
                        wizard.setBundleVersion(null);
                        setButtonsDisableMode(false);
                    }
                }
            });
    }

    private BundleNotFoundException unpackBundleNotFoundException(Throwable caught) {
        if (caught instanceof RuntimeException) {
            String message = caught.getMessage();
            int patternStart = message.indexOf(BUNDLE_NOT_FOUND_EXCEPTION_PATTERN_START);
            if (patternStart > -1) {
                int patternEnd = message.indexOf(BUNDLE_NOT_FOUND_EXCEPTION_PATTERN_END,
                        patternStart + BUNDLE_NOT_FOUND_EXCEPTION_PATTERN_START.length());
                if (patternEnd > -1) {
                    return new BundleNotFoundException(
                        message.substring(
                            patternStart + BUNDLE_NOT_FOUND_EXCEPTION_PATTERN_START.length(),
                            patternEnd));
                }
            }
        }
        return null;
    }

    private void handleBundleNotFoundException(BundleNotFoundException e) {
        String token = e.getMessage();
        if (null == token || token.isEmpty()) {
            wizard.getView().showMessage("BundleNotFound: Unexpected failure creating bundle version.");
            CoreGUI.getErrorHandler().handleError(MSG.view_bundle_createWizard_createFailure(), e);
            wizard.setBundleVersion(null);
            setButtonsDisableMode(false);
        }

        wizard.setCreateInitialBundleVersionToken(token);
        setButtonsDisableMode(false);
        incrementStep(); // go to the next step
    }

    private void incrementStep() {
        // before moving to the next step, get the assignable/assigned bundle groups for this new bundle version
        boolean isInitialVersion = this.wizard.getBundleVersion() == null
            || this.wizard.getBundleVersion().getVersionOrder() == 0;
        int bundleId = isInitialVersion ? 0 : this.wizard.getBundleVersion().getBundle().getId();

        GWTServiceLookup.getBundleService().getAssignableBundleGroups(bundleId,
            new AsyncCallback<BundleGroupAssignmentComposite>() {

            public void onSuccess(BundleGroupAssignmentComposite result) {
                wizard.setBundleGroupAssignmentComposite(result);
                wizard.getView().incrementStep(); // go to the next step
            }

            public void onFailure(Throwable caught) {
                setButtonsDisableMode(false);
                String message = MSG.view_bundle_createWizard_groupsStep_failedGetAssignable();
                wizard.getView().showMessage(message);
                CoreGUI.getErrorHandler().handleError(message, caught);
            }
        });
    }

    private void processUpload() {
        if (Boolean.TRUE.equals(uploadDistroForm.getUploadResult())) {
            if (null != uploadDistroForm.getCreateInitialBundleVersionToken()) {
                handleBundleNotFoundException(new BundleNotFoundException(
                    uploadDistroForm.getCreateInitialBundleVersionToken()));

            } else {
                int bvId = uploadDistroForm.getBundleVersionId();
                BundleVersionCriteria criteria = new BundleVersionCriteria();
                criteria.addFilterId(bvId);
                criteria.fetchBundle(true);
                GWTServiceLookup.getBundleService().findBundleVersionsByCriteria(criteria,
                    new AsyncCallback<PageList<BundleVersion>>() {
                    @Override
                    public void onSuccess(PageList<BundleVersion> result) {
                        BundleVersion bv = result.get(0);
                        CoreGUI.getMessageCenter().notify(
                            new Message(MSG.view_bundle_createWizard_createSuccessful(bv.getName(), bv.getVersion()),
                                Message.Severity.Info));
                        wizard.setBundleVersion(bv);
                        setButtonsDisableMode(false);
                        incrementStep(); // go to the next step
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        wizard.getView().showMessage(caught.getMessage());
                        CoreGUI.getErrorHandler().handleError(MSG.view_bundle_createWizard_createFailure(), caught);
                        wizard.setBundleVersion(null);
                        setButtonsDisableMode(false);
                    }
                });
            }
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
        GWTServiceLookup.getBundleService().createBundleVersionViaRecipe(this.wizard.getRecipe(),
            new AsyncCallback<BundleVersion>() {
            @Override
            public void onSuccess(BundleVersion result) {
                CoreGUI.getMessageCenter().notify(
                    new Message(MSG.view_bundle_createWizard_createSuccessful(result.getName(), result.getVersion()),
                        Message.Severity.Info));
                wizard.setBundleVersion(result);
                setButtonsDisableMode(false);
                incrementStep(); // go to the next step
            }

            @Override
            public void onFailure(Throwable caught) {
                boolean handled = false;
                String message = caught.getMessage();

                if (message.contains("PermissionException") && message.contains("initial")) {
                    handled = true;
                    wizard.setCreateInitialBundleVersionRecipe(wizard.getRecipe());
                    setButtonsDisableMode(false);
                    incrementStep(); // go to the next step
                }

                if (!handled) {
                    wizard.getView().showMessage(caught.getMessage());
                    CoreGUI.getErrorHandler().handleError(MSG.view_bundle_createWizard_createFailure(), caught);
                    wizard.setBundleVersion(null);
                    wizard.setRecipe("");
                    setButtonsDisableMode(false);
                }
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

    private class RadioDynamicForm extends DynamicForm {

        public RadioDynamicForm() {
            super();
        }

        @Override
        public void destroy() {
            radioGroup.destroyComponents();
            super.destroy();
        }
    }
}
