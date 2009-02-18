package org.jboss.on.plugins.tomcat.helper;

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

}
