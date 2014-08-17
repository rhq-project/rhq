package org.rhq.plugins.cassandra.util;

import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.util.exception.ThrowableUtil;

/**
 * @author John Sanda
 */
public class RepairService {

    private static final Log log = LogFactory.getLog(RepairService.class);

    public static final String STORAGE_SERVICE_BEAN = "org.apache.cassandra.db:type=StorageService";

    public static final String REPAIR_OPERATION = "forceTableRepair";

    public static final String REPAIR_PRIMARY_RANGE = "forceTableRepairPrimaryRange";

    private String jmxURL;

    public RepairService(String jmxURL) {
        this.jmxURL = jmxURL;
    }

    public OperationResult repairPrimaryRange(String keyspace, String... tables) {
        OperationResult result = new OperationResult();
        MBeanServerConnection serverConnection = null;
        ObjectName objectName = null;
        RepairListener listener = new RepairListener();

        try {
            JMXServiceURL serviceURL = new JMXServiceURL(jmxURL);
            JMXConnector connector = JMXConnectorFactory.connect(serviceURL, null);
            serverConnection = connector.getMBeanServerConnection();
            objectName = new ObjectName(STORAGE_SERVICE_BEAN);
            serverConnection.addNotificationListener(objectName, listener, null, null);
            serverConnection.invoke(objectName, REPAIR_PRIMARY_RANGE, new Object[] {keyspace, false, true,
                tables}, new String[] {String.class.getName(), boolean.class.getName(),
                boolean.class.getName(), String[].class.getName()});
            result.getComplexResults().put(listener.failedSessions);
        } catch (Exception e) {
            result.setErrorMessage(ThrowableUtil.getStackAsString(e));
        } finally {
            if (!(serverConnection == null || objectName == null)) {
                try {
                    serverConnection.removeNotificationListener(objectName, listener);
                } catch (Exception e) {
                    log.info("An error occurred while removing the repair notification listener", e);
                }
            }
        }
        return result;
    }

    private class RepairListener implements NotificationListener {

        public PropertyList failedSessions;

        public RepairListener() {
            failedSessions = new PropertyList("failedSessions");
        }

        @Override
        public void handleNotification(Notification notification, Object handback) {
            String message = notification.getMessage();

            if (log.isDebugEnabled()) {
                log.debug(message);
            }

            if (message.contains("failed")) {
                int index = message.indexOf("range (");
                int start = index + "range (".length();
                int end = message.indexOf("]");
                if (index == -1 || end == -1) {
                    log.info("Cannot parse range from [" + message + "]");
                } else {
                    String range = message.substring(start, end);
                    failedSessions.add(new PropertySimple("range", range));
                }
            }
        }
    }

}
