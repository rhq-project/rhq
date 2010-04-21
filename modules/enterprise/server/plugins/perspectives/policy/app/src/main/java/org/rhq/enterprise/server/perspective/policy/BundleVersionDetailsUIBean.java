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
 * Provides details about a particular {@link org.rhq.core.domain.bundle.BundleVersion version} of a provisioning
 * bundle, including CRUD operations on the {@link org.rhq.core.domain.bundle.BundleDeployment deployment}s defined
 * for that bundle version. The backing bean for bundleVersionDetails.xhtml.
 *
 * @author Ian Springer
 */
@Name("BundleVersionDetailsUIBean")
@Scope(ScopeType.EVENT)
@KeepAlive
public class BundleVersionDetailsUIBean extends AbstractPerspectivePagedDataUIBean {
    private List<BundleDeployment> selectedBundleDeployments;

    @RequestParameter
    private int bundleVersionId;

    private BundleVersion bundleVersion;

    public BundleVersion getBundleVersion() {
        if (this.bundleVersion == null) {
            this.bundleVersion = loadBundleVersion();
        }
        return this.bundleVersion;
    }

    public Bundle getBundle() {
        return getBundleVersion().getBundle();
    }

    @Override
    public PagedListDataModel createDataModel() {
        return new DataModel(this);
    }

    public List<BundleDeployment> getselectedBundleDeployments() {
        return this.selectedBundleDeployments;
    }

    public void setselectedBundleDeployments(List<BundleDeployment> selectedBundleDeployments) {
        this.selectedBundleDeployments = selectedBundleDeployments;
    }

    public void deleteselectedBundleDeployments() throws Exception {
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

        int[] selectedBundleDeploymentIds = new int[this.selectedBundleDeployments.size()];
        for (int i = 0, selectedBundlesSize = this.selectedBundleDeployments.size(); i < selectedBundlesSize; i++) {
            BundleDeployment selectedBundleDeployment = this.selectedBundleDeployments.get(i);
            selectedBundleDeploymentIds[i] = selectedBundleDeployment.getId();
        }

        // Add message to tell the user the uninventory was a success.
        String pluralizer = (this.selectedBundleDeployments.size() == 1) ? "" : "s";
        this.facesMessages.add("Deleted " + this.selectedBundleDeployments.size() + " bundle deployment" + pluralizer + ".");

        // Reset the data model, so the current page will get refreshed to reflect the Resources we just uninventoried.
        // This is essential, since we are CONVERSATION-scoped and will live on beyond this request.
        setDataModel(null);
    }

    private BundleVersion loadBundleVersion() {
        BundleManagerRemote bundleManager;
        Subject subject;
        try {
            bundleManager = this.perspectiveClient.getRemoteClient().getBundleManagerRemote();
            subject = this.perspectiveClient.getSubject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        BundleVersionCriteria bundleVersionCriteria = new BundleVersionCriteria();
        bundleVersionCriteria.addFilterId(this.bundleVersionId);
        PageList<BundleVersion> bundleVersions = bundleManager.findBundleVersionsByCriteria(subject, bundleVersionCriteria);
        if (bundleVersions.isEmpty()) {
            throw new IllegalStateException("Bundle version with id " + this.bundleVersionId + " not found.");
        }
        return bundleVersions.get(0);
    }

    private class DataModel extends PagedListDataModel<BundleDeployment> {
        private DataModel(AbstractPagedDataUIBean pagedDataBean) {
            super(pagedDataBean);
        }

        @Override
        public PageList<BundleDeployment> fetchPage(PageControl pageControl) {
            PerspectiveClientUIBean perspectiveClient = BundleVersionDetailsUIBean.this.perspectiveClient;
            BundleManagerRemote bundleManager;
            Subject subject;
            try {
                bundleManager = perspectiveClient.getRemoteClient().getBundleManagerRemote();
                subject = BundleVersionDetailsUIBean.this.perspectiveClient.getSubject();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            BundleDeploymentCriteria bundleDeploymentCriteria = new BundleDeploymentCriteria();
            bundleDeploymentCriteria.setPageControl(pageControl);
            // TODO
            //bundleDeploymentCriteria.addFilterVersionId(BundleVersionDetailsUIBean.this.bundleVersionId);
            bundleDeploymentCriteria.fetchBundle(true);
            // TODO: Implement user-specified filters.
            PageList<BundleDeployment> bundleDeployments = bundleManager.findBundleDeploymentsByCriteria(subject,
                    bundleDeploymentCriteria);
            return bundleDeployments;
        }
    }
}