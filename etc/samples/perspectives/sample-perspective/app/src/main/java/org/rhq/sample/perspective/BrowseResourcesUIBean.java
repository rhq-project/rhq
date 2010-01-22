/*
 * RHQ Management Platform
 * Copyright (C) 2009-2010 Red Hat, Inc.
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
package org.rhq.sample.perspective;

import java.util.List;
import java.util.Map;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.model.PagedDataModel;
import org.rhq.core.gui.model.PagedDataProvider;
import org.rhq.enterprise.client.RemoteClient;
import org.rhq.enterprise.server.perspective.AbstractPagedDataPerspectiveUIBean;
import org.rhq.enterprise.server.perspective.PerspectiveManagerRemote;
import org.rhq.enterprise.server.perspective.PerspectiveTarget;
import org.rhq.enterprise.server.resource.ResourceManagerRemote;

/**
 * A Seam component that utilizes the RHQ remote API to obtain a paged list of all inventoried Resources.
 *
 * @author Ian Springer
 */
@Name("BrowseResourcesUIBean")
@Scope(ScopeType.CONVERSATION)
public class BrowseResourcesUIBean extends AbstractPagedDataPerspectiveUIBean {
    private PagedDataModel<Resource> dataModel;
    private List<Resource> selectedResources;
    private Map<Integer, String> resourceUrlMap;

    @Begin(join = true)
    public PagedDataModel<Resource> getDataModel() throws Exception {
        if (this.dataModel == null) {
            this.dataModel = createDataModel();
        }
        return this.dataModel;
    }

    public List<Resource> getSelectedResources() {
        return this.selectedResources;
    }

    public void setSelectedResources(List<Resource> selectedResources) {
        this.selectedResources = selectedResources;
    }

    public void uninventorySelectedResources() throws Exception {
        RemoteClient remoteClient = getRemoteClient();
        Subject subject = getSubject();

        // ***NOTE***: The javassist.NotFoundException stack traces that are logged by this call can be ignored.
        ResourceManagerRemote resourceManager = remoteClient.getResourceManagerRemote();

        int[] selectedResourceIds = new int[this.selectedResources.size()];
        for (int i = 0, selectedResourcesSize = this.selectedResources.size(); i < selectedResourcesSize; i++) {
            Resource selectedResource = this.selectedResources.get(i);
            selectedResourceIds[i] = selectedResource.getId();
        }

        resourceManager.uninventoryResources(subject, selectedResourceIds);

        // Add message to tell the user the uninventory was a success.
        String pluralizer = (this.selectedResources.size() == 1) ? "" : "s";
        getFacesMessages().add("Uninventoried " + this.selectedResources.size() + " Resource" + pluralizer + ".");

        // Reset the data model, so the current page will get refreshed to reflect the Resources we just uninventoried.
        this.dataModel = null;
    }

    @Override
    protected PageControl getDefaultPageControl() {
        PageControl defaultPageControl = super.getDefaultPageControl();
        defaultPageControl.addDefaultOrderingField("r.id");
        return defaultPageControl;
    }

    private PagedDataModel<Resource> createDataModel() throws Exception {
        RemoteClient remoteClient = getRemoteClient();
        Subject subject = getSubject();

        // ***NOTE***: The javassist.NotFoundException stack traces that are logged by this call can be ignored.
        ResourceManagerRemote resourceManager = remoteClient.getResourceManagerRemote();

        ResourcesDataProvider dataProvider = new ResourcesDataProvider(subject, resourceManager);
        return new PagedDataModel<Resource>(dataProvider);
    }

    private class ResourcesDataProvider implements PagedDataProvider<Resource> {
        private Subject subject;
        private ResourceManagerRemote resourceManager;

        public ResourcesDataProvider(Subject subject, ResourceManagerRemote resourceManager) {
            this.subject = subject;
            this.resourceManager = resourceManager;
        }

        public PageList<Resource> getDataPage(PageControl pageControl) {
            ResourceCriteria resourceCriteria = new ResourceCriteria();
            resourceCriteria.setPageControl(pageControl);
            PageList<Resource> resources = this.resourceManager.findResourcesByCriteria(this.subject, resourceCriteria);
            setLinkBackUrls(resources);
            return resources;
        }
    }

    private void setLinkBackUrls(List<Resource> resources) {
        int[] ids = new int[resources.size()];
        for (int i = 0, size = resources.size(); (i < size); ++i) {
            ids[i] = resources.get(i).getId();
        }

        try {
            RemoteClient remoteClient = getRemoteClient();
            Subject subject = getSubject();
            PerspectiveManagerRemote perspectiveManager = remoteClient.getPerspectiveManagerRemote();
            this.resourceUrlMap = perspectiveManager.getTargetUrls(subject, PerspectiveTarget.RESOURCE, ids, false,
                false);
        } catch (Exception e) {
            // for the demo, just dump a stack in this unlikely case
            e.printStackTrace();
        }
    }

    public String getResourceUrl(int resourceId) {
        String url = this.resourceUrlMap.get(resourceId);
        return url;
    }

}