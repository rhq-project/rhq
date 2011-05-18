
package org.rhq.plugins.pattern;

import java.util.Date;
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
public class PatternComponent implements ResourceComponent, MeasurementFacet
{
    private final Log log = LogFactory.getLog(this.getClass());

    int count = 0;
    int traitCount = 0;
    int number = 0; // We start with returning zeros
    int numerForTrait = 0;
    int[] wanted = new int[2];

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
        String tmp = conf.getSimpleValue("ones","1");
        int wantedOnes = Integer.parseInt(tmp);
        if (wantedOnes<1)
            throw new InvalidPluginConfigurationException("Ones must be > 0");
        tmp = conf.getSimpleValue("zeros","1");
        int wantedZeros = Integer.parseInt(tmp);
        if (wantedZeros<1)
            throw new InvalidPluginConfigurationException("Zeros must be > 0");

        wanted[0] = wantedZeros;
        wanted[1] = wantedOnes;
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
    public  void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {

         for (MeasurementScheduleRequest req : metrics) {
            if (req.getName().equals("pattern1")) {

                double val;

                val = (double) number;

                count++;
                // enough of this? Flip over to the other number and reset the series.
                if (count>=wanted[number]) {
                    number = 1-number;
                    count=0;
                }

                MeasurementDataNumeric res = new MeasurementDataNumeric(req, val);
                report.addData(res);
            }
             else if (req.getName().equals("text1")) {

                String trait = "Trait_" + number;
                traitCount++;
                // enough of this? Flip over to the other number and reset the series.
                if (traitCount>=wanted[number]) {
                    number = 1-number;
                    traitCount=0;
                }

                MeasurementDataTrait res = new MeasurementDataTrait(req,trait);
                report.addData(res);
            }
         }
    }
}
