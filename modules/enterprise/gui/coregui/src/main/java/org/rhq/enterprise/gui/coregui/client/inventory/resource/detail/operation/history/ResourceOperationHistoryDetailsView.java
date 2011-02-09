package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.history;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.operation.history.AbstractOperationHistoryDetailsView;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Ian Springer
 */
public class ResourceOperationHistoryDetailsView extends AbstractOperationHistoryDetailsView<ResourceOperationHistory> {

    public ResourceOperationHistoryDetailsView(String locatorId) {
        super(locatorId);
    }

    public ResourceOperationHistoryDetailsView(String locatorId, OperationDefinition definition,
                                               ResourceOperationHistory operationHistory) {
        super(locatorId, definition, operationHistory);
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
                    ResourceOperationHistory item = result.get(0);
                    displayDetails(item);
                }
            });
    }

    @Override
    protected Canvas buildResultsSection(ResourceOperationHistory operationHistory) {
        OperationRequestStatus status = operationHistory.getStatus();
        if (status == OperationRequestStatus.SUCCESS && operationHistory.getResults() != null) {
            LocatableVLayout resultsSection = new LocatableVLayout(extendLocatorId("ResultsSection"));

            Label title = new Label("<h4>" + MSG.view_operationHistoryDetails_results() + "</h4>");
            title.setHeight(27);
            resultsSection.addMember(title);

            OperationDefinition operationDefinition = operationHistory.getOperationDefinition();
            ConfigurationEditor editor = new ConfigurationEditor(extendLocatorId("results"), operationDefinition
                .getResultsConfigurationDefinition(), operationHistory.getResults());
            editor.setReadOnly(true);
            resultsSection.addMember(editor);

            return resultsSection;
        } else {
            return null;
        }
    }

}
