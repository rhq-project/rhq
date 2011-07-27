package org.rhq.augeas;

import org.rhq.augeas.config.AugeasConfiguration;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.augeas.tree.AugeasTreeBuilder;

public abstract class AugeasComponent {

    AugeasProxy augeasProxy;
    boolean isClosed = true;

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
        augeasProxy.close();
    }

    public AugeasConfiguration getConfiguration() {
        return augeasProxy.getConfiguration();
    }

}
