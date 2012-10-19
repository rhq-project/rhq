package org.jboss.on.plugins.tomcat.helper;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;

public class CreateResourceHelper {

    public static void setErrorOnReport(CreateResourceReport report, String message) {
        setErrorOnReport(report, message, null);
    }

    public static void setErrorOnReport(CreateResourceReport report, Exception e) {
        setErrorOnReport(report, null, e);
    }

    public static void setErrorOnReport(CreateResourceReport report, String message, Exception e) {
        report.setStatus(CreateResourceStatus.FAILURE);
        report.setErrorMessage(message);
        report.setException(e);
    }

    public static String getCanonicalName(String objectName) {
        ObjectName on;
        try {
            on = new ObjectName(objectName);
        } catch (MalformedObjectNameException e) {
            throw new IllegalStateException("Malformed JMX object name: " + objectName + " - " + e.getLocalizedMessage());
        }
        return on.getCanonicalName();
    }

    public static void setResourceName(CreateResourceReport report, String baseName) {
        String resourceName;
        if (report.getUserSpecifiedResourceName() != null) {
            resourceName = report.getUserSpecifiedResourceName();
        } else {
            resourceName = report.getResourceType().getName() + " (" + baseName + ")";
        }
        report.setResourceName(resourceName);
    }

}
