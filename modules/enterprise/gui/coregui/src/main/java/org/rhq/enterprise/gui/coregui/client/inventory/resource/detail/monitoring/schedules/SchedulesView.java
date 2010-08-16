package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.schedules;

import com.smartgwt.client.data.Criteria;
import org.rhq.core.domain.criteria.MeasurementScheduleCriteria;
import org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMeasurementScheduleListView;

/**
 * The Resource Monitoring>Schedules subtab.
 *
 * @author Ian Springer
 */
public class SchedulesView extends AbstractMeasurementScheduleListView {
    private static final String[] EXCLUDED_FIELD_NAMES = new String[] { MeasurementScheduleCriteria.FILTER_FIELD_RESOURCE_ID };

    public SchedulesView(int resourceId) {
        super(new SchedulesDataSource(resourceId), createCriteria(resourceId), EXCLUDED_FIELD_NAMES);
    }

    private static Criteria createCriteria(int resourceId) {
        Criteria criteria = new Criteria();
        criteria.addCriteria(MeasurementScheduleCriteria.FILTER_FIELD_RESOURCE_ID, resourceId);
        return criteria;
    }
}
