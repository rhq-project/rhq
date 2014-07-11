/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.coregui.client.alert.definitions;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.FormSubmitFailedEvent;
import com.smartgwt.client.widgets.form.events.FormSubmitFailedHandler;
import com.smartgwt.client.widgets.form.events.ItemChangeEvent;
import com.smartgwt.client.widgets.form.events.ItemChangeHandler;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.FormItemIcon;
import com.smartgwt.client.widgets.form.fields.PasswordItem;
import com.smartgwt.client.widgets.form.fields.SectionItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.UploadItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;

import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.content.PackageVersionFormatDescription;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.content.composite.PackageAndLatestVersionComposite;
import org.rhq.core.domain.content.composite.PackageTypeAndVersionFormatComposite;
import org.rhq.core.domain.criteria.PackageCriteria;
import org.rhq.core.domain.criteria.RepoCriteria;
import org.rhq.core.domain.criteria.SubjectCriteria;
import org.rhq.core.domain.util.OSGiVersion;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.UserSessionManager;
import org.rhq.coregui.client.components.form.RadioGroupWithComponentsItem;
import org.rhq.coregui.client.components.form.SortedSelectItem;
import org.rhq.coregui.client.components.upload.DynamicFormHandler;
import org.rhq.coregui.client.components.upload.DynamicFormSubmitCompleteEvent;
import org.rhq.coregui.client.components.upload.PackageVersionFileUploadForm;
import org.rhq.coregui.client.gwt.GWTServiceLookup;

/**
 * A form to configure the CLI script alert notification.
 *
 * @author Lukas Krejci
 */
public class CliNotificationSenderForm extends AbstractNotificationSenderForm {

    private static final String PROP_PACKAGE_ID = "packageId";
    private static final String PROP_REPO_ID = "repoId";
    private static final String PROP_USER_ID = "userId";
    private static final String PROP_USER_NAME = "userName";
    private static final String PROP_USER_PASSWORD = "userPassword";
    private static final String PACKAGE_TYPE_NAME = "org.rhq.enterprise.server.plugins.packagetypeCli.SERVER_SIDE_CLI_SCRIPT";

    private static class Config {
        List<Repo> allRepos;
        Repo selectedRepo;

        List<PackageAndLatestVersionComposite> allPackages;
        PackageAndLatestVersionComposite selectedPackage;

        Subject selectedSubject;

        /*
         * This is a counter to keep track if all the async
         * loading of the above data has finished.
         * 
         * In principle, this should be AtomicInteger but GWT didn't like it.
         * 
         * The reason why it still works as a normal integer even though it 
         * is being updated from within the AsyncCallbacks is that the javascript
         * engines in the browsers are single threaded and therefore, even though
         * the data is loaded in the background, it is processed (and this variable
         * updated) only in that single thread.
         */
        int __handlerCounter = 3;

        public void setSelectedRepo(int repoId) {
            if (allRepos != null) {
                for (Repo r : allRepos) {
                    if (r.getId() == repoId) {
                        selectedRepo = r;
                        break;
                    }
                }
            }
        }

        public void setSelectedPackage(int packageId) {
            if (allPackages != null) {
                for (PackageAndLatestVersionComposite p : allPackages) {
                    if (p.getGeneralPackage().getId() == packageId) {
                        selectedPackage = p;
                        break;
                    }
                }
            }
        }
    }

    private static class PackageVersionFileUploadFormWithVersion extends PackageVersionFileUploadForm {

        public PackageVersionFileUploadFormWithVersion(int packageTypeId) {
            super(packageTypeId, null, null, null, null, true, false, false);
            setName("File");
        }

