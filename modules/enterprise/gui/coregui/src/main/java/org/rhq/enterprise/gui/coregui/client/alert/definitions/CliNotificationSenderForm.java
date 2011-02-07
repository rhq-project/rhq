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

import java.util.LinkedHashMap;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.SelectItem;

import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.criteria.PackageCriteria;
import org.rhq.core.domain.criteria.RepoCriteria;
import org.rhq.core.domain.criteria.SubjectCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.components.form.RadioGroupWithComponentsItem;
import org.rhq.enterprise.gui.coregui.client.components.selector.AbstractSelector;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * A form to configure the CLI script alert notification.
 *
 * @author Lukas Krejci
 */
public class CliNotificationSenderForm extends AbstractNotificationSenderForm {

    private static final String PROP_PACKAGE_ID = "packageId";
    private static final String PROP_REPO_ID = "repoId";
    private static final String PROP_USER_ID = "userId";

    boolean formBuilt;
    
    private static class Config {
        List<Repo> allRepos;
        Repo selectedRepo;
        
        List<Package> allPackages;
        Package selectedPackage;
        
        Subject selectedSubject;
    }
    
    public CliNotificationSenderForm(String locatorId, AlertNotification notif, String sender) {
        super(locatorId, notif, sender);
    }

    @Override
    protected void onInit() {
        super.onInit();
        
        if (!formBuilt) {     
            LocatableDynamicForm form = new LocatableDynamicForm(extendLocatorId("form"));
            
            SelectItem repoSelector = new SelectItem("repoSelector", "Repository:"); //TODO i18n
            repoSelector.setDefaultToFirstOption(false);
            repoSelector.setWrapTitle(false);
            repoSelector.setRedrawOnChange(true);
            repoSelector.setWidth("*");            
            repoSelector.setValueMap(MSG.common_msg_loading());
            repoSelector.setDisabled(true);

            SelectItem packageSelector = new SelectItem("packageSelector", "Script:"); //TODO i18n
            packageSelector.setDefaultToFirstOption(false);
            packageSelector.setWrapTitle(false);
            packageSelector.setRedrawOnChange(true);
            packageSelector.setWidth("*");            
            packageSelector.setValueMap(MSG.common_msg_loading());
            packageSelector.setDisabled(true);

            DynamicForm anotherUserForm = createAnotherUserForm();
            
            LinkedHashMap<String, DynamicForm> userSelectItems = new LinkedHashMap<String, DynamicForm>();
            
            userSelectItems.put(MSG.view_alert_definition_notification_cliScript_editor_thisUser(), null);
            userSelectItems.put(MSG.view_alert_definition_notification_cliScript_editor_anotherUser(), anotherUserForm);
            
            RadioGroupWithComponentsItem userSelector = new RadioGroupWithComponentsItem(
                extendLocatorId("userSelector"), 
                MSG.view_alert_definition_notification_cliScript_editor_whichUser(), userSelectItems, form);
                        
            form.setFields(repoSelector, packageSelector, userSelector);
            addMember(form);
            
            Config config = loadConfig();
            
            if (config.selectedRepo != null) {
                
            }
            
            formBuilt = true;
        }
    }
    
    public boolean validate() {
        // TODO implement
        return false;
    }
    
    private Config loadConfig() {
        String repoId = getConfiguration().getSimpleValue(PROP_REPO_ID, null);
        String packageId = getConfiguration().getSimpleValue(PROP_PACKAGE_ID, null);
        String subjectId = getConfiguration().getSimpleValue(PROP_USER_ID, null);

        final Config config = new Config();

        if (repoId != null && repoId.trim().length() > 0) {
            int rid = Integer.parseInt(repoId);
            RepoCriteria c = new RepoCriteria();
            c.addFilterId(rid);
            GWTServiceLookup.getRepoService().findReposByCriteria(c, new AsyncCallback<PageList<Repo>>() {
                public void onSuccess(PageList<Repo> result) {
                    if (result.size() > 0) {
                        config.selectedRepo = result.get(0);
                    }
                }

                public void onFailure(Throwable caught) {
                    //TODO
                }
            });
        }
        
        if (packageId != null && packageId.trim().length() > 0) {
            int pid = Integer.parseInt(packageId);
            PackageCriteria c = new PackageCriteria();
            c.addFilterId(pid);
            
            GWTServiceLookup.getContentService().findPackagesByCriteria(c, new AsyncCallback<PageList<Package>>() {
                public void onSuccess(PageList<Package> result) {
                    if (result.size() > 0) {
                        config.selectedPackage = result.get(0);
                    }
                }
                
                public void onFailure(Throwable caught) {
                    //TODO
                }
            });
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
               }
               
               public void onFailure(Throwable caught) {
                   //TODO
               }
           });
        }
        
        return config;
    }
    
    DynamicForm createAnotherUserForm() {
        LocatableDynamicForm form = new LocatableDynamicForm(extendLocatorId("anotherUserForm"));
        
        return form;
    }
}       
