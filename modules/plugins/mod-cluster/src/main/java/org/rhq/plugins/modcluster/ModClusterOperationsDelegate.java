package org.rhq.plugins.modcluster;

import org.rhq.plugins.modcluster.ModClusterServerComponent.SupportedOperations;

import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.operation.EmsOperation;

import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;

import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.domain.configuration.Configuration;

import org.rhq.plugins.jmx.MBeanResourceComponent;

/**
 * @author Maxime Beck
 */

public class ModClusterOperationsDelegate {

    private static final String STORE_CONFIG_MBEAN_NAME = "Catalina:type=StoreConfig";
    private static final String MOD_CLUSTER_MBEAN_NAME = "Catalina:type=ModClusterListener";

    private MBeanResourceComponent resourceComponent;

    public ModClusterOperationsDelegate(MBeanResourceComponent resourceComponent) {
        this.resourceComponent = resourceComponent;
    }

    public OperationResult invoke(SupportedOperations operation, Configuration parameters) throws InterruptedException {
        String message = null;

        switch (operation) {
            case STORECONFIG:
                message = storeConfig();
                break;

            case PROXY_INFO_STRING:
                message = getProxyInfoString();
                break;
        }

        OperationResult result = new OperationResult(message);
        return result;
    }

    private String storeConfig() {
        EmsConnection connection = this.resourceComponent.getEmsConnection();
        if (connection == null) {
            throw new RuntimeException("Can not connect to the MBean Server");
        }
        EmsBean bean = connection.getBean(STORE_CONFIG_MBEAN_NAME);

        EmsOperation operation = bean.getOperation("storeConfig");
        operation.invoke(new Object[0]);

        return ("Mod Cluster configuration updated.");
    }

    private String getProxyInfoString() {
        EmsConnection connection = this.resourceComponent.getEmsConnection();
        if (connection == null) {
            throw new RuntimeException("Can not connect to the MBean Server");
        }
        EmsBean bean = connection.getBean(MOD_CLUSTER_MBEAN_NAME);

        EmsOperation operation = bean.getOperation("proxyInfoString");
        operation.invoke(new Object[0]);

        return ("Proxy Info has been invoked");
    }
}
