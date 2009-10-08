package org.rhq.plugins.irc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.event.EventContext;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationContext;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;


/**
 * @author Greg Hinkle
 */
public class IRCChannelComponent implements ResourceComponent<IRCServerComponent>, MeasurementFacet, OperationFacet {

    private final Log log = LogFactory.getLog(this.getClass());


    private ResourceContext<IRCServerComponent> context;
    private EventContext eventContext;
    private String channel;

    private AtomicLong messageCount = new AtomicLong();



    /**
     * Return availability of this resource
     *
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#getAvailability()
     */
    public AvailabilityType getAvailability() {
        if (!context.getParentResourceComponent().isInChannel(channel)) {
            context.getParentResourceComponent().registerChannel(this);
        }

        // TODO supply real implementation
        return AvailabilityType.UP;
    }


    /**
     * Start the resource connection
     *
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#start(org.rhq.core.pluginapi.inventory.ResourceContext)
     */
    public void start(ResourceContext<IRCServerComponent> context) throws InvalidPluginConfigurationException, Exception {
        this.context = context;
        Configuration conf = context.getPluginConfiguration();
        // TODO add code to start the resource / connection to it
        channel = conf.getSimple(IRCChannelDiscoveryComponent.CONFIG_CHANNEL).getStringValue();

        eventContext = context.getEventContext();

        context.getParentResourceComponent().registerChannel(this);
    }


    /**
     * Tear down the rescource connection
     *
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#stop()
     */
    public void stop() {

        context.getParentResourceComponent().unregisterChannel(this);

    }


    /**
     * Gather measurement data
     *
     * @see org.rhq.core.pluginapi.measurement.MeasurementFacet#getValues(org.rhq.core.domain.measurement.MeasurementReport, java.util.Set)
     */
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {

        for (MeasurementScheduleRequest req : metrics) {
            if (req.getName().equals("users")) {
                double count = this.context.getParentResourceComponent().getUserCount(this.getChannel());
                MeasurementDataNumeric res = new MeasurementDataNumeric(req, count);
                report.addData(res);
            } else if (req.getName().equals("messages")) {
                report.addData( new MeasurementDataNumeric(req, Double.valueOf(messageCount.get())));
            }
        }
    }



    /**
     * Invokes the passed operation on the managed resource
     *
     * @param name   Name of the operation
     * @param params The method parameters
     * @return An operation result
     * @see org.rhq.core.pluginapi.operation.OperationFacet
     */
    public OperationResult invokeOperation(String name, Configuration params) throws Exception {

        OperationResult res = new OperationResult();
        if ("sendMessage".equals(name)) {
            String message = params.getSimple("message").getStringValue();
            context.getParentResourceComponent().sendMessage(channel, message);
        }
        return res;
    }


    public String getChannel() {
        return channel;
    }

    public void acceptMessage(String sender, String login, String hostname, String message) {
        Event event = new Event("message", sender, System.currentTimeMillis(), EventSeverity.INFO, message);
        this.eventContext.publishEvent(event);

        this.messageCount.incrementAndGet();
    }
}