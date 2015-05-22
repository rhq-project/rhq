/*
 * RHQ Management Platform
 * Copyright (C) 2005-2015 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.modules.plugins.jbossas7;

import java.io.File;

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryCallback;
import org.rhq.modules.plugins.jbossas7.helper.HostConfiguration;
import org.rhq.modules.plugins.jbossas7.helper.HostPort;
import org.rhq.modules.plugins.jbossas7.helper.ServerPluginConfiguration;

/**
 * Default abstract implementation of {@link ResourceDiscoveryCallback} interface, that may be extended by
 * AS7 based plugins. This implementation handles cases when discovered JBossProduct equals to {@link JBossProduct#unknown()} - this means JBossAS7 plugin
 * does know definition of JBossProduct it detected. The extending class only needs to implement {@link #getProduct()}.
 * @author lzoubek@redhat.com
 *
 */
public abstract class JBossProductDiscoveryCallback implements ResourceDiscoveryCallback {

    /**
     * this implementation only forwards to {@link #handleUnknownProduct(JBossProductContext)}
     */
    @Override
    public DiscoveryCallbackResults discoveredResources(DiscoveredResourceDetails discoveredDetails) throws Exception {
        return handleUnknownProduct(new JBossProductContext(discoveredDetails));
    }

    private HostConfiguration getHostConfiguration(JBossProductContext context) throws Exception {
        File hostConfig = context.getServerConfiguration().getHostConfigFile();
        return new HostConfiguration(hostConfig);
    }

    /**
     * Default implementation handles only {@link JBossProduct#unknown()} (this is determined by {@link #isManagedProduct(JBossProductContext)}).
     * It reads slot value from product.conf and when {@link #getProduct()}'s PRODUCT_SLOT matches the slot (this is determined by {@link #equalsProductSlot(String)}),
     * processes {@link DiscoveredResourceDetails} instance by setting up detected product {@link DiscoveredResourceDetails#getPluginConfiguration()}
     * and setting new resource details (resource name, description and version).
     * 
     * @param context
     * @return
     * @throws Exception
     */
    protected DiscoveryCallbackResults handleUnknownProduct(JBossProductContext context) throws Exception {
        JBossProduct product = getProduct();
        if (product == null) {
            throw new Exception(getClass().getName()
                + " returned null as a result of getProduct() which is not allowed");
        }
        if (isManagedProduct(context)) {
            File homeDir = context.getServerConfiguration().getHomeDir();
            // we need to re-read slot, because JBossProduct instance returned from ServerPluginConfiguration does not know it
            String slot = JBossProductDiscovery.readProductSlot(homeDir);

            if (equalsProductSlot(slot)) {
                context.getServerConfiguration().setProduct(product);
                context.getServerConfiguration().setExpectedRuntimeProductName(product.PRODUCT_NAME);

                if (isUpdateResourceDetails()) {
                    AS7CommandLine commandLine = new AS7CommandLine(context.getDiscoveredDetails().getProcessInfo());
                    HostConfiguration hostConfig = getHostConfiguration(context);
                    HostPort hp = hostConfig.getDomainControllerHostPort(commandLine);
                    HostPort mgmtHp = hostConfig.getManagementHostPort(commandLine, AS7Mode.STANDALONE);

                    // we need this instance to build resource name & description
                    StandaloneASDiscovery discovery = new StandaloneASDiscovery();
                    String name = discovery.buildDefaultResourceName(hp, mgmtHp, product, hostConfig.getHostName());
                    String desc = discovery.buildDefaultResourceDescription(hp, product);
                    String version = discovery.getVersion(homeDir, product);

                    context.getDiscoveredDetails().setResourceName(name);
                    context.getDiscoveredDetails().setResourceDescription(desc);
                    context.getDiscoveredDetails().setResourceVersion(version);
                }
                return DiscoveryCallbackResults.PROCESSED;
            }
        }
        return DiscoveryCallbackResults.UNPROCESSED;
    }

    /**
     * determine whether discovered product is what we expect to handle. Default implementation only cares about {@link JBossProduct#unknown()} 
     * @param context
     * @return
     */
    protected boolean isManagedProduct(JBossProductContext context) {
        return JBossProduct.unknown().equals(context.getProduct());
    }

    /**
     * true if {@link #getProduct()}s SLOT_VALUE equals to given slotValue
     * @param slotValue detected from product.conf
     */
    protected boolean equalsProductSlot(String slotValue) {
        return getProduct().SLOT_VALUE.equals(slotValue);
    }

    /**
     * subclass can override and set whether {@link #handleUnknownProduct(JBossProductContext)} will update discovered resource name, description and version based
     * on {@link #getProduct()} details or not. By default is true
     */
    protected boolean isUpdateResourceDetails() {
        return true;
    }

    /**
     * return JBossProduct instance. Most important is {@link JBossProduct#SLOT_VALUE} which must match slot value of the product 
     * which extending class/plugin manages
     * 
     * @return non-null target product
     */
    abstract JBossProduct getProduct();

    /**
     * helper class keeping discovered Server context
     * @author lzoubek@redhat.com
     *
     */
    public static class JBossProductContext {
        private final DiscoveredResourceDetails discoveredDetails;
        private final ServerPluginConfiguration serverConfiguration;
        private final JBossProduct product;

        public JBossProductContext(DiscoveredResourceDetails discoveredDetails) {
            this.discoveredDetails = discoveredDetails;
            this.serverConfiguration = new ServerPluginConfiguration(discoveredDetails.getPluginConfiguration());
            this.product = this.serverConfiguration.getProduct();
        }

        public DiscoveredResourceDetails getDiscoveredDetails() {
            return discoveredDetails;
        }

        public ServerPluginConfiguration getServerConfiguration() {
            return serverConfiguration;
        }

        public JBossProduct getProduct() {
            return product;
        }

    }
}
