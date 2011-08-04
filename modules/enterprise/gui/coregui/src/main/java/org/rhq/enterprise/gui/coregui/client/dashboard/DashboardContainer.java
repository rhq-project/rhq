package org.rhq.enterprise.gui.coregui.client.dashboard;

import java.util.Set;

import org.rhq.core.domain.authz.Permission;

public interface DashboardContainer {

    public Set<Permission> getGlobalPermissions();

    public boolean supportsDashboardNameEdit();

    public void updateDashboardNames();

}
