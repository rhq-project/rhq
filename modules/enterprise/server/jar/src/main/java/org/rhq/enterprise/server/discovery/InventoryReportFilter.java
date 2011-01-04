package org.rhq.enterprise.server.discovery;

import org.rhq.core.clientapi.server.discovery.InventoryReport;

public interface InventoryReportFilter {

    boolean accept(InventoryReport report);

}
