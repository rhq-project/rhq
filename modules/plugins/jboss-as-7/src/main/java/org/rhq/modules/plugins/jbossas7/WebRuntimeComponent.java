package org.rhq.modules.plugins.jbossas7;

import java.io.File;
import java.util.Iterator;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.calltime.CallTimeData;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.util.ResponseTimeConfiguration;
import org.rhq.core.pluginapi.util.ResponseTimeLogParser;
import org.rhq.modules.plugins.jbossas7.helper.ServerPluginConfiguration;
import org.rhq.modules.plugins.jbossas7.json.ReadAttribute;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * The ResourceComponent for a "Web Runtime" Resource.
 *
 * @author Ian Springer
 */
public class WebRuntimeComponent extends BaseComponent<BaseComponent<?>> {

    private static final String RESPONSE_TIME_METRIC = "responseTime";

    private ResponseTimeLogParser responseTimeLogParser;

    @Override
    public void start(ResourceContext<BaseComponent<?>> resourceContext) throws InvalidPluginConfigurationException, Exception {
        super.start(resourceContext);

        Configuration pluginConfig = resourceContext.getPluginConfiguration();
        ResponseTimeConfiguration responseTimeConfig = new ResponseTimeConfiguration(pluginConfig);
        File logFile = responseTimeConfig.getLogFile();
        if (logFile == null) {
            logFile = findLogFile();
        }

        if (logFile != null) {
            this.responseTimeLogParser = new ResponseTimeLogParser(logFile);
            this.responseTimeLogParser.setExcludes(responseTimeConfig.getExcludes());
            this.responseTimeLogParser.setTransforms(responseTimeConfig.getTransforms());
        }
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) throws Exception {
        for (Iterator<MeasurementScheduleRequest> iterator = requests.iterator(); iterator.hasNext(); ) {
            MeasurementScheduleRequest request = iterator.next();
            if (request.getName().equals(RESPONSE_TIME_METRIC)) {
                iterator.remove();
                if (this.responseTimeLogParser != null) {
                    try {
                        CallTimeData callTimeData = new CallTimeData(request);
                        this.responseTimeLogParser.parseLog(callTimeData);
                        report.addData(callTimeData);
                    } catch (Exception e) {
                        log.error("Failed to retrieve call-time metric '" + RESPONSE_TIME_METRIC + "' for "
                                + context.getResourceType() + " Resource with key [" + context.getResourceKey() + "].",
                                e);
                    }
                } else {
                    log.error("The '" + RESPONSE_TIME_METRIC + "' metric is enabled for " + context.getResourceType()
                            + " Resource with key [" + context.getResourceKey() + "], but no value is defined for the '"
                            + ResponseTimeConfiguration.RESPONSE_TIME_LOG_FILE_CONFIG_PROP + "' connection property.");
                    // TODO: Communicate this error back to the server for display in the GUI.
                }
                break;
            }
        }

        super.getValues(report, requests);
    }

    private File findLogFile() {
        File logFile = null;
        ServerPluginConfiguration serverPluginConfig = getServerComponent().getServerPluginConfiguration();
        File logDir = serverPluginConfig.getLogDir();
        if (logDir != null && logDir.isDirectory()) {
            String virtualHost = readAttribute("virtual-host");
            if (virtualHost != null) {
                String contextRoot = readAttribute("context-root");
                if (contextRoot != null) {
                    // e.g. "192.168.1.100_foo_rt.log" for foo.war deployed to 192.168.1.100 vhost
                    String logFileName = String.format("%s_%s_rt.log", virtualHost, contextRoot);
                    logFile = new File(logDir, logFileName);
                }
            }
        }
        return logFile;
    }

    // TODO: Make this more robust and move it up to BaseComponent.
    private String readAttribute(String attributeName) {
        ASConnection connection = getASConnection();
        ReadAttribute op = new ReadAttribute(address, attributeName);
        Result result = connection.execute(op);
        return result.toString();
    }

}
