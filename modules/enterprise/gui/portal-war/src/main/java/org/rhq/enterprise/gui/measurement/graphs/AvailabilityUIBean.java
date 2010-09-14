package org.rhq.enterprise.gui.measurement.graphs;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.util.units.UnitNumber;
import org.rhq.core.clientapi.util.units.UnitsConstants;
import org.rhq.core.clientapi.util.units.UnitsFormat;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.enterprise.gui.legacy.DefaultConstants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.measurement.AvailabilityManagerLocal;
import org.rhq.enterprise.server.measurement.AvailabilityPoint;
import org.rhq.enterprise.server.measurement.MeasurementPreferences;
import org.rhq.enterprise.server.measurement.MeasurementPreferences.MetricRangePreferences;
import org.rhq.enterprise.server.util.LookupUtil;

public class AvailabilityUIBean {

    private final Log log = LogFactory.getLog(AvailabilityUIBean.class);

    private AvailabilityManagerLocal availabilityManager = LookupUtil.getAvailabilityManager();

    private List<AvailabilityPoint> data = null;
    private String percentage = "???"; // default

    public List<AvailabilityPoint> getData() {
        return data;
    }

    public String getPercentage() {
        return percentage;
    }

    public AvailabilityUIBean() {
        WebUser user = EnterpriseFacesContextUtility.getWebUser();
        MeasurementPreferences preferences = user.getMeasurementPreferences();

        EntityContext context = WebUtility.getEntityContext();
        try {
            MetricRangePreferences rangePreferences = preferences.getMetricRangePreferences();

            // adjust down so the start time of the first point equals the begin time of the metric display range prefs
            long adjust = (rangePreferences.end - rangePreferences.begin) / DefaultConstants.DEFAULT_CHART_POINTS;

            if (context.category == EntityContext.Category.Resource) {
                data = availabilityManager.findAvailabilitiesForResource(user.getSubject(), context.resourceId,
                    rangePreferences.begin - adjust, rangePreferences.end - adjust,
                    DefaultConstants.DEFAULT_CHART_POINTS, !rangePreferences.readOnly);
            } else if (context.category == EntityContext.Category.ResourceGroup) {
                data = availabilityManager.findAvailabilitiesForResourceGroup(user.getSubject(), context.groupId,
                    rangePreferences.begin - adjust, rangePreferences.end - adjust,
                    DefaultConstants.DEFAULT_CHART_POINTS, !rangePreferences.readOnly);
            } else if (context.category == EntityContext.Category.AutoGroup) {
                data = availabilityManager.findAvailabilitiesForAutoGroup(user.getSubject(), context.parentResourceId,
                    context.resourceTypeId, rangePreferences.begin - adjust, rangePreferences.end - adjust,
                    DefaultConstants.DEFAULT_CHART_POINTS, !rangePreferences.readOnly);
            }

            if (data != null) {
                percentage = getFormattedAvailability(data);
            }
        } catch (Exception e) {
            log.info("Error while looking up availability data for " + context);
        }
    }

    protected String getFormattedAvailability(List<AvailabilityPoint> values) {
        double sum = 0;
        int count = 0;

        for (AvailabilityPoint ap : values) {
            if (ap.isKnown()) {
                count++;
                if (ap.getAvailabilityType() == AvailabilityType.UP) {
                    sum++;
                }
            }
        }

        // by the logic above, if sum is zero then count is also zero
        // so, shortcut the result as 0 if this is the case, otherwise result sum / count
        double result = ((sum == 0) ? 0 : (sum / count));

        UnitNumber average = new UnitNumber(result, UnitsConstants.UNIT_PERCENTAGE);
        return UnitsFormat.format(average).toString();
    }
}
