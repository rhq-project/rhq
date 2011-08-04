/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.gui.inventory.resource;

import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.resource.CreateResourceHistory;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.resource.ResourceFactoryManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Jason Dobies
 * @author Ian Springer
 */
public class DetermineChildResourceCreationTypeUIBean {
    private static final String OUTCOME_NO_TYPE = "noTypeSelected";
    private static final String OUTCOME_ARTIFACT = "artifact";
    private static final String OUTCOME_CONFIGURATION_MULTIPLE_TEMPLATES = "configuration-multipleTemplates";
    private static final String OUTCOME_CONFIGURATION_SINGLE_TEMPLATE = "configuration-singleTemplate";

    private CreateResourceHistory retryCreateItem;

    /**
     * Determines which branch of the create workflow to take, based on the resource type selected (i.e. show the user
     * the resource configuration or the create artifact form).
     *
     * @return action forward
     */
    public String determineCreationType() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        ResourceType resourceType;

        // When the drop down was removed, a link containing the ID as a request parameter was added. Handle
        // that ID now to load the failed request if there was one. 
        HttpServletRequest request = FacesContextUtility.getRequest();
        String sRetryCreateItemId = request.getParameter("retryCreateItemId");

        if (sRetryCreateItemId != null) {
            ResourceFactoryManagerLocal resourceFactoryManager = LookupUtil.getResourceFactoryManager();
            int retryCreateItemId = Integer.parseInt(sRetryCreateItemId);
            retryCreateItem = resourceFactoryManager.getCreateHistoryItem(retryCreateItemId);
        }

        if (this.retryCreateItem != null) {
            // This is a retry of an earlier failed create request.
            // Any data that should be prepopulated into the resulting workflow will be loaded. For instance, if the create
            // call failed because of an invalid configuration, the new page in the workflow should show the previous
            // configuration, with any plugin-side validation errors that occurred.
            resourceType = this.retryCreateItem.getResourceType();

            FacesContextUtility.getFacesContext().getExternalContext().getRequestMap().put(
                ParamConstants.RESOURCE_TYPE_ID_PARAM, resourceType.getId());

            switch (resourceType.getCreationDataType()) {
            case CONTENT: {
                CreateNewPackageChildResourceUIBean createPackageBean = FacesContextUtility
                    .getManagedBean(CreateNewPackageChildResourceUIBean.class);
                createPackageBean.setResourceType(this.retryCreateItem.getResourceType());
                createPackageBean.setConfiguration(this.retryCreateItem.getConfiguration());
                break;
            }

            case CONFIGURATION: {
                CreateNewConfigurationChildResourceUIBean createConfigBean = FacesContextUtility
                    .getManagedBean(CreateNewConfigurationChildResourceUIBean.class);
                createConfigBean.setResourceName(this.retryCreateItem.getCreatedResourceName());
                createConfigBean.setResourceType(this.retryCreateItem.getResourceType());
                createConfigBean.setConfiguration(this.retryCreateItem.getConfiguration());
                break;
            }
            }
        } else {
            ResourceTypeManagerLocal resourceTypeManager = LookupUtil.getResourceTypeManager();
            int resourceTypeId = EnterpriseFacesContextUtility.getResourceType().getId();
            try {
                resourceType = resourceTypeManager.getResourceTypeById(subject, resourceTypeId);
            } catch (ResourceTypeNotFoundException e) {
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                    "Could not retrieve resource type for resource type id: " + resourceTypeId);
                return OUTCOME_NO_TYPE;
            }
        }

        String outcome = OUTCOME_NO_TYPE;

        switch (resourceType.getCreationDataType()) {
        case CONTENT: {
            outcome = OUTCOME_ARTIFACT;
            break;
        }

        case CONFIGURATION: {
            ConfigurationDefinition configurationDefinition = lookupConfigurationDefinition(resourceType.getId());
            outcome = (configurationDefinition.getTemplates().size() > 1) ? OUTCOME_CONFIGURATION_MULTIPLE_TEMPLATES
                : OUTCOME_CONFIGURATION_SINGLE_TEMPLATE;
            break;
        }
        }

        return outcome;
    }

    /**
     * Temporary hack to get around the bug in the RichFaces data table that breaks links in each row (see
     * http://jira.jboss.com/jira/browse/RF-250). Ultimately, each failed row will have a button to retry the create.
     * Until that bug is fixed, use a separate dropdown with each failed create to select the one to retry. Note, Exadel
     * claims RF-250 is fixed in the 3.1.0 nightlies, but we're still seeing the issue.
     *
     * @return list of failed create history requests
     */
    public SelectItem[] getFailedCreateHistory() {
        Subject user = EnterpriseFacesContextUtility.getSubject();
        Resource parentResource = EnterpriseFacesContextUtility.getResource();

        ResourceFactoryManagerLocal resourceFactoryManager = LookupUtil.getResourceFactoryManager();
        PageControl pageControl = new PageControl(0, 1000);
        PageList<CreateResourceHistory> pageList = resourceFactoryManager.findCreateChildResourceHistory(user,
            parentResource.getId(), null, null, pageControl);

        List<SelectItem> selectItemsList = new ArrayList<SelectItem>();
        for (CreateResourceHistory history : pageList) {
            if ((history.getStatus() == CreateResourceStatus.FAILURE)
                || (history.getStatus() == CreateResourceStatus.TIMED_OUT)) {
                selectItemsList.add(new SelectItem(history, history.getCreatedResourceName()));
            }
        }

        SelectItem[] items = selectItemsList.toArray(new SelectItem[selectItemsList.size()]);
        return items;
    }

    private ConfigurationDefinition lookupConfigurationDefinition(int resourceTypeId) {
        Subject user = EnterpriseFacesContextUtility.getSubject();
        ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();
        ConfigurationDefinition configurationDefinition = configurationManager
            .getResourceConfigurationDefinitionWithTemplatesForResourceType(user, resourceTypeId);
        return configurationDefinition;
    }

    public CreateResourceHistory getRetryCreateItem() {
        return retryCreateItem;
    }

    public void setRetryCreateItem(CreateResourceHistory retryCreateItem) {
        this.retryCreateItem = retryCreateItem;
    }

}