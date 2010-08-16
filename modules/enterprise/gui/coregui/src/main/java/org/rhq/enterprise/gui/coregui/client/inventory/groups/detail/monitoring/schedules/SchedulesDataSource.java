package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.schedules;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import org.rhq.core.domain.criteria.MeasurementScheduleCriteria;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.MeasurementDataGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMeasurementScheduleDataSource;
import org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMeasurementScheduleListView;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

import java.util.List;

/**
 * A DataSource for reading and updating the metric schedules for the current group.
 *
 * @author Ian Springer
 */
public class SchedulesDataSource extends AbstractMeasurementScheduleDataSource {
    private MeasurementDataGWTServiceAsync measurementService = GWTServiceLookup.getMeasurementDataService();
    private int resourceGroupId;

    public SchedulesDataSource(int resourceGroupId) {
        this.resourceGroupId = resourceGroupId;
    }

    @Override
    protected List<DataSourceField> createFields() {
        List<DataSourceField> fields = super.createFields();
        DataSourceField resourceGroupIdField = new DataSourceIntegerField(MeasurementScheduleCriteria.FILTER_FIELD_RESOURCE_GROUP_ID,
                "Resource Group Id");
        resourceGroupIdField.setHidden(true);
        fields.add(resourceGroupIdField);
        return fields;
    }

    @Override
    public ListGridRecord copyValues(MeasurementSchedule from) {
        ListGridRecord record = super.copyValues(from);
        record.setAttribute(MeasurementScheduleCriteria.FILTER_FIELD_RESOURCE_GROUP_ID, this.resourceGroupId);
        return record;
    }

    @Override
    protected void enableSchedules(AbstractMeasurementScheduleListView measurementScheduleListView,
                                   int[] measurementDefinitionIds, final List<String> measurementDefinitionDisplayNames) {
         this.measurementService.enableSchedulesForCompatibleGroup(this.resourceGroupId, measurementDefinitionIds,
             new AsyncCallback<Void>() {
                 @Override
                 public void onFailure(Throwable throwable) {
                     CoreGUI.getErrorHandler().handleError("Failed to enable collection of metrics "
                             + measurementDefinitionDisplayNames + " for Resource group with id [" + resourceGroupId + "].",
                             throwable);
                 }

                 @Override
                 public void onSuccess(Void aVoid) {
                     CoreGUI.getMessageCenter().notify(new Message("Enabled collection of metrics "
                             + measurementDefinitionDisplayNames + " for Resource group with id [" + resourceGroupId +
                                             "].", Message.Severity.Info));

                 }
         });
    }

    @Override
    protected void disableSchedules(AbstractMeasurementScheduleListView measurementScheduleListView, int[] measurementDefinitionIds, final List<String> measurementDefinitionDisplayNames) {
         this.measurementService.disableSchedulesForCompatibleGroup(this.resourceGroupId, measurementDefinitionIds,
             new AsyncCallback<Void>() {
             @Override
             public void onFailure(Throwable throwable) {
                 CoreGUI.getErrorHandler().handleError("Failed to disable collection of metrics "
                         + measurementDefinitionDisplayNames + " for Resource group with id [" + resourceGroupId + "].",
                         throwable);
             }

             @Override
             public void onSuccess(Void aVoid) {
                 CoreGUI.getMessageCenter().notify(new Message("Disabled collection of metrics "
                         + measurementDefinitionDisplayNames + " for Resource group with id [" + resourceGroupId +
                                         "].", Message.Severity.Info));

             }
         });
    }

    @Override
    protected void updateSchedules(AbstractMeasurementScheduleListView measurementScheduleListView,
                                   int[] measurementDefinitionIds, final List<String> measurementDefinitionDisplayNames, final long collectionInterval) {
        this.measurementService.updateSchedulesForCompatibleGroup(this.resourceGroupId, measurementDefinitionIds, collectionInterval,
            new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable throwable) {
                CoreGUI.getErrorHandler().handleError("Failed to set collection interval to " + (collectionInterval / 1000)
                        + " seconds for metrics " + measurementDefinitionDisplayNames + " for Resource group with id ["
                        + resourceGroupId + "].",
                        throwable);
            }

            @Override
            public void onSuccess(Void aVoid) {
                CoreGUI.getMessageCenter().notify(new Message("Collection interval for metrics "
                        + measurementDefinitionDisplayNames + " for Resource group with id [" + resourceGroupId +
                                        "] set to " + (collectionInterval / 1000) + " seconds.", Message.Severity.Info));

            }
        });
    }
}
