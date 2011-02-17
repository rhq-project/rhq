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

package org.rhq.enterprise.gui.coregui.client.alert.definitions;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.PasswordItem;
import com.smartgwt.client.widgets.form.fields.SectionItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;

import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.criteria.PackageCriteria;
import org.rhq.core.domain.criteria.RepoCriteria;
import org.rhq.core.domain.criteria.SubjectCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.components.form.RadioGroupWithComponentsItem;
import org.rhq.enterprise.gui.coregui.client.components.upload.FileUploadForm;
import org.rhq.enterprise.gui.coregui.client.components.upload.PackageVersionFileUploadForm;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;

/**
 * A form to configure the CLI script alert notification.
 *
 * @author Lukas Krejci
 */
public class CliNotificationSenderForm extends AbstractNotificationSenderForm {

    private static final String PROP_PACKAGE_ID = "packageId";
    private static final String PROP_REPO_ID = "repoId";
    private static final String PROP_USER_ID = "userId";
    private static final String PACKAGE_TYPE_NAME = "__SERVER_SIDE_CLI_SCRIPT";

    private static class Config {
        List<Repo> allRepos;
        Repo selectedRepo;
        
        List<Package> allPackages;
        Package selectedPackage;
        
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
                for(Repo r : allRepos) { 
                    if (r.getId() == repoId) {
                        selectedRepo = r;
                        break;
                    }
                }
            }
        }

        public void setSelectedPackage(int packageId) {
            if (allPackages != null) {
                for(Package p : allPackages) { 
                    if (p.getId() == packageId) {
                        selectedPackage = p;
                        break;
                    }
                }
            }
        }
    }
    
    private static class PackageVersionFileUploadFormWithVersion extends PackageVersionFileUploadForm {
        
        /**
         * @param locatorId
         * @param packageTypeId
         */
        public PackageVersionFileUploadFormWithVersion(String locatorId, int packageTypeId) {
            super(locatorId, packageTypeId, null, null, null, null, true, false, false);
            setName("File");
        }

        protected List<FormItem> getOnDrawItems() {
            List<FormItem> items = super.getOnDrawItems();
            
            TextItem version = new TextItem("editableVersion", "Version"); //TODO i18n
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
    
    
    private boolean formBuilt;
    
    private SelectItem repoSelector;
    private RadioGroupWithComponentsItem packageSelector;
    private SelectItem existingPackageSelector;
    private RadioGroupWithComponentsItem userSelector;
    private PackageVersionFileUploadFormWithVersion uploadForm;
    private PackageType cliScriptPackageType;
    private Config config;
    
    public CliNotificationSenderForm(String locatorId, AlertNotification notif, String sender) {
        super(locatorId, notif, sender);
    }

    @Override
    protected void onInit() {
        super.onInit();
        
        if (!formBuilt) {                 
            loadPackageType(new AsyncCallback<PackageType>() {
                public void onFailure(Throwable t) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_alert_definition_notification_cliScript_editor_loadFailed(),
                        t);
                }
                
                public void onSuccess(PackageType result) {
                    cliScriptPackageType = result;

                    LocatableDynamicForm form = new LocatableDynamicForm(extendLocatorId("form"));
                    form.setTitleOrientation(TitleOrientation.TOP);
                    form.setWidth(500);
                    
                    SectionItem repoSection = new SectionItem("repoSection");
                    repoSection.setDefaultValue(MSG.view_alert_definition_notification_cliScript_editor_repository());
                    repoSection.setWidth("100%");
                    SectionItem packageSection = new SectionItem("packageSection");
                    packageSection.setDefaultValue(MSG.view_alert_definition_notification_cliScript_editor_script());
                    SectionItem userSection = new SectionItem("userSection");
                    userSection.setDefaultValue(MSG.view_alert_definition_notification_cliScript_editor_whichUser());
                    
                    repoSelector = new SelectItem(extendLocatorId("repoSelector"), "Select the repository to look for the script in"); //TODO i18n
                    repoSelector.setDefaultToFirstOption(true);
                    repoSelector.setWrapTitle(false);
                    repoSelector.setWidth(400);            
                    repoSelector.setValueMap(MSG.common_msg_loading());
                    repoSelector.setDisabled(true);

                    LinkedHashMap<String, DynamicForm> packageSelectItems = new LinkedHashMap<String, DynamicForm>();
                    packageSelectItems.put(MSG.view_alert_definition_notification_cliScript_editor_existingScript(), createExistingPackageForm());
                    packageSelectItems.put(MSG.view_alert_definition_notification_cliScript_editor_uploadNewScript(), createUploadNewScriptForm());            
                    packageSelector = new RadioGroupWithComponentsItem(extendLocatorId("packageSelector"), 
                        "", packageSelectItems, form);
                    packageSelector.setWidth("100%");
                                
                    LinkedHashMap<String, DynamicForm> userSelectItems = new LinkedHashMap<String, DynamicForm>();            
                    userSelectItems.put(MSG.view_alert_definition_notification_cliScript_editor_thisUser(), 
                        null);
                    userSelectItems.put(MSG.view_alert_definition_notification_cliScript_editor_anotherUser(), 
                        createAnotherUserForm());            
                    userSelector = new RadioGroupWithComponentsItem(
                        extendLocatorId("userSelector"), 
                        "", userSelectItems, form);
                    userSelector.setWidth("100%");
                    
                    repoSection.setItemIds(extendLocatorId("repoSelector"));
                    packageSection.setItemIds(extendLocatorId("packageSelector"));
                    userSection.setItemIds(extendLocatorId("userSelector"));
                    
                    form.setFields(userSection, userSelector, repoSection,  repoSelector, packageSection, packageSelector);
                    
                    addMember(form);
                    
                    loadConfig(new AsyncCallback<Config>() {
                        public void onFailure(Throwable t) {
                            CoreGUI.getErrorHandler().handleError(MSG.view_alert_definition_notification_cliScript_editor_loadFailed(),
                                t);
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
            LocatableDynamicForm anotherUserForm = (LocatableDynamicForm) userSelector.getSelectedComponent();
            anotherUserForm.getItem("userName").setValue(config.selectedSubject.getName());
        } else {
            userSelector.setSelected(MSG.view_alert_definition_notification_cliScript_editor_thisUser());
        }
        markForRedraw();
    }

    private void setupPackageSelector() {
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        for(Package p : config.allPackages) {
            map.put(String.valueOf(p.getId()), p.getName());
        }
        
        existingPackageSelector.setValueMap(map);
        if (config.selectedPackage != null) {
            existingPackageSelector.setValue(config.selectedPackage.getId());
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
        for(Repo r : config.allRepos) {
            map.put(String.valueOf(r.getId()), r.getName());
        }
        
        repoSelector.setValueMap(map);
        if (config.selectedRepo != null) {
            repoSelector.setValue(config.selectedRepo.getId());
            packageSelector.setDisabled(false);
        } else {
            repoSelector.setValue("");
            packageSelector.setDisabled(true);
        }
        
        if (!formBuilt) {
            repoSelector.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent event) {
                    final Integer repoId = Integer.valueOf(event.getItem().getValue().toString());
                    config.setSelectedRepo(repoId);
                    
                    PackageCriteria pc = new PackageCriteria();
                    pc.addFilterRepoId(repoId);
                    pc.addFilterPackageTypeId(cliScriptPackageType.getId());
                    
                    packageSelector.setDisabled(false);
                    existingPackageSelector.setDisabled(true);
                    existingPackageSelector.setValueMap(MSG.common_msg_loading());
                                    
                    GWTServiceLookup.getContentService().findPackagesByCriteria(pc, new AsyncCallback<PageList<Package>>() {
                        public void onSuccess(PageList<Package> result) {
                            config.allPackages = result;
                            config.selectedPackage = result.isEmpty() ? null : result.get(0); //we're autoselecting the first item
                            setupPackageSelector();
                            
                            uploadForm.setRepoId(repoId);
                        }
                        
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError(MSG.view_alert_definition_notification_cliScript_editor_loadFailed(),
                                caught);
                        }
                    });                
                }
            });
        }
        
        repoSelector.setDisabled(false);
        
        markForRedraw();
    }

    public boolean validate() {
        // TODO add validation messages to the individual fields
        
        if (userSelector.getSelectedIndex() != 0 && config.selectedSubject == null) {
            return false;
        }
        
        if (config.selectedRepo == null) {
            return false;
        }
        
        if (packageSelector.getSelectedIndex() == 0 && config.selectedPackage == null) {
            return false;
        }
        
        if (userSelector.getSelectedIndex() == 0) {
            getConfiguration().put(new PropertySimple(PROP_USER_ID, UserSessionManager.getSessionSubject().getId()));
        } else {
            getConfiguration().put(new PropertySimple(PROP_USER_ID, config.selectedSubject.getId()));
        }
        
        getConfiguration().put(new PropertySimple(PROP_REPO_ID, config.selectedRepo.getId()));
        
        if (packageSelector.getSelectedIndex() == 0) {
            getConfiguration().put(new PropertySimple(PROP_PACKAGE_ID, config.selectedPackage.getId()));
        } else {
            //TODO do upload here and wait for the result before returning...
        }
        
        return true;
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
            pc.addFilterPackageTypeId(cliScriptPackageType.getId());
            
            GWTServiceLookup.getContentService().findPackagesByCriteria(pc, new AsyncCallback<PageList<Package>>() {
                public void onSuccess(PageList<Package> result) {
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
        LocatableDynamicForm form = new LocatableDynamicForm(extendLocatorId("anotherUserForm"));
        form.setTitleOrientation(TitleOrientation.TOP);
        TextItem userNameItem = new TextItem("userName", MSG.dataSource_users_field_name());
        PasswordItem passwordItem = new PasswordItem("password", MSG.dataSource_users_field_password());
        ButtonItem verifyItem = new ButtonItem("verify", MSG.view_alert_definition_notification_cliScript_editor_verifyAuthentication());
        form.setFields(userNameItem, passwordItem, verifyItem);
        
        //TODO add verification functionality
        return form;
    }
    
    private DynamicForm createExistingPackageForm() {
        LocatableDynamicForm form = new LocatableDynamicForm(extendLocatorId("existingPackageForm"));
        form.setTitleOrientation(TitleOrientation.TOP);
        existingPackageSelector = new SelectItem(extendLocatorId("existingPackageSelector"), "");
        existingPackageSelector.setDefaultToFirstOption(true);
        existingPackageSelector.setWrapTitle(false);
        existingPackageSelector.setRedrawOnChange(true);
        existingPackageSelector.setWidth("*");            
        existingPackageSelector.setValueMap(MSG.common_msg_loading());
        existingPackageSelector.setDisabled(true);
        
        form.setFields(existingPackageSelector);
        return form;
    }
    
    private DynamicForm createUploadNewScriptForm() {
        uploadForm = new PackageVersionFileUploadFormWithVersion(extendLocatorId("uploadForm"), cliScriptPackageType.getId());         
        uploadForm.setTitleOrientation(TitleOrientation.TOP);

        return uploadForm;
    }
    
    private void loadPackageType(AsyncCallback<PackageType> handler) {
        GWTServiceLookup.getContentService().findPackageType(null, PACKAGE_TYPE_NAME, handler);
    }
}       
