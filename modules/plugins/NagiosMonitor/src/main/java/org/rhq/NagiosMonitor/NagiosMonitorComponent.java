package org.rhq.NagiosMonitor;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;


public class NagiosMonitorComponent implements ResourceComponent, MeasurementFacet
{
	private final Log log = LogFactory.getLog(this.getClass());
    
    private final String NAGIOSIP = "127.0.0.1";
    private final int NAGIOSPORT = 6557;
        
    private NetworkConnection myConnection;
    
     /**
     * Return availability of this resource
     *  @see org.rhq.core.pluginapi.inventory.ResourceComponent#getAvailability()
     */
    public AvailabilityType getAvailability() 
    {
        // TODO supply real implementation
        return AvailabilityType.UP;
    }

    /**
     * Start the resource connection
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#start(org.rhq.core.pluginapi.inventory.ResourceContext)
     */
    public void start(ResourceContext context) throws InvalidPluginConfigurationException, Exception 
    {
        @SuppressWarnings("unused")
		Configuration conf = context.getPluginConfiguration();	
	
        LqlServiceRequest lqlServiceRequest = new LqlServiceRequest();
    	myConnection = new NetworkConnection(NAGIOSIP, NAGIOSPORT, lqlServiceRequest);
			
		log.info("Plugin started");
    }


    /**
     * Tear down the rescource connection
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#stop()
     */
    public void stop() 
    {
    			
    }

    /**
     * Gather measurement data
     *  @see org.rhq.core.pluginapi.measurement.MeasurementFacet#getValues(org.rhq.core.domain.measurement.MeasurementReport, java.util.Set)
     */
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics)
    {
    	log.info("getValues() called at " + System.currentTimeMillis());
    		
		NetworkConnection networkConnection = new NetworkConnection(NAGIOSIP, NAGIOSPORT, new LqlServiceRequest());
		networkConnection.sendAndReceive();
		
		Controller controller = new Controller(networkConnection.getLqlReply());
		
		NagiosData data = controller.createDataModel();
	
		try
		{
			log.info("execution_time: " + data.getSingleMetricForRessource("execution_time", "Current Load").getValue());
			log.info("host_check_period: " + data.getSingleMetricForRessource("host_check_period", "Current Load").getValue());
			log.info("host_execution_time: " + data.getSingleMetricForRessource("host_execution_time", "Current Load").getValue());
			
			for (MeasurementScheduleRequest req : metrics) 
	         {    
				//TODO switch instead of if-else?
				if("execution_time".equals(req.getName()) ) 
				{      	        		
		    		String value = data.getSingleMetricForRessource("execution_time", "Current Load").getValue();
					
					MeasurementDataNumeric res = new MeasurementDataNumeric(req, Double.valueOf(value));
	    			report.addData(res);
				}
				else if("host_check_period".equals(req.getName()) ) 
				{   
					String value = data.getSingleMetricForRessource("host_check_period", "Current Load").getValue();
					MeasurementDataTrait res = new MeasurementDataTrait(req, value);
					report.addData(res);
				}
				else if("host_execution_time".equals(req.getName()) ) 
				{   
					String value = data.getSingleMetricForRessource("host_execution_time", "Current Load").getValue();
					MeasurementDataNumeric res = new MeasurementDataNumeric(req, Double.valueOf(value));
					report.addData(res);
					
				}
	     	}
		}
		catch(InvalidMetricRequestException e)
		{
			log.error(e);
		} 
		catch (InvalidServiceRequestException e)
		{
			log.error(e);
		}
		
		
	}
}