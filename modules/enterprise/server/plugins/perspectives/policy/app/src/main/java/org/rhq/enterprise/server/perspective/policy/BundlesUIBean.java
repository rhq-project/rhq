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
import org.jboss.seam.international.StatusMessage;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.criteria.BundleCriteria;
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
 * Provides CRUD operations on provisioning {@link Bundle}s. The backing bean for bundles.xhtml.
 *
 * @author Ian Springer
 */
@Name("BundlesUIBean")
@Scope(ScopeType.EVENT)
@KeepAlive
public class BundlesUIBean extends AbstractPerspectivePagedDataUIBean {
    private List<Bundle> selectedBundles;

    @Override
    public PagedListDataModel createDataModel() {
        return new DataModel(this);
    }

    public List<Bundle> getSelectedBundles() {
        return this.selectedBundles;
    }

    public void setSelectedBundles(List<Bundle> selectedResources) {
        this.selectedBundles = selectedResources;
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

        int[] selectedBundleIds = new int[this.selectedBundles.size()];
        for (int i = 0, selectedBundlesSize = this.selectedBundles.size(); i < selectedBundlesSize; i++) {
            Bundle selectedBundle = this.selectedBundles.get(i);
            selectedBundleIds[i] = selectedBundle.getId();
        }

        bundleManager.deleteBundles(subject, selectedBundleIds);

        // Add message to tell the user the uninventory was a success.
        String pluralizer = (this.selectedBundles.size() == 1) ? "" : "s";
        this.facesMessages.add("Deleted " + this.selectedBundles.size() + " bundle" + pluralizer + ".");

        // Reset the data model, so the current page will get refreshed to reflect the Resources we just uninventoried.
        // This is essential, since we are CONVERSATION-scoped and will live on beyond this request.
        setDataModel(null);
    }

    private class DataModel extends PagedListDataModel<Bundle> {
        private DataModel(AbstractPagedDataUIBean pagedDataBean) {
            super(pagedDataBean);
        }

        @Override
        public PageList<Bundle> fetchPage(PageControl pageControl) {
            PerspectiveClientUIBean perspectiveClient = BundlesUIBean.this.perspectiveClient;
            BundleManagerRemote bundleManager;
            Subject subject;
            try {
                bundleManager = perspectiveClient.getRemoteClient().getBundleManagerRemote();
                subject = BundlesUIBean.this.perspectiveClient.getSubject();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            BundleCriteria bundleCriteria = new BundleCriteria();
            // TODO: Implement filtering.
            PageList<Bundle> bundles = bundleManager.findBundlesByCriteria(subject, bundleCriteria);
            return bundles;
        }
    }
}
