package org.rhq.enterprise.server.bundle;

import org.rhq.enterprise.server.plugin.pc.MasterServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.ServerPluginServiceManagement;
import org.rhq.enterprise.server.plugin.pc.bundle.BundleServerPluginContainer;
import org.rhq.enterprise.server.util.LookupUtil;

public class BundleManagerHelper {

    public static BundleServerPluginContainer getPluginContainer() {
        BundleServerPluginContainer pc;

        try {
            ServerPluginServiceManagement mbean = LookupUtil.getServerPluginService();
            if (!mbean.isMasterPluginContainerStarted()) {
                throw new IllegalStateException("The master plugin container is not started!");
            }

            MasterServerPluginContainer master = mbean.getMasterPluginContainer();
            pc = master.getPluginContainerByClass(BundleServerPluginContainer.class);
        } catch (IllegalStateException ise) {
            throw ise;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot obtain the Bundle plugin container!", e);
        }

        if (pc == null) {
            throw new IllegalStateException("Bundle plugin container is null!");
        }

        return pc;
    }

}