        protected List<FormItem> getOnDrawItems() {
            List<FormItem> items = super.getOnDrawItems();

            TextItem version = new TextItem("editableVersion",
                MSG.view_alert_definition_notification_cliScript_editor_newScriptVersion());
            version.setColSpan(getNumCols());
            version.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent event) {
                    getField("version").setValue(event.getValue());
                }
            });

            items.add(version);

            return items;
        }
    }

    private static abstract class ForwardingDynamicFormHandler implements DynamicFormHandler {
        private AsyncCallback<Void> callback;

        public AsyncCallback<Void> getCallback() {
            return callback;
        }

        public void setCallback(AsyncCallback<Void> callback) {
            this.callback = callback;
        }
    };

    private static abstract class ForwardingSubmitFailedHandler implements FormSubmitFailedHandler {
        private AsyncCallback<Void> callback;

        public AsyncCallback<Void> getCallback() {
            return callback;
        }

        public void setCallback(AsyncCallback<Void> callback) {
            this.callback = callback;
        }
    };

    private static String FAILED_LAST_TIME = "failed";

    private boolean formBuilt;
    private SelectItem repoSelector;
    private RadioGroupWithComponentsItem packageSelector;
    private SelectItem existingPackageSelector;
    private RadioGroupWithComponentsItem userSelector;
    private PackageVersionFileUploadFormWithVersion uploadForm;
    private PackageTypeAndVersionFormatComposite cliScriptPackageType;
    private TextItem anotherUserName;
    private TextItem anotherUserPassword;
    private ButtonItem verifyUserButton;
    private FormItemIcon successIcon;
    private FormItemIcon failureIcon;
    private ForwardingSubmitFailedHandler uploadFailureHandler;
    private ForwardingDynamicFormHandler uploadSuccessHandler;

    private Config config;

    public CliNotificationSenderForm(AlertNotification notif, String sender) {
        super(notif, sender);
    }

    @Override
    protected void onInit() {
        super.onInit();

        if (!formBuilt) {
            loadPackageType(new AsyncCallback<PackageTypeAndVersionFormatComposite>() {
                public void onFailure(Throwable t) {
                    CoreGUI.getErrorHandler().handleError(
                        MSG.view_alert_definition_notification_cliScript_editor_loadFailed(), t);
                }

                public void onSuccess(PackageTypeAndVersionFormatComposite result) {
                    cliScriptPackageType = result;

                    DynamicForm form = new DynamicForm();
                    form.setTitleOrientation(TitleOrientation.TOP);
                    form.setWidth(500);

                    SectionItem repoSection = new SectionItem("repoSection");
                    repoSection.setDefaultValue(MSG.view_alert_definition_notification_cliScript_editor_repository());
                    repoSection.setWidth("100%");
                    SectionItem packageSection = new SectionItem("packageSection");
                    packageSection.setDefaultValue(MSG.view_alert_definition_notification_cliScript_editor_script());
                    SectionItem userSection = new SectionItem("userSection");
                    userSection.setDefaultValue(MSG.view_alert_definition_notification_cliScript_editor_whichUser());

                    repoSelector = new SortedSelectItem("repoSelector",
                        MSG.view_alert_definition_notification_cliScript_editor_selectRepo());
                    repoSelector.setDefaultToFirstOption(true);
                    repoSelector.setWrapTitle(false);
                    repoSelector.setWidth(400);
                    repoSelector.setValueMap(MSG.common_msg_loading());
                    repoSelector.setDisabled(true);

                    LinkedHashMap<String, DynamicForm> packageSelectItems = new LinkedHashMap<String, DynamicForm>();
                    packageSelectItems.put(MSG.view_alert_definition_notification_cliScript_editor_existingScript(),
                        createExistingPackageForm());
                    packageSelectItems.put(MSG.view_alert_definition_notification_cliScript_editor_uploadNewScript(),
                        createUploadNewScriptForm());
                    packageSelector = new RadioGroupWithComponentsItem("packageSelector", "", packageSelectItems, form);
                    packageSelector.setWidth("100%");

                    LinkedHashMap<String, DynamicForm> userSelectItems = new LinkedHashMap<String, DynamicForm>();
                    userSelectItems.put(MSG.view_alert_definition_notification_cliScript_editor_thisUser(), null);
                    userSelectItems.put(MSG.view_alert_definition_notification_cliScript_editor_anotherUser(),
                        createAnotherUserForm());
                    userSelector = new RadioGroupWithComponentsItem("userSelector", "", userSelectItems, form);
                    userSelector.setWidth("100%");
                    userSelector.setShowTitle(false);

                    repoSection.setItemIds("repoSelector");
                    packageSection.setItemIds("packageSelector");
                    userSection.setItemIds("userSelector");

                    form.setFields(userSection, userSelector, repoSection, repoSelector, packageSection,
                        packageSelector);

                    addMember(form);

                    loadConfig(new AsyncCallback<Config>() {
                        public void onFailure(Throwable t) {
                            CoreGUI.getErrorHandler().handleError(
                                MSG.view_alert_definition_notification_cliScript_editor_loadFailed(), t);
                        }

                        public void onSuccess(Config config) {
                            CliNotificationSenderForm.this.config = config;
                            setupRepoSelector();
                            setupPackageSelector();
                            setupUserSelector();

                            formBuilt = true;

                            markForRedraw();
                        }
                    });
                }
            });
        }
    }

    private void setupUserSelector() {
        if (config.selectedSubject != null && !UserSessionManager.getSessionSubject().equals(config.selectedSubject)) {
            userSelector.setSelected(MSG.view_alert_definition_notification_cliScript_editor_anotherUser());
            DynamicForm anotherUserForm = (DynamicForm) userSelector.getSelectedComponent();
            anotherUserForm.getItem("userName").setValue(config.selectedSubject.getName());
        } else {
            userSelector.setSelected(MSG.view_alert_definition_notification_cliScript_editor_thisUser());
        }
        markForRedraw();
    }

    private void setupPackageSelector() {
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        for (PackageAndLatestVersionComposite p : config.allPackages) {
            String label = p.getGeneralPackage().getName() + " (" + p.getLatestPackageVersion().getVersion() + ")";
            map.put(String.valueOf(p.getGeneralPackage().getId()), label);
        }

        existingPackageSelector.setValueMap(map);
        if (config.selectedPackage != null) {
            existingPackageSelector.setValue(config.selectedPackage.getGeneralPackage().getId());
            packageSelector.setSelected(MSG.view_alert_definition_notification_cliScript_editor_existingScript());
        } else {
            packageSelector.setSelected(MSG.view_alert_definition_notification_cliScript_editor_uploadNewScript());
        }

        if (!formBuilt) {
            existingPackageSelector.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent event) {
                    int packageId = Integer.valueOf(event.getItem().getValue().toString());
                    config.setSelectedPackage(packageId);
                }
            });
        }

        existingPackageSelector.setDisabled(false);

        markForRedraw();
    }

    private void setupRepoSelector() {
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        for (Repo r : config.allRepos) {
            map.put(String.valueOf(r.getId()), r.getName());
        }

        repoSelector.setValueMap(map);
        if (config.selectedRepo != null) {
            repoSelector.setValue(config.selectedRepo.getId());
            packageSelector.setDisabled(false);
            uploadForm.setRepoId(config.selectedRepo.getId());
        } else {
            repoSelector.setValue("");
            packageSelector.setDisabled(true);
            uploadForm.setRepoId(null);
        }

        if (!formBuilt) {
            repoSelector.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent event) {
                    final Integer repoId = Integer.valueOf(event.getItem().getValue().toString());
                    config.setSelectedRepo(repoId);

                    PackageCriteria pc = new PackageCriteria();
                    pc.addFilterRepoId(repoId);
                    pc.addFilterPackageTypeId(cliScriptPackageType.getPackageType().getId());

                    packageSelector.setDisabled(false);
                    existingPackageSelector.setDisabled(true);
                    existingPackageSelector.setValueMap(MSG.common_msg_loading());

                    GWTServiceLookup.getContentService().findPackagesWithLatestVersion(pc,
                        new AsyncCallback<PageList<PackageAndLatestVersionComposite>>() {
                            public void onSuccess(PageList<PackageAndLatestVersionComposite> result) {
                                config.allPackages = result;
                                config.selectedPackage = result.isEmpty() ? null : result.get(0); //we're autoselecting the first item
                                setupPackageSelector();

                                uploadForm.setRepoId(repoId);
                            }

                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError(
                                    MSG.view_alert_definition_notification_cliScript_editor_loadFailed(), caught);
                            }
                        });
                }
            });
        }

        repoSelector.setDisabled(false);

        markForRedraw();
    }

    @Override
    public void validate(final AsyncCallback<Void> callback) {
        if (userSelector.getSelectedIndex() == 0) {
            getConfiguration().put(new PropertySimple(PROP_USER_ID, UserSessionManager.getSessionSubject().getId()));
            validatePackage(callback);
        } else {
            checkAuthenticationAndDo(new AsyncCallback<Void>() {
                public void onFailure(Throwable caught) {
                    callback.onFailure(caught);
                }

                public void onSuccess(Void result) {
                    getConfiguration().put(new PropertySimple(PROP_USER_ID, config.selectedSubject.getId()));
                    getConfiguration().put(new PropertySimple(PROP_USER_NAME, anotherUserName.getEnteredValue()));
                    getConfiguration().put(
                        new PropertySimple(PROP_USER_PASSWORD, anotherUserPassword.getEnteredValue()));

                    validatePackage(callback);
                }
            });
        }
    }

    private void validatePackage(final AsyncCallback<Void> callback) {
        if (config.selectedRepo == null) {
            setFailed(repoSelector);
            callback.onFailure(null);
            return;
        }
        setOk(repoSelector);
        if (!existingPackageSelector.isDisabled()) {
            if (config.selectedPackage == null) {
                setFailed(existingPackageSelector);
                callback.onFailure(null);
                return;
            }
            setOk(existingPackageSelector);
        }
        getConfiguration().put(new PropertySimple(PROP_REPO_ID, config.selectedRepo.getId()));

        if (packageSelector.getSelectedIndex() == 0) {
            getConfiguration().put(
                new PropertySimple(PROP_PACKAGE_ID, config.selectedPackage.getGeneralPackage().getId()));
            callback.onSuccess(null);
        } else {
            if (cliScriptPackageType.getVersionFormat() != null) {
                String versionRegex = cliScriptPackageType.getVersionFormat().getFullFormatRegex();
                if (versionRegex != null) {
                    Object version = uploadForm.getField("version").getValue();
                    if (version == null || !matches(version.toString(), versionRegex)) {
                        uploadForm.getItem("editableVersion").setIcons(failureIcon);
                        callback.onFailure(null);
                        return;
                    } else {
                        uploadForm.getItem("editableVersion").setIcons(successIcon);
                    }
                } else {
                    uploadForm.getItem("editableVersion").setIcons();
                }
            }
            uploadFailureHandler.setCallback(callback);
            uploadSuccessHandler.setCallback(new AsyncCallback<Void>() {
                public void onFailure(Throwable caught) {
                    callback.onFailure(null);
                };

                public void onSuccess(Void result) {
                    getConfiguration().put(new PropertySimple(PROP_PACKAGE_ID, uploadForm.getPackageId()));
                    callback.onSuccess(null);
                };
            });
            uploadForm.submitForm();
        }
    }

    private void loadConfig(final AsyncCallback<Config> handler) {
        final String repoId = getConfiguration().getSimpleValue(PROP_REPO_ID, null);
        final String packageId = getConfiguration().getSimpleValue(PROP_PACKAGE_ID, null);
        final String subjectId = getConfiguration().getSimpleValue(PROP_USER_ID, null);

        final Config config = new Config();

        RepoCriteria rc = new RepoCriteria();

        GWTServiceLookup.getRepoService().findReposByCriteria(rc, new AsyncCallback<PageList<Repo>>() {
            public void onSuccess(PageList<Repo> result) {
                config.allRepos = result;

                if (repoId != null && repoId.trim().length() > 0) {
                    final int rid = Integer.parseInt(repoId);
                    config.setSelectedRepo(rid);
                }

                if (--config.__handlerCounter == 0) {
                    handler.onSuccess(config);
                }
            }

            public void onFailure(Throwable caught) {
                handler.onFailure(caught);
            }
        });

        if (repoId != null && repoId.trim().length() > 0) {
            PackageCriteria pc = new PackageCriteria();
            pc.addFilterRepoId(Integer.parseInt(repoId));
            pc.addFilterPackageTypeId(cliScriptPackageType.getPackageType().getId());
            GWTServiceLookup.getContentService().findPackagesWithLatestVersion(pc,
                new AsyncCallback<PageList<PackageAndLatestVersionComposite>>() {
                    public void onSuccess(PageList<PackageAndLatestVersionComposite> result) {
                        config.allPackages = result;

                        if (packageId != null && packageId.trim().length() > 0) {
                            int pid = Integer.parseInt(packageId);
                            config.setSelectedPackage(pid);
                        }

                        if (--config.__handlerCounter == 0) {
                            handler.onSuccess(config);
                        }
                    }

                    public void onFailure(Throwable caught) {
                        handler.onFailure(caught);
                    }
                });
        } else {
            config.allPackages = Collections.emptyList();
            --config.__handlerCounter;
        }

        if (subjectId != null && subjectId.trim().length() > 0) {
            int sid = Integer.parseInt(subjectId);
            SubjectCriteria c = new SubjectCriteria();
            c.addFilterId(sid);

            GWTServiceLookup.getSubjectService().findSubjectsByCriteria(c, new AsyncCallback<PageList<Subject>>() {
                public void onSuccess(PageList<Subject> result) {
                    if (result.size() > 0) {
                        config.selectedSubject = result.get(0);
                    }

                    if (--config.__handlerCounter == 0) {
                        handler.onSuccess(config);
                    }
                }

                public void onFailure(Throwable caught) {
                    handler.onFailure(caught);
                }
            });
        } else {
            --config.__handlerCounter;
        }
    }

    private DynamicForm createAnotherUserForm() {
        DynamicForm form = new DynamicForm();
        form.setTitleOrientation(TitleOrientation.TOP);
        anotherUserName = new TextItem("userName", MSG.dataSource_users_field_name());
        anotherUserPassword = new PasswordItem("password", MSG.common_title_password());
        anotherUserPassword.setAttribute("autocomplete", "off");
        verifyUserButton = new ButtonItem("verify",
            MSG.view_alert_definition_notification_cliScript_editor_verifyAuthentication());

        successIcon = new FormItemIcon();
        successIcon.setSrc(ImageManager.getAvailabilityIcon(Boolean.TRUE));
        successIcon.setWidth(16);
        successIcon.setHeight(16);

        failureIcon = new FormItemIcon();
        failureIcon.setSrc(ImageManager.getAvailabilityIcon(Boolean.FALSE));
        failureIcon.setWidth(16);
        failureIcon.setHeight(16);

        form.setFields(anotherUserName, anotherUserPassword, verifyUserButton);

        verifyUserButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                //just checking the auth is ok, no other action is needed.
                checkAuthenticationAndDo(new AsyncCallback<Void>() {
                    public void onFailure(Throwable caught) {
                    }

                    public void onSuccess(Void result) {
                    }
                });
            }
        });
        return form;
    }

    private DynamicForm createExistingPackageForm() {
        DynamicForm form = new DynamicForm();
        form.setTitleOrientation(TitleOrientation.TOP);
        existingPackageSelector = new SortedSelectItem("existingPackageSelector", "");
        existingPackageSelector.setDefaultToFirstOption(true);
        existingPackageSelector.setWrapTitle(false);
        existingPackageSelector.setRedrawOnChange(true);
        existingPackageSelector.setWidth(300);
        existingPackageSelector.setValueMap(MSG.common_msg_loading());
        existingPackageSelector.setDisabled(true);

        form.setFields(existingPackageSelector);
        return form;
    }

    private DynamicForm createUploadNewScriptForm() {
        uploadForm = new PackageVersionFileUploadFormWithVersion(cliScriptPackageType.getPackageType()
            .getId());
        uploadForm.setTitleOrientation(TitleOrientation.TOP);
        uploadForm.setPackageTypeId(cliScriptPackageType.getPackageType().getId());

        uploadFailureHandler = (new ForwardingSubmitFailedHandler() {
            public void onFormSubmitFailed(FormSubmitFailedEvent event) {
                if (getCallback() != null) {
                    getCallback().onFailure(null);
                }
            }
        });

        uploadSuccessHandler = new ForwardingDynamicFormHandler() {
            public void onSubmitComplete(DynamicFormSubmitCompleteEvent event) {
                if (uploadForm.getPackageId() == 0 || uploadForm.getPackageVersionId() == 0) {
                    if (getCallback() != null) {
                        getCallback().onFailure(null);
                    }
                    return;
                }

                if (getCallback() != null) {
                    getCallback().onSuccess(null);
                }
            }
        };
        uploadForm.addFormHandler(uploadSuccessHandler);

        uploadForm.addItemChangeHandler(new ItemChangeHandler() {
            public void onItemChange(ItemChangeEvent event) {
                if (event.getItem() instanceof UploadItem) {
                    if (event.getNewValue() == null) {
                        uploadForm.getField("editableVersion").setValue("");
                        uploadForm.getField("version").setValue("");
                        return;
                    }

                    String fileName = event.getNewValue().toString();

                    if (config.allPackages != null) {
                        for (PackageAndLatestVersionComposite plv : config.allPackages) {
                            if (plv.getGeneralPackage().getName().equals(fileName)) {
                                try {
                                    String version = plv.getLatestPackageVersion().getVersion();

                                    PackageVersionFormatDescription format = cliScriptPackageType.getVersionFormat();
                                    if (format == null) {
                                        return;
                                    }

                                    if (format.getOsgiVersionExtractionRegex() == null) {
                                        return;
                                    }

                                    String regex = format.getOsgiVersionExtractionRegex();
                                    int group = format.getOsgiVersionGroupIndex();
                                    String oldOsgiVersion = getOsgiVersion(version, regex, group);

                                    OSGiVersion v = new OSGiVersion(oldOsgiVersion);
                                    if (v.getMicroIfDefined() != null) {
                                        v.setMicro(v.getMicro() + 1);
                                    } else if (v.getMinorIfDefined() != null) {
                                        v.setMinor(v.getMinor() + 1);
                                    } else {
                                        v.setMajor(v.getMajor() + 1);
                                    }

                                    String newVersion = version.replace(oldOsgiVersion, v.toString());

                                    uploadForm.getField("editableVersion").setValue(newVersion);
                                    uploadForm.getField("version").setValue(newVersion);
                                    return;
                                } catch (Exception e) {
                                    //ok, can't suggest anything
                                    return;
                                }
                            }
                        }

                        //no existing package exists with the same name as the file being uploaded

                        String defaultVersion = "";
                        if (cliScriptPackageType.getVersionFormat() != null
                            && cliScriptPackageType.getVersionFormat().getOsgiVersionExtractionRegex() != null) {
                            //check if the package format understands plain OSGi versioning
                            if (matches("1.0", cliScriptPackageType.getVersionFormat().getFullFormatRegex())) {
                                defaultVersion = "1.0";
                            }
                        }
                        uploadForm.getField("editableVersion").setValue(defaultVersion);
                        uploadForm.getField("version").setValue(defaultVersion);
                    }
                }
            }
        });

        return uploadForm;
    }

    private void loadPackageType(AsyncCallback<PackageTypeAndVersionFormatComposite> handler) {
        GWTServiceLookup.getContentService().findPackageType(null, PACKAGE_TYPE_NAME, handler);
    }

    private void setFailed(FormItem item) {
        item.setIcons(failureIcon);
        item.setAttribute(FAILED_LAST_TIME, true);
    }

    private void setOk(FormItem item) {
        Boolean lastTimeFail = item.getAttributeAsBoolean(FAILED_LAST_TIME);
        if (lastTimeFail != null && lastTimeFail) {
            item.setIcons(successIcon);
            item.setAttribute(FAILED_LAST_TIME, false);
        }
    }

    private void checkAuthenticationAndDo(final AsyncCallback<Void> action) {
        String username = anotherUserName.getEnteredValue();
        String password = anotherUserPassword.getEnteredValue();

        GWTServiceLookup.getSubjectService().checkAuthentication(username, password, new AsyncCallback<Subject>() {
            public void onFailure(Throwable caught) {
                config.selectedSubject = null;
                verifyUserButton.setIcons(failureIcon);
                markForRedraw();
                action.onFailure(caught);
            }

            public void onSuccess(Subject result) {
                config.selectedSubject = result;
                verifyUserButton.setIcons(result == null ? failureIcon : successIcon);
                markForRedraw();
                if (result == null) {
                    action.onFailure(null);
                } else {
                    action.onSuccess(null);
                }
            }
        });
    }

    private static native String getOsgiVersion(String version, String osgiRegex, int groupIdx) /*-{
                                                                                                var re = new RegExp(osgiRegex);
                                                                                                var groupRef = '$' + groupIdx;
                                                                                                version = version.replace(re, groupRef);
                                                                                                
                                                                                                return version;
                                                                                                }-*/;

    private static native boolean matches(String string, String regex) /*-{
                                                                       var re = new RegExp(regex);
                                                                       return re.test(string);
                                                                       }-*/;
}
