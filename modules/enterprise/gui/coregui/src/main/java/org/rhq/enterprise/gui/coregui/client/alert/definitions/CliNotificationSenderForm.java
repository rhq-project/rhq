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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.widgets.form.DynamicForm;

import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.criteria.RepoCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.components.selector.AbstractSelector;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * A form to configure the CLI script alert notification.
 *
 * @author Lukas Krejci
 */
public class CliNotificationSenderForm extends AbstractNotificationSenderForm {

    private static final String PROP_PACKAGE_ID = "packageId";
    private static final String PROP_REPO_ID = "repoId";
    private static final String PROP_USER_ID = "userId";
    
    private static class Config {
        Repo selectedRepo;
        Package selectedPackage;
        Subject selectedSubject;
    }
    
    public CliNotificationSenderForm(String locatorId, AlertNotification notif, String sender) {
        super(locatorId, notif, sender);
    }

    @Override
    protected void onInit() {
        super.onInit();
        
        String repoId = getConfiguration().getSimpleValue(PROP_REPO_ID, null);
        String packageId = getConfiguration().getSimpleValue(PROP_PACKAGE_ID, null);
        String userId = getConfiguration().getSimpleValue(PROP_USER_ID, null);

        final Config config = new Config();
        
        if (repoId != null && repoId.trim().length() > 0) {
            int rid = Integer.parseInt(repoId);
            RepoCriteria c = new RepoCriteria();
            c.addFilterId(rid);
            GWTServiceLookup.getRepoService().findRepoByCriteria(c, new AsyncCallback<PageList<Repo>>() {
                public void onSuccess(PageList<Repo> result) {
                    if (result.size() > 0) {
                        config.selectedRepo = result.get(0);
                    }
                }

                public void onFailure(Throwable caught) {
                }
            });
        }
        
        RepoSelector repoSelector = new RepoSelector(extendLocatorId("repoSelector"), config.selectedRepo);
        PackageSelector packageSelector = new PackageSelector(extendLocatorId("packageSelector"), config.selectedRepo, config.selectedPackage);
        //TODO UI to select the user.
    }
    
    public boolean validate() {
        // TODO implement
        return false;
    }

    private class RepoSelector extends AbstractSelector<Repo> {

        private Repo repo;
        /**
         * @param locatorId
         */
        public RepoSelector(String locatorId, Repo preselectedRepo) {
            super(locatorId);
            this.repo = preselectedRepo;
        }

        protected DynamicForm getAvailableFilterForm() {
            return null;
        }

        protected RPCDataSource<Repo> getDataSource() {
            return null;
        }

        protected Criteria getLatestCriteria(DynamicForm availableFilterForm) {
            return null;
        }

        protected String getItemTitle() {
            return null;
        }
    }
    
    private class PackageSelector extends AbstractSelector<Package> {

        private Repo repoFilter;
        
        /**
         * @param locatorId
         */
        public PackageSelector(String locatorId, Repo preselectedRepo, Package preselectedPackage) {
            super(locatorId);
            this.repoFilter = preselectedRepo;
            //TODO implement
        }

        protected DynamicForm getAvailableFilterForm() {
            return null;
        }

        protected RPCDataSource<Package> getDataSource() {
            return null;
        }

        protected Criteria getLatestCriteria(DynamicForm availableFilterForm) {
            return null;
        }

        protected String getItemTitle() {
            // TODO Auto-generated method stub
            return null;
        }               
    }
}
