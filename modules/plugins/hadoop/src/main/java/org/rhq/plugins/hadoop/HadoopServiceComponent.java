
package org.rhq.plugins.hadoop;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;

import org.rhq.core.domain.configuration.Configuration;
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
import org.rhq.plugins.jmx.JMXComponent;
import org.rhq.plugins.jmx.JMXServerComponent;

public class HadoopServiceComponent extends JMXServerComponent implements JMXComponent,  ResourceComponent
, MeasurementFacet
, OperationFacet
{
    private final Log log = LogFactory.getLog(this.getClass());

    private ResourceContext context;




    /**
     * Return availability of this resource
     *  @see org.rhq.core.pluginapi.inventory.ResourceComponent#getAvailability()
     */
    public AvailabilityType getAvailability() {
        return context.getNativeProcess().isRunning() ? AvailabilityType.UP: AvailabilityType.DOWN;
    }


    /**
     * Start the resource connection
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#start(org.rhq.core.pluginapi.inventory.ResourceContext)
     */
    public void start(ResourceContext context) throws InvalidPluginConfigurationException, Exception {

        Configuration conf = context.getPluginConfiguration();
        this.context = context;
        super.start(context);
        log.info("Started " + context.getResourceKey());

    }


    /**
     * Tear down the resource connection
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#stop()
     */
    public void stop() {
        super.stop();

    }

    @Override
    public EmsConnection getEmsConnection() {
        EmsConnection conn =  super.getEmsConnection();    // TODO: Customise this generated block
        log.info("EmsConnection is " + conn.toString());
        return conn;

    }

    /**
     * Gather measurement data
     *  @see org.rhq.core.pluginapi.measurement.MeasurementFacet#getValues(org.rhq.core.domain.measurement.MeasurementReport, java.util.Set)
     */
    public  void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {

         for (MeasurementScheduleRequest req : metrics) {
             String property=req.getName();
             String props[] = property.split("\\|");

             EmsConnection conn = getEmsConnection();
             EmsBean bean = conn.getBean(props[0]);
             if (bean != null) {
                 bean.refreshAttributes();
                 EmsAttribute att = bean.getAttribute(props[1]);
                 if (att!=null) {
                     Long val = (Long) att.getValue(); // TODO check for real type

                     MeasurementDataNumeric res = new MeasurementDataNumeric(req, Double.valueOf(val));
                     report.addData(res);
                 }
                 else
                     log.warn("Attribute " + props[1] + " not found");
             }
             else
                 log.warn("MBean " + props[0] +" not found");
         }
    }



    public void startOperationFacet(OperationContext context) {

    }


    /**
     * Invokes the passed operation on the managed resource
     * @param name Name of the operation
     * @param params The method parameters
     * @return An operation result
     * @see org.rhq.core.pluginapi.operation.OperationFacet
     */
    public OperationResult invokeOperation(String name, Configuration params) throws Exception {

        OperationResult res = new OperationResult();
        if ("dummyOperation".equals(name)) {
            // TODO implement me

        }
        return res;
    }





}
