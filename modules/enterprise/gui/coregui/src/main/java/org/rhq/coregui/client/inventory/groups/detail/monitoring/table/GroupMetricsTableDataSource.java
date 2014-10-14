package org.rhq.coregui.client.inventory.groups.detail.monitoring.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.ui.MetricDisplaySummary;
import org.rhq.core.domain.measurement.util.Moment;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.UserSessionManager;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.coregui.client.util.MeasurementUtility;
import org.rhq.coregui.client.util.preferences.MeasurementUserPreferences;
import org.rhq.coregui.client.util.preferences.UserPreferences;

/**
 * A simple data source to read in metric data summaries for a resource.
 * This doesn't support paging - everything is returned in one query. Since
 * the number of metrics per resource is relatively small (never more than tens of them),
 * we just load them all in at once.
 *
 * @author John Mazzitelli
 * @author Simeon PInder
 */
public class GroupMetricsTableDataSource extends MetricsTableDataSource {

    public static final String FIELD_MEMBERS_REPORTING = "membersReporting";

    private ResourceGroupComposite groupComposite;

    public GroupMetricsTableDataSource(ResourceGroupComposite groupComposite) {
        // this is a little ugly, the subclass expects a resourceId, we override everything relevant to
        // ensure everything works and we deal with the group.
        super(groupComposite.getResourceGroup().getId());
        this.groupComposite = groupComposite;
    }

    /**
     * The view that contains the list grid which will display this datasource's data will call this
     * method to get the field information which is used to control the display of the data.
     *
     * @return list grid fields used to display the datasource data
     */
    public ArrayList<ListGridField> getListGridFields() {
        ArrayList<ListGridField> fields = new ArrayList<ListGridField>(6);

        ListGridField memberCountField = new ListGridField(FIELD_MEMBERS_REPORTING,
            MSG.common_title_members_reporting());
        memberCountField.setWidth("15%");
        fields.add(memberCountField);

        ListGridField nameField = new ListGridField(FIELD_METRIC_LABEL, MSG.common_title_name());
        //launching modal window, not normal link so javascript target set.
        nameField.setType(ListGridFieldType.LINK);
        nameField.setTarget("javascript");
        nameField.setWidth("30%");
        fields.add(nameField);

        ListGridField alertsField = new ListGridField(FIELD_ALERT_COUNT, MSG.common_title_alerts());
        alertsField.setWidth("10%");
        fields.add(alertsField);

        ListGridField minField = new ListGridField(FIELD_MIN_VALUE, MSG.common_title_monitor_minimum());
        minField.setWidth("15%");
        fields.add(minField);

        ListGridField maxField = new ListGridField(FIELD_MAX_VALUE, MSG.common_title_monitor_maximum());
        maxField.setWidth("15%");
        fields.add(maxField);

        ListGridField avgField = new ListGridField(FIELD_AVG_VALUE, MSG.common_title_monitor_average());
        avgField.setWidth("15%");
        fields.add(avgField);

        return fields;
    }

    @Override
    public ListGridRecord copyValues(MetricDisplaySummary from) {
        MeasurementUtility.formatSimpleMetrics(from);

        ListGridRecord record = new ListGridRecord();
        record.setAttribute(FIELD_METRIC_LABEL, from.getLabel());
        record.setAttribute(FIELD_ALERT_COUNT, String.valueOf(from.getAlertCount()));
        record.setAttribute(FIELD_MIN_VALUE, getMetricStringValue(from.getMinMetric()));
        record.setAttribute(FIELD_MAX_VALUE, getMetricStringValue(from.getMaxMetric()));
        record.setAttribute(FIELD_AVG_VALUE, getMetricStringValue(from.getAvgMetric()));
        record.setAttribute(FIELD_METRIC_DEF_ID, from.getDefinitionId());
        record.setAttribute(FIELD_METRIC_SCHED_ID, from.getScheduleId());
        record.setAttribute(FIELD_METRIC_UNITS, from.getUnits());
        record.setAttribute(FIELD_METRIC_NAME, from.getMetricName());
        record.setAttribute(FIELD_MEMBERS_REPORTING, from.getNumberCollecting());
        return record;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response, final Criteria unused) {
        final ResourceGroupComposite groupComposite = this.groupComposite;
        final ResourceGroup group = groupComposite.getResourceGroup();
        // Load the fully fetched ResourceType.
        ResourceType groupType = group.getResourceType();
        ResourceTypeRepository.Cache.getInstance().getResourceTypes(
            groupType.getId(),
            EnumSet.of(ResourceTypeRepository.MetadataType.content, ResourceTypeRepository.MetadataType.operations,
                ResourceTypeRepository.MetadataType.measurements, ResourceTypeRepository.MetadataType.events,
                ResourceTypeRepository.MetadataType.resourceConfigurationDefinition),
            new ResourceTypeRepository.TypeLoadedCallback() {
                public void onTypesLoaded(ResourceType type) {
                    group.setResourceType(type);
                    //metric definitions
                    Set<MeasurementDefinition> definitions = type.getMetricDefinitions();

                    //build id mapping for measurementDefinition instances Ex. Free Memory -> MeasurementDefinition[100071]
                    final HashMap<String, MeasurementDefinition> measurementDefMap = new HashMap<String, MeasurementDefinition>();
                    for (MeasurementDefinition definition : definitions) {
                        measurementDefMap.put(definition.getDisplayName(), definition);
                    }
                    //bundle definition ids for asynch call.
                    int[] definitionArrayIds = new int[definitions.size()];
                    final String[] displayOrder = new String[definitions.size()];
                    measurementDefMap.keySet().toArray(displayOrder);
                    //sort the charting data ex. Free Memory, Free Swap Space,..System Load
                    Arrays.sort(displayOrder);

                    //organize definitionArrayIds for ordered request on server.
                    int index = 0;
                    for (String definitionToDisplay : displayOrder) {
                        definitionArrayIds[index++] = measurementDefMap.get(definitionToDisplay).getId();
                    }

                    UserPreferences prefs = UserSessionManager.getUserPreferences();
                    MeasurementUserPreferences mprefs = new MeasurementUserPreferences(prefs);
                    ArrayList<Moment> range = mprefs.getMetricRangePreferences().getBeginEndTimes();

                    //now retrieve metric display summaries
                    GWTServiceLookup.getMeasurementChartsService().getMetricDisplaySummariesForCompatibleGroup(
                        EntityContext.forGroup(group), definitionArrayIds, range.get(0), range.get(1), false,
                        new AsyncCallback<ArrayList<MetricDisplaySummary>>() {
                            @Override
                            public void onSuccess(ArrayList<MetricDisplaySummary> result) {
                                ArrayList<MetricDisplaySummary> validSummaries = new ArrayList<MetricDisplaySummary>();
                                for (MetricDisplaySummary mds : result) {
                                    if (mds.getValuesPresent()) {//include only populated datapoints.
                                        validSummaries.add(mds);
                                    }
                                }
                                response.setData(buildRecords(validSummaries));
                                processResponse(request.getRequestId(), response);
                            }

                            @Override
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError("Cannot load metrics", caught);
                            }
                        });
                }
            });
    }
}
