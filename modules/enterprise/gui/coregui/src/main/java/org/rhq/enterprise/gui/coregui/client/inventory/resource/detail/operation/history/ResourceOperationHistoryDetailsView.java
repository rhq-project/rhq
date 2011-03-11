/*
 * RHQ Management Platform
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.history;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;

import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.operation.GroupOperationHistory;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.operation.history.AbstractOperationHistoryDetailsView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Ian Springer
 */
public class ResourceOperationHistoryDetailsView extends AbstractOperationHistoryDetailsView<ResourceOperationHistory> {

    private String disambiguatedResourceName;
    private boolean showResourceField;

    public ResourceOperationHistoryDetailsView(String locatorId) {
        this(locatorId, false);
    }

    public ResourceOperationHistoryDetailsView(String locatorId, boolean showResourceField) {
        super(locatorId);

        this.showResourceField = showResourceField;
    }

    @Override
    protected List<FormItem> createFields(ResourceOperationHistory operationHistory) {
        List<FormItem> items = super.createFields(operationHistory);

        if (this.showResourceField) {
            StaticTextItem resourceItem = new StaticTextItem(ResourceOperationHistoryDataSource.Field.RESOURCE,
                "Resource");
            resourceItem.setValue(this.disambiguatedResourceName);
            items.add(1, resourceItem);
        }

        GroupOperationHistory groupOperationHistory = operationHistory.getGroupOperationHistory();
        if (groupOperationHistory != null) {
            StaticTextItem groupOperationHistoryItem = new StaticTextItem(
                ResourceOperationHistoryDataSource.Field.GROUP_OPERATION_HISTORY, "Parent Group Execution");
            String groupOperationHistoryUrl = LinkManager.getGroupOperationHistoryLink(groupOperationHistory.getGroup()
                .getId(), groupOperationHistory.getId());
            String value = "<a href=\"" + groupOperationHistoryUrl + "\">" + groupOperationHistory.getId()
                + "</a> (on group '" + groupOperationHistory.getGroup().getName() + "')";
            groupOperationHistoryItem.setValue(value);
            items.add(groupOperationHistoryItem);
        }

        return items;
    }

    protected void lookupDetails(int historyId) {
        ResourceOperationHistoryCriteria criteria = new ResourceOperationHistoryCriteria();

        criteria.addFilterId(historyId);

        criteria.fetchOperationDefinition(true);
        criteria.fetchParameters(true);
        criteria.fetchResults(true);

        GWTServiceLookup.getOperationService().findResourceOperationHistoriesByCriteria(criteria,
            new AsyncCallback<PageList<ResourceOperationHistory>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler()
                        .handleError(MSG.view_operationHistoryDetails_error_fetchFailure(), caught);
                }

                public void onSuccess(PageList<ResourceOperationHistory> result) {
                    ResourceOperationHistory resourceOperationHistory = result.get(0);

                    if (showResourceField) {
                        Resource resource = resourceOperationHistory.getResource();
                        disambiguatedResourceName = AncestryUtil.getResourceLongName(resource.getId(), resource
                            .getName(), resource.getResourceType());
                    }

                    displayDetails(resourceOperationHistory);
                }
            });
    }

    @Override
    protected Canvas buildResultsSection(ResourceOperationHistory operationHistory) {
        OperationRequestStatus status = operationHistory.getStatus();
        if (status == OperationRequestStatus.SUCCESS) {
            LocatableVLayout resultsSection = new LocatableVLayout(extendLocatorId("ResultsSection"));

            Label title = new Label("<h4>" + MSG.view_operationHistoryDetails_results() + "</h4>");
            title.setHeight(27);
            resultsSection.addMember(title);

            OperationDefinition operationDefinition = operationHistory.getOperationDefinition();
            ConfigurationDefinition resultsConfigurationDefinition = operationDefinition
                .getResultsConfigurationDefinition();
            if (resultsConfigurationDefinition != null
                && !resultsConfigurationDefinition.getPropertyDefinitions().isEmpty()) {
                ConfigurationEditor editor = new ConfigurationEditor(extendLocatorId("results"), operationDefinition
                    .getResultsConfigurationDefinition(), operationHistory.getResults());
                editor.setReadOnly(true);
                resultsSection.addMember(editor);
            } else {
                // TODO: i18n
                Label noResultsLabel = new Label("This operation does not return any results.");
                noResultsLabel.setHeight(17);
                resultsSection.addMember(noResultsLabel);
            }

            return resultsSection;
        } else {
            return null;
        }
    }

}
