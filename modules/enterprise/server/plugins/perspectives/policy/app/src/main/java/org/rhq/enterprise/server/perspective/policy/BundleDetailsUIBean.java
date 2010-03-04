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
package org.rhq.enterprise.server.perspective.policy;

import org.ajax4jsf.model.KeepAlive;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.international.StatusMessage;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.criteria.BundleCriteria;
import org.rhq.core.domain.criteria.BundleVersionCriteria;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.table.bean.AbstractPagedDataUIBean;
import org.rhq.core.gui.table.model.PagedListDataModel;
import org.rhq.enterprise.client.RemoteClient;
import org.rhq.enterprise.server.bundle.BundleManagerRemote;
import org.rhq.enterprise.server.perspective.AbstractPerspectivePagedDataUIBean;
import org.rhq.enterprise.server.perspective.PerspectiveClientUIBean;

import java.util.List;

/**
 * Provides details about a particular provisioning {@link Bundle bundle}, including CRUD operations on
 * the {@link org.rhq.core.domain.bundle.BundleVersion version}s defined for that bundle. The backing bean for
 * bundleDetails.xhtml.
 *
 * @author Ian Springer
 */
@Name("BundleDetailsUIBean")
@Scope(ScopeType.EVENT)
@KeepAlive
public class BundleDetailsUIBean extends AbstractPerspectivePagedDataUIBean {
    private List<BundleVersion> selectedBundleVersions;

    @RequestParameter
    private int bundleId;

    private Bundle bundle;

    public Bundle getBundle() {
        if (this.bundle == null) {
            this.bundle = loadBundle();
        }
        return this.bundle;
    }

    @Override
    public PagedListDataModel createDataModel() {
        return new DataModel(this);
    }

    public List<BundleVersion> getSelectedBundleVersions() {
        return this.selectedBundleVersions;
    }

    public void setSelectedBundleVersions(List<BundleVersion> selectedBundleVersions) {
        this.selectedBundleVersions = selectedBundleVersions;
    }

    public void deleteSelectedBundles() throws Exception {
        RemoteClient remoteClient;
        Subject subject;
        try {
            remoteClient = this.perspectiveClient.getRemoteClient();
            subject = this.perspectiveClient.getSubject();
        } catch (Exception e) {
            this.facesMessages.add(StatusMessage.Severity.FATAL, "Failed to connect to RHQ Server - cause: " + e);
            return;
        }

        // ***NOTE***: The javassist.NotFoundException stack traces that are logged by this call can be ignored.
        BundleManagerRemote bundleManager = remoteClient.getBundleManagerRemote();

        int[] selectedBundleVersionIds = new int[this.selectedBundleVersions.size()];
        for (int i = 0, selectedBundlesSize = this.selectedBundleVersions.size(); i < selectedBundlesSize; i++) {
            BundleVersion selectedBundleVersion = this.selectedBundleVersions.get(i);
            selectedBundleVersionIds[i] = selectedBundleVersion.getId();
        }

        bundleManager.deleteBundleVersions(subject, selectedBundleVersionIds);

        // Add message to tell the user the uninventory was a success.
        String pluralizer = (this.selectedBundleVersions.size() == 1) ? "" : "s";
        this.facesMessages.add("Deleted " + this.selectedBundleVersions.size() + " bundle version" + pluralizer + ".");

        // Reset the data model, so the current page will get refreshed to reflect the Resources we just uninventoried.
        // This is essential, since we are CONVERSATION-scoped and will live on beyond this request.
        setDataModel(null);
    }

    private Bundle loadBundle() {
        BundleManagerRemote bundleManager;
        Subject subject;
        try {
            bundleManager = this.perspectiveClient.getRemoteClient().getBundleManagerRemote();
            subject = this.perspectiveClient.getSubject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        BundleCriteria bundleCriteria = new BundleCriteria();
        bundleCriteria.addFilterId(this.bundleId);
        PageList<Bundle> bundles = bundleManager.findBundlesByCriteria(subject, bundleCriteria);
        if (bundles.isEmpty()) {
            throw new IllegalStateException("Bundle with id " + this.bundleId + " not found.");
        }
        return bundles.get(0);
    }

    private class DataModel extends PagedListDataModel<BundleVersion> {
        private DataModel(AbstractPagedDataUIBean pagedDataBean) {
            super(pagedDataBean);
        }

        @Override
        public PageList<BundleVersion> fetchPage(PageControl pageControl) {
            PerspectiveClientUIBean perspectiveClient = BundleDetailsUIBean.this.perspectiveClient;
            BundleManagerRemote bundleManager;
            Subject subject;
            try {
                bundleManager = perspectiveClient.getRemoteClient().getBundleManagerRemote();
                subject = BundleDetailsUIBean.this.perspectiveClient.getSubject();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            BundleVersionCriteria bundleVersionCriteria = new BundleVersionCriteria();
            bundleVersionCriteria.setPageControl(pageControl);
            String bundleName = BundleDetailsUIBean.this.getBundle().getName();            
            bundleVersionCriteria.addFilterBundleName(bundleName);
            // TODO: Implement user-specified filters.
            PageList<BundleVersion> bundleVersions = bundleManager.findBundleVersionsByCriteria(subject,
                    bundleVersionCriteria);
            return bundleVersions;
        }
    }
}