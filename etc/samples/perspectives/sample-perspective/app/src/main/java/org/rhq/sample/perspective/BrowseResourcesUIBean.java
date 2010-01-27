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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ajax4jsf.event.AjaxEvent;
import org.ajax4jsf.event.AjaxListener;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

import org.jboss.seam.international.StatusMessage;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.model.PagedDataProvider;
import org.rhq.core.gui.table.component.SortableColumnHeaderComponent;
import org.rhq.core.gui.table.model.PagedListDataModel;
import org.rhq.core.gui.util.FacesComponentUtility;
import org.rhq.enterprise.client.RemoteClient;
import org.rhq.enterprise.server.perspective.AbstractPerspectivePagedDataUIBean;
import org.rhq.enterprise.server.perspective.PerspectiveManagerRemote;
import org.rhq.enterprise.server.perspective.PerspectiveTarget;
import org.rhq.enterprise.server.resource.ResourceManagerRemote;

import javax.faces.component.UIData;

/**
 * A Seam component that utilizes the RHQ remote API to obtain a paged list of all inventoried Resources.
 *
 * @author Ian Springer
 */
@Name("BrowseResourcesUIBean")
@Scope(ScopeType.CONVERSATION)
public class BrowseResourcesUIBean extends AbstractPerspectivePagedDataUIBean implements AjaxListener {
    private List<Resource> selectedResources;
    private Map<Integer, String> resourceUrlMap = new HashMap<Integer, String>();

    public List<Resource> getSelectedResources() {
        return this.selectedResources;
    }

    public void setSelectedResources(List<Resource> selectedResources) {
        this.selectedResources = selectedResources;
    }

    public void uninventorySelectedResources() throws Exception {
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
        ResourceManagerRemote resourceManager = remoteClient.getResourceManagerRemote();

        int[] selectedResourceIds = new int[this.selectedResources.size()];
        for (int i = 0, selectedResourcesSize = this.selectedResources.size(); i < selectedResourcesSize; i++) {
            Resource selectedResource = this.selectedResources.get(i);
            selectedResourceIds[i] = selectedResource.getId();
        }

        resourceManager.uninventoryResources(subject, selectedResourceIds);

        // Add message to tell the user the uninventory was a success.
        String pluralizer = (this.selectedResources.size() == 1) ? "" : "s";
        this.facesMessages.add("Uninventoried " + this.selectedResources.size() + " Resource" + pluralizer + ".");

        // Reset the data model, so the current page will get refreshed to reflect the Resources we just uninventoried.
        // This is essential, since we are CONVERSATION-scoped and will live on beyond this request.
        setDataModel(null);
    }

    @Override
    protected PageControl getDefaultPageControl() {
        PageControl defaultPageControl = super.getDefaultPageControl();
        defaultPageControl.addDefaultOrderingField("r.id");
        return defaultPageControl;
    }

    public DataModel createDataModel() {
        RemoteClient remoteClient;
        Subject subject;
        try {
            remoteClient = this.perspectiveClient.getRemoteClient();
            subject = this.perspectiveClient.getSubject();
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect to RHQ Server.", e);
        }

        // ***NOTE***: The javassist.NotFoundException stack traces that are logged by this call can be ignored.
        ResourceManagerRemote resourceManager = remoteClient.getResourceManagerRemote();

        //ResourcesDataProvider dataProvider = new ResourcesDataProvider(subject, resourceManager);
        //return new PagedDataModel<Resource>(dataProvider);
        return new DataModel(subject, resourceManager);

    }

    private void setLinkBackUrls(List<Resource> resources) {
        int[] ids = new int[resources.size()];
        for (int i = 0, size = resources.size(); (i < size); ++i) {
            ids[i] = resources.get(i).getId();
        }

        try {
            RemoteClient remoteClient = this.perspectiveClient.getRemoteClient();
            Subject subject = this.perspectiveClient.getSubject();
            PerspectiveManagerRemote perspectiveManager = remoteClient.getPerspectiveManagerRemote();
            this.resourceUrlMap = perspectiveManager.getTargetUrls(subject, PerspectiveTarget.RESOURCE, ids, false,
                false);
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve Resource URLs.", e);
        }
    }

    public Map<Integer, String> getResourceUrlMap() {
        return this.resourceUrlMap;
    }

    public AjaxListener getAjaxSortListener() {
        return this;
    }

    public void processAjax(AjaxEvent ajaxEvent) {
        SortableColumnHeaderComponent sortableColumnHeader = (SortableColumnHeaderComponent)ajaxEvent.getComponent();
        String sortBy = sortableColumnHeader.getSortBy();

        UIData data = FacesComponentUtility.getAncestorOfType(sortableColumnHeader, UIData.class);
        PagedListDataModel<?> model = (PagedListDataModel<?>) data.getValue();

        PageControl pageControl = model.getPageControl();
        pageControl.sortBy(sortBy);
        // Even though its the same PageControl instance, call setPageControl() so the updated version gets
        // persisted.
        model.setPageControl(pageControl);
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

    private class DataModel extends PagedListDataModel<Resource> {
        private Subject subject;
        private ResourceManagerRemote resourceManager;

        private DataModel(Subject subject, ResourceManagerRemote resourceManager) {
            super(BrowseResourcesUIBean.this);
            this.subject = subject;
            this.resourceManager = resourceManager;
        }

        @Override
        public PageList<Resource> fetchPage(PageControl pageControl) {
            ResourceCriteria resourceCriteria = new ResourceCriteria();
            resourceCriteria.setPageControl(pageControl);
            PageList<Resource> resources = this.resourceManager.findResourcesByCriteria(this.subject, resourceCriteria);
            setLinkBackUrls(resources);
            return resources;
        }
    }
}