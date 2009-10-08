package org.rhq.enterprise.server.measurement.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.enterprise.server.measurement.uibean.MetricDisplaySummary;

public class MeasurementUtils {

    private static final Log LOG = LogFactory.getLog(MeasurementUtils.class);

    public static final int UNIT_COLLECTION_POINTS = 1;
    public static final int UNIT_MINUTES = 2;
    public static final int UNIT_HOURS = 3;
    public static final int UNIT_DAYS = 4;
    public static final int UNIT_WEEKS = 5;

    /**
     * Method calculateTimeFrame
     * <p/>
     * Returns a two element<code>List</code> of <code>Long</code> objects representing the begin and end times (in
     * milliseconds since the epoch) of the timeframe. Returns null instead if the time unit is indicated as
     * <code>UNIT_COLLECTION_POINTS</code>.
     *
     * @param lastN the number of time units in the time frame
     * @param unit  the unit of time (as defined by <code>UNIT_*</code> constants
     * @return List
     */
    public static List<Long> calculateTimeFrame(int lastN, int unit) {
        List<Long> l = new ArrayList<Long>(0);

        if (unit == UNIT_COLLECTION_POINTS) {
            return null;
        }

        long now = System.currentTimeMillis();

        long retrospective = lastN;
        switch (unit) {
        case UNIT_WEEKS:
            retrospective *= NumberConstants.WEEKS;
            break;
        case UNIT_MINUTES:
            retrospective *= NumberConstants.MINUTES;
            break;
        case UNIT_HOURS:
            retrospective *= NumberConstants.HOURS;
            break;
        case UNIT_DAYS:
            retrospective *= NumberConstants.DAYS;
            break;
        default:
            retrospective = -1;
            break;
        }

        l.add(now - retrospective);
        l.add(now);

        return l;
    }

    /**
     * Parse the passed token that identifies single metric or group.
     * The format of the token is (without quotation marks):
     * <ul>
     * <li>For a compatible group: "cg,<i>groupId</i>,<i>definitionId</i>"</li>
     * <li>For an autogroup : "ag,<i>parentId</i>,<i>definitionId</i>,<i>childTypeId</i>"</li>
     * <li>For a single resource: "<i>resourceId</i>,<i>scheduleId</i>"</li>
     * </ul>
     * @param token A token that follows the form mentioned above.
     * @return a new {@link MetricDisplaySummary} where the identifiers for resource/group have been set.
     * @see #getContextKeyChart(MetricDisplaySummary)
     */
    public static MetricDisplaySummary parseMetricToken(String token) {
        String DELIMITER = ",";
        if (LOG.isTraceEnabled()) {
            LOG.trace("parseMetricToken: input is " + token);
        }

        MetricDisplaySummary ret = new MetricDisplaySummary();

        String[] tokens = token.split(DELIMITER);
        if (tokens == null || tokens.length < 2) {
            throw new IllegalArgumentException(token + " is not valid");
        }

        if (tokens[0].equals("cg")) {
            ret.setGroupId(Integer.parseInt(tokens[1]));
            ret.setDefinitionId(Integer.parseInt(tokens[2]));
        } else if (tokens[0].equals("ag")) {
            ret.setParentId(Integer.parseInt(tokens[1]));
            ret.setDefinitionId(Integer.parseInt(tokens[2]));
            ret.setChildTypeId(Integer.parseInt(tokens[3]));
        } else {
            ret.setResourceId(Integer.parseInt(tokens[0]));
            ret.setScheduleId(Integer.parseInt(tokens[1]));
        }
        ret.setMetricToken(token);
        return ret;
    }
}
