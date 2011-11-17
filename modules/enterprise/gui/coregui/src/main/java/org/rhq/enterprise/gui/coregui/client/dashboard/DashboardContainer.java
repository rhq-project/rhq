package org.rhq.enterprise.gui.coregui.client.dashboard;

import java.util.Set;

import org.rhq.core.domain.authz.Permission;
import org.rhq.enterprise.gui.coregui.client.RefreshableView;

public interface DashboardContainer extends RefreshableView {

    public Set<Permission> getGlobalPermissions();

    public boolean supportsDashboardNameEdit();

    public void updateDashboardNames();

    public boolean isValidDashboardName(String name);

}
