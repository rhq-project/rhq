package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.operation.history;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import org.rhq.core.domain.criteria.GroupOperationHistoryCriteria;
import org.rhq.core.domain.operation.GroupOperationHistory;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.operation.history.AbstractOperationHistoryDetailsView;

/**
 * @author Ian Springer
 */
public class GroupOperationHistoryDetailsView extends AbstractOperationHistoryDetailsView<GroupOperationHistory> {

    public GroupOperationHistoryDetailsView(String locatorId) {
        super(locatorId);
    }

    public GroupOperationHistoryDetailsView(String locatorId, OperationDefinition definition,
                                            GroupOperationHistory operationHistory) {
        super(locatorId, definition, operationHistory);
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
                        GroupOperationHistory item = result.get(0);
                        displayDetails(item);
                    }
                });
    }

    @Override
    protected Canvas buildResultsSection(GroupOperationHistory operationHistory) {
        OperationRequestStatus status = operationHistory.getStatus();

        return new Label("<h5>TODO</h5>");
    }

}
