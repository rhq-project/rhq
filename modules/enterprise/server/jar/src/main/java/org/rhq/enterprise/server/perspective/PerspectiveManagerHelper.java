package org.rhq.enterprise.server.perspective;

import org.rhq.enterprise.server.plugin.pc.MasterServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.ServerPluginServiceManagement;
import org.rhq.enterprise.server.plugin.pc.perspective.PerspectiveServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.perspective.PerspectiveServerPluginManager;
import org.rhq.enterprise.server.plugin.pc.perspective.metadata.PerspectivePluginMetadataManager;
import org.rhq.enterprise.server.util.LookupUtil;

public class PerspectiveManagerHelper {

    public static PerspectiveServerPluginContainer getPluginContainer() throws Exception {
        PerspectiveServerPluginContainer pc = null;

        try {
            ServerPluginServiceManagement mbean = LookupUtil.getServerPluginService();
            if (!mbean.isMasterPluginContainerStarted()) {
                throw new IllegalStateException("The master plugin container is not started!");
            }

            MasterServerPluginContainer master = mbean.getMasterPluginContainer();
            pc = master.getPluginContainerByClass(PerspectiveServerPluginContainer.class);
        } catch (IllegalStateException ise) {
            throw ise;
        } catch (Exception e) {
            throw new Exception("Cannot obtain the Perspective plugin container", e);
        }

        if (pc == null) {
            throw new Exception("Perspective plugin container is null!");
        }

        return pc;
    }

    public static PerspectivePluginMetadataManager getPluginMetadataManager() throws Exception {
        PerspectiveServerPluginManager manager = (PerspectiveServerPluginManager) getPluginContainer()
            .getPluginManager();
        return manager.getMetadataManager();
    }

}
