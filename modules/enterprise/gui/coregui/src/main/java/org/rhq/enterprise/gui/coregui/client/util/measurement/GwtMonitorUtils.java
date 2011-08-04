package org.rhq.enterprise.gui.coregui.client.util.measurement;

import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementUnits;

/** An RPC gwt serializable implemenation of parts of MonitorUtils
 *  utility.
 *
 * @author Joseph Marques
 * @author Simeon Pinder
 */
public class GwtMonitorUtils {
    public static final String RO = "ro";
    public static final String LASTN = "lastN";
    public static final String UNIT = "unit";
    public static final String BEGIN = "begin";
    public static final String END = "end";

    public static final int DEFAULT_CURRENTHEALTH_LASTN = 8;

    public static final int THRESHOLD_BASELINE_VALUE = 1;
    public static final String THRESHOLD_BASELINE_LABEL = "Baseline";
    public static final int THRESHOLD_HIGH_RANGE_VALUE = 2;
    public static final String THRESHOLD_HIGH_RANGE_LABEL = "HighRange";
    public static final int THRESHOLD_LOW_RANGE_VALUE = 3;
    public static final String THRESHOLD_LOW_RANGE_LABEL = "LowRange";

    public static final int THRESHOLD_UNDER_VALUE = 1;
    public static final int THRESHOLD_OVER_VALUE = 2;

    /**
     * Formats the passed summary. The userLocale is currently ignored
     * @param summary  MetricDisplaySummary with some values
     * @param userLocale ignored
     */
    public static String[] formatSimpleMetrics(double[] summary, MeasurementDefinition md) {
        String units = md.getUnits().getName();
        if (units.length() < 1) {
            units = MeasurementUnits.NONE.name();
        }

        String[] formattedValues = GwtMeasurementConverter.formatToSignificantPrecision(summary, MeasurementUnits
            .valueOf(units), true);

        return formattedValues;
    }
}
