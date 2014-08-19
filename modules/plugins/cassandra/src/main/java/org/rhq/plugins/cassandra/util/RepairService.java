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
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.util.exception.ThrowableUtil;

/**
 * @author John Sanda
 */
public class RepairService {

    private static final Log log = LogFactory.getLog(RepairService.class);

    public static final String STORAGE_SERVICE_BEAN = "org.apache.cassandra.db:type=StorageService";

    public static final String REPAIR = "forceTableRepair";

    public static final String REPAIR_PRIMARY_RANGE = "forceTableRepairPrimaryRange";

    public static final String REPAIR_RANGE = "forceTableRepairRange";

    private String jmxURL;

    public RepairService(String jmxURL) {
        this.jmxURL = jmxURL;
    }

    public OperationResult repairPrimaryRange(String keyspace, boolean useSnapshot, String... tables) {
        OperationResult result = createRepairResult(keyspace, tables);
        MBeanServerConnection serverConnection = null;
        ObjectName objectName = null;
        RepairListener listener = new RepairListener();

        try {
            JMXServiceURL serviceURL = new JMXServiceURL(jmxURL);
            JMXConnector connector = JMXConnectorFactory.connect(serviceURL, null);
            serverConnection = connector.getMBeanServerConnection();
            objectName = new ObjectName(STORAGE_SERVICE_BEAN);
            serverConnection.addNotificationListener(objectName, listener, null, null);
            serverConnection.invoke(objectName, REPAIR_PRIMARY_RANGE, new Object[] {keyspace, useSnapshot, true,
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

    public OperationResult repair(String keyspace, boolean useSnapshot, String... tables) {
        OperationResult result = createRepairResult(keyspace, tables);
        MBeanServerConnection serverConnection = null;
        ObjectName objectName = null;
        RepairListener listener = new RepairListener();

        try {
            JMXServiceURL serviceURL = new JMXServiceURL(jmxURL);
            JMXConnector connector = JMXConnectorFactory.connect(serviceURL, null);
            serverConnection = connector.getMBeanServerConnection();
            objectName = new ObjectName(STORAGE_SERVICE_BEAN);
            serverConnection.addNotificationListener(objectName, listener, null, null);
            serverConnection.invoke(objectName, REPAIR, new Object[] {keyspace, useSnapshot, true,
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

    public OperationResult repairRange(String keyspace, boolean useSnapshot, String startToken, String endToken,
        String... tables) {
        OperationResult result = createRepairResult(keyspace, tables);
        MBeanServerConnection serverConnection = null;
        ObjectName objectName = null;
        RepairListener listener = new RepairListener();

        try {
            JMXServiceURL serviceURL = new JMXServiceURL(jmxURL);
            JMXConnector connector = JMXConnectorFactory.connect(serviceURL, null);
            serverConnection = connector.getMBeanServerConnection();
            objectName = new ObjectName(STORAGE_SERVICE_BEAN);
            serverConnection.addNotificationListener(objectName, listener, null, null);
            serverConnection.invoke(objectName, REPAIR_RANGE, new Object[] {startToken, endToken, keyspace, useSnapshot,
                true, tables}, new String[] {String.class.getName(), String.class.getName(), String.class.getName(),
                boolean.class.getName(), boolean.class.getName(), String[].class.getName()});
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

    private OperationResult createRepairResult(String keyspace, String... tables) {
        OperationResult result = new OperationResult();
        result.getComplexResults().put(new PropertySimple("keyspace", keyspace));
        PropertyList list = new PropertyList("tables");
        for (String table : tables) {
            list.add(new PropertySimple("table", table));
        }
        result.getComplexResults().put(list);

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
                log.info(message);

                int index = message.indexOf("range (");
                int start = index + "range (".length();
                int end = message.indexOf("]");
                if (index == -1 || end == -1) {
                    log.warn("Cannot parse range from [" + message + "]");
                } else {
                    String[] range = message.substring(start, end).split(",");
                    if (range.length == 2) {
                        failedSessions.add(newRangeMap(range[0], range[1]));
                    } else {
                        log.warn("Cannot parse range start/end from [" + message + "]");
                    }
                }
            }
        }
    }

    private PropertyMap newRangeMap(String start, String end) {
        PropertyMap map = new PropertyMap("range");
        map.put(new PropertySimple("start", start));
        map.put(new PropertySimple("end", end));
        return map;
    }

}
