package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.schedules;

import com.smartgwt.client.data.Criteria;
import org.rhq.core.domain.criteria.MeasurementScheduleCriteria;
import org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMeasurementScheduleListView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.schedules.SchedulesDataSource;

/**
 * The group Monitoring>Schedules subtab.
 *
 * @author Ian Springer
 */
public class SchedulesView extends AbstractMeasurementScheduleListView {
    private static final String[] EXCLUDED_FIELD_NAMES = new String[] { MeasurementScheduleCriteria.FILTER_FIELD_RESOURCE_GROUP_ID };

    public SchedulesView(int resourceGroupId) {
        super(new SchedulesDataSource(resourceGroupId), createCriteria(resourceGroupId), EXCLUDED_FIELD_NAMES);
    }

    private static Criteria createCriteria(int resourceGroupId) {
        Criteria criteria = new Criteria();
        criteria.addCriteria(MeasurementScheduleCriteria.FILTER_FIELD_RESOURCE_GROUP_ID, resourceGroupId);
        return criteria;
    }
}
