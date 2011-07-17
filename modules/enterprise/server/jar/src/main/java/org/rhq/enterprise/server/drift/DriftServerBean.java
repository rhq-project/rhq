package org.rhq.enterprise.server.drift;

import java.io.File;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.enterprise.server.plugin.pc.MasterServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.drift.DriftServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.drift.DriftServerPluginFacet;
import org.rhq.enterprise.server.plugin.pc.drift.DriftServerPluginManager;
import org.rhq.enterprise.server.util.LookupUtil;

import static javax.ejb.TransactionAttributeType.NOT_SUPPORTED;

@Stateless
public class DriftServerBean implements DriftServerLocal {

    private Log log = LogFactory.getLog(DriftServerBean.class);

    @Override
    @TransactionAttribute(NOT_SUPPORTED)
    public void saveChangeSet(int resourceId, File changeSetZip) throws Exception {
        DriftServerPluginFacet driftServerPlugin = getServerPlugin();
        driftServerPlugin.saveChangeSet(resourceId, changeSetZip);
    }

    @Override
    @TransactionAttribute(NOT_SUPPORTED)
    public void saveChangeSetFiles(File changeSetFilesZip) throws Exception {
        DriftServerPluginFacet driftServerPlugin = getServerPlugin();
        driftServerPlugin.saveChangeSetFiles(changeSetFilesZip);
    }

    DriftServerPluginFacet getServerPlugin() {
        MasterServerPluginContainer masterPC = LookupUtil.getServerPluginService().getMasterPluginContainer();
        if (masterPC == null) {
            log.warn(MasterServerPluginContainer.class.getSimpleName() + " is not started yet");
            return null;
        }

        DriftServerPluginContainer pc = masterPC.getPluginContainerByClass(DriftServerPluginContainer.class);
        if (pc == null) {
            log.warn(DriftServerPluginContainer.class + " has not been loaded by the " + masterPC.getClass() + " yet");
            return null;
        }

        DriftServerPluginManager pluginMgr = (DriftServerPluginManager) pc.getPluginManager();

        return pluginMgr.getDriftServerPluginComponent();
    }

}
