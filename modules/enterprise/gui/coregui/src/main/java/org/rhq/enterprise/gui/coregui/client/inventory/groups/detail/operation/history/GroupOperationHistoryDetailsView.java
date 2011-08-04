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
package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.operation.history;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import org.rhq.core.domain.criteria.GroupOperationHistoryCriteria;
import org.rhq.core.domain.operation.GroupOperationHistory;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.operation.history.AbstractOperationHistoryDetailsView;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Ian Springer
 */
public class GroupOperationHistoryDetailsView extends AbstractOperationHistoryDetailsView<GroupOperationHistory> {

    private ResourceGroupComposite groupComposite;

    public GroupOperationHistoryDetailsView(String locatorId, ResourceGroupComposite groupComposite) {
        super(locatorId);

        this.groupComposite = groupComposite;
    }

    protected void lookupDetails(int historyId) {
        GroupOperationHistoryCriteria criteria = new GroupOperationHistoryCriteria();

        criteria.addFilterId(historyId);

        criteria.fetchOperationDefinition(true);
        criteria.fetchParameters(true);

        GWTServiceLookup.getOperationService().findGroupOperationHistoriesByCriteria(criteria,
                new AsyncCallback<PageList<GroupOperationHistory>>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler()
                                .handleError(MSG.view_operationHistoryDetails_error_fetchFailure(), caught);
                    }

                    public void onSuccess(PageList<GroupOperationHistory> result) {
                        GroupOperationHistory groupOperationHistory = result.get(0);
                        displayDetails(groupOperationHistory);
                    }
                });
    }

    @Override
    protected Canvas buildResultsSection(GroupOperationHistory operationHistory) {
        LocatableVLayout resultsSection = new LocatableVLayout(extendLocatorId("ResultsSection"));

        Label title = new Label("<h4>" + MSG.view_operationHistoryDetails_results() + "</h4>");
        title.setHeight(27);
        resultsSection.addMember(title);

        GroupMemberResourceOperationHistoryListView memberHistoryListView =
                new GroupMemberResourceOperationHistoryListView(extendLocatorId("MembersListView"), this.groupComposite,
                        getOperationHistory().getId());
        memberHistoryListView.setOverflow(Overflow.VISIBLE);
        memberHistoryListView.setHeight(200);
        resultsSection.addMember(memberHistoryListView);

        return resultsSection;
    }

}
