package org.rhq.augeas;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.augeas.config.AugeasConfiguration;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.augeas.tree.AugeasTreeBuilder;

public abstract class AugeasComponent {

    AugeasProxy augeasProxy;
    boolean isClosed = true;
    private final Log log = LogFactory.getLog(this.getClass());

    public abstract AugeasConfiguration initConfiguration();

    public abstract AugeasTreeBuilder initTreeBuilder();

    protected void reloadAugeas() {
        if (!isClosed)
            close();

        augeasProxy = new AugeasProxy(initConfiguration(), initTreeBuilder());
        augeasProxy.load();
        isClosed = false;
    }

    public AugeasTree getAugeasTree(String moduleName) {
        reloadAugeas();
        return augeasProxy.getAugeasTree(moduleName, true);
    }

    public void close() {
        isClosed = true;
        if (augeasProxy != null) {
            try {
                augeasProxy.close();
            } catch (Exception e) {
                log.error("Could not close augeas instance", e);
            }
        }
    }

    public AugeasConfiguration getConfiguration() {
        if (augeasProxy == null)
            throw new RuntimeException("Could not provide augeas configuration because augeas was not initialized yet.");

        return augeasProxy.getConfiguration();
    }

}
