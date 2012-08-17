package org.rhq.plugins.pattern;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;

@SuppressWarnings("unused")
public class PatternComponent implements ResourceComponent, MeasurementFacet {

    private final Log log = LogFactory.getLog(this.getClass());

    int count = 0;
    int traitCount = 0;

    int number = 0; // We start with returning zeros
    int numberForTrait = 0;

    int[] wanted = new int[2];

    long delay = 0L; // in seconds

    /**
     * Return availability of this resource
     *  @see org.rhq.core.pluginapi.inventory.ResourceComponent#getAvailability()
     */
    public AvailabilityType getAvailability() {
        return AvailabilityType.UP;
    }

    /**
     * Start the resource connection
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#start(org.rhq.core.pluginapi.inventory.ResourceContext)
     */
    public void start(ResourceContext context) throws InvalidPluginConfigurationException {

        Configuration conf = context.getPluginConfiguration();
        String tmp = conf.getSimpleValue("ones", "1");
        int wantedOnes = Integer.parseInt(tmp);
        if (wantedOnes < 1)
            throw new InvalidPluginConfigurationException("Ones must be > 0");
        tmp = conf.getSimpleValue("zeros", "1");
        int wantedZeros = Integer.parseInt(tmp);
        if (wantedZeros < 1)
            throw new InvalidPluginConfigurationException("Zeros must be > 0");

        wanted[0] = wantedZeros;
        wanted[1] = wantedOnes;

        tmp = conf.getSimpleValue("delay", "0");
        delay = Long.parseLong(tmp);
        if (delay < 0L)
            delay = 0L;
    }

    /**
     * Tear down the resource connection
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#stop()
     */
    public void stop() {
        // Nothing to do.
    }

    /**
     * Gather "measurement" data - actually a series of 1s and 0s starting with 0.
     *  @see org.rhq.core.pluginapi.measurement.MeasurementFacet#getValues(org.rhq.core.domain.measurement.MeasurementReport, java.util.Set)
     */
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) throws Exception {

        if (delay > 0L) {
            Thread.sleep(delay * 1000L);
        }

        boolean flipMetrics = false;
        boolean flipTraits = false;
        for (MeasurementScheduleRequest request : requests) {
            String metricName = request.getName();

            if (metricName.startsWith("pattern")) {

                double value;
                if (metricName.equals("pattern1")) {
                    value = number;
                    flipMetrics = true;

                } else {
                    // pattern2
                    value = 1 - number;
                }

                MeasurementDataNumeric datum = new MeasurementDataNumeric(request, value);
                report.addData(datum);
            } else if (metricName.startsWith("text")) {

                double value;
                if (metricName.equals("text1")) {
                    value = numberForTrait;
                    flipTraits = true;
                } else {
                    // text2
                    value = 1 - numberForTrait;
                }
                String traitValue = (value == 0) ? "red" : "green";

                MeasurementDataTrait datum = new MeasurementDataTrait(request, traitValue);
                report.addData(datum);
            }
        }
        if (flipMetrics) {
            count++;
            if (count >= wanted[number]) {
                // Flip over to the other number and reset the series.
                number = 1 - number;
                count = 0;
            }
        }
        if (flipTraits) {
            traitCount++;
            if (traitCount >= wanted[numberForTrait]) {
                // Flip over to the other number and reset the series.
                numberForTrait = 1 - numberForTrait;
                traitCount = 0;
            }
        }
    }

}
