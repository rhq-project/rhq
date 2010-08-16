package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.schedules;

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
 * A DataSource for reading an updating the metric schedules for the current Resource.
 *
 * @author Ian Springer
 */
public class SchedulesDataSource extends AbstractMeasurementScheduleDataSource {
    private MeasurementDataGWTServiceAsync measurementService = GWTServiceLookup.getMeasurementDataService();
    private int resourceId;

    public SchedulesDataSource(int resourceId) {
        this.resourceId = resourceId;
    }

    @Override
    protected List<DataSourceField> createFields() {
        List<DataSourceField> fields = super.createFields();
        DataSourceField resourceIdField = new DataSourceIntegerField(MeasurementScheduleCriteria.FILTER_FIELD_RESOURCE_ID,
                "Resource Id");
        resourceIdField.setHidden(true);
        fields.add(resourceIdField);
        return fields;
    }

    @Override
    public ListGridRecord copyValues(MeasurementSchedule from) {
        ListGridRecord record = super.copyValues(from);
        record.setAttribute(MeasurementScheduleCriteria.FILTER_FIELD_RESOURCE_ID, this.resourceId);
        return record;
    }

    @Override
    protected void enableSchedules(AbstractMeasurementScheduleListView measurementScheduleListView,
                                   int[] measurementDefinitionIds) {
         this.measurementService.enableSchedulesForResource(this.resourceId, measurementDefinitionIds,
             new AsyncCallback<Void>() {
             @Override
             public void onFailure(Throwable throwable) {
                 CoreGUI.getErrorHandler().handleError("Failed to enable specified metric schedules for Resource with id[" + resourceId + "].",
                         throwable);
             }

             @Override
             public void onSuccess(Void aVoid) {
                 CoreGUI.getMessageCenter().notify(new Message("Specified Metric schedules for Resource with id [" + resourceId +
                                         "] enabled.", Message.Severity.Info));

             }
         });
    }

    @Override
    protected void disableSchedules(AbstractMeasurementScheduleListView measurementScheduleListView, int[] measurementDefinitionIds) {
         this.measurementService.disableSchedulesForResource(this.resourceId, measurementDefinitionIds,
             new AsyncCallback<Void>() {
             @Override
             public void onFailure(Throwable throwable) {
                 CoreGUI.getErrorHandler().handleError("Failed to disable specified metric schedules for Resource with id[" + resourceId + "].",
                         throwable);
             }

             @Override
             public void onSuccess(Void aVoid) {
                 CoreGUI.getMessageCenter().notify(new Message("Specified metric schedules for Resource with id [" + resourceId +
                                         "] disabled.", Message.Severity.Info));

             }
         });
    }

    @Override
    protected void updateSchedules(AbstractMeasurementScheduleListView measurementScheduleListView,
                                   int[] measurementDefinitionIds, long collectionInterval) {
        this.measurementService.updateSchedulesForResource(this.resourceId, measurementDefinitionIds, collectionInterval,
            new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable throwable) {
                CoreGUI.getErrorHandler().handleError("Failed to update specified metric schedules for Resource with id[" + resourceId + "].",
                        throwable);
            }

            @Override
            public void onSuccess(Void aVoid) {
                CoreGUI.getMessageCenter().notify(new Message("Specified Metric schedules for Resource with id [" + resourceId +
                                        "] updated.", Message.Severity.Info));

            }
        });
    }
}
