
package org.rhq.modules.plugins.openshift;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.mapping.Component;

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
import org.rhq.core.pluginapi.operation.OperationContext;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.modules.plugins.jbossas7.ASConnection;
import org.rhq.modules.plugins.jbossas7.BaseComponent;
import org.rhq.modules.plugins.jbossas7.StandaloneASComponent;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.ComplexResult;
import org.rhq.modules.plugins.jbossas7.json.ReadAttribute;

@SuppressWarnings("unused")
public class OpenshiftComponent extends StandaloneASComponent<BaseComponent<?>> implements MeasurementFacet
{
    private final Log log = LogFactory.getLog(this.getClass());

    private static final int CHANGEME = 1; // TODO remove or change this

    private ResourceContext context;
    private StandaloneASComponent parent;

    /**
     * Return availability of this resource
     *  @see org.rhq.core.pluginapi.inventory.ResourceComponent#getAvailability()
     */
    public AvailabilityType getAvailability() {
        // TODO supply real implementation
        return AvailabilityType.UP;
    }


    /**
     * Start the resource connection
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#start(org.rhq.core.pluginapi.inventory.ResourceContext)
     */
    public void start(ResourceContext context) throws InvalidPluginConfigurationException, Exception {

        this.context = context;
        Configuration conf = context.getPluginConfiguration();
        parent = (StandaloneASComponent) context.getParentResourceComponent();
        // TODO add code to start the resource / connection to it


    }


    /**
     * Tear down the resource connection
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#stop()
     */
    public void stop() {


    }



    /**
     * Gather measurement data
     *  @see org.rhq.core.pluginapi.measurement.MeasurementFacet#getValues(org.rhq.core.domain.measurement.MeasurementReport, java.util.Set)
     */
    @Override
    public  void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {

         for (MeasurementScheduleRequest req : metrics) {
            if (req.getName().equals("appuid")) {

                Address addr = new Address("core-service=platform-mbean,type=runtime");
                ReadAttribute op = new ReadAttribute(addr,"system-properties");
                ASConnection conn = parent.getASConnection();
                ComplexResult result = conn.executeComplex(op);

                if (result.isSuccess()) {
                    Map<String,Object> data = result.getResult();
                    if (data.containsKey("OPENSHIFT_APP_UUID")) {
                        String uid = (String) data.get("OPENSHIFT_APP_UUID");
                        MeasurementDataTrait res = new MeasurementDataTrait(req,uid);
                 report.addData(res);
                    }
                }
                else {
                    log.warn("Operation failed: " + result.getFailureDescription());
                }
            }
            // TODO add more metrics here
         }
    }


}
