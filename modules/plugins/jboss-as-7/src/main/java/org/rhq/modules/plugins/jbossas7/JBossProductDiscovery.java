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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class JBossProductDiscovery {
    private static final Log LOG = LogFactory.getLog(JBossProductDiscovery.class);
    private static final Map<String, JBossProduct> KNOWN_PRODUCTS = new HashMap<String, JBossProduct>();

    static {
        Iterator<JBossProduct> products = Arrays.asList(
            JBossProduct.AS,
            JBossProduct.EAP,
            JBossProduct.WILDFLY8,
            JBossProduct.ISPN,
            JBossProduct.JDG,
            JBossProduct.JPP,
            JBossProduct.SOA,
            JBossProduct.JDV,
            JBossProduct.unknown()
        ).iterator();

        while (products.hasNext()) {
            JBossProduct product = products.next();
            KNOWN_PRODUCTS.put(product.PLUGIN_CONFIG_NAME, product);
        }
    }

    /**
     * determine JBossProduct based on product homeDir and apiVersion. This method can return {@link JBossProduct#unknown()} in case
     * it finds product.conf with valid (non-empty) slot value
     * @param homeDir
     * @param apiVersion
     * @return
     */
    public static JBossProduct determineProduct(File homeDir, String apiVersion) {
        JBossProduct product;
        try {
            product = determineJBossProductTypeViaProductConfFile(homeDir);
            if (product == null) {
                // Wildfly and The Server Formerly Known AS JBossAS share the same absence of a slot
                // and thus have no product type. So we need to check differently
                // AS 7.0/1 use a domain api version of 1.x, while WildFly uses version 2.0+
                // like 2.0 in "urn:jboss:domain:2.0" from <server xmlns="..." > element in standalone.xml
                if (apiVersion.startsWith("1")) {
                    product = JBossProduct.AS;
                } else {
                    // We should later check for newer WildFly versions to differentiate them
                    product = JBossProduct.WILDFLY8;
                }
            }
            return product;
        } catch (Exception e) {
            // fallback to homeDir
            LOG.debug(
                "Failed to determine product type using bin/product.conf file. Will now detect using homeDir name", e);
            return determineJBossProductViaHomeDir(homeDir);
        }
    }

    private static JBossProduct determineJBossProductTypeViaProductConfFile(File homeDir) throws Exception {
        String slot = readProductSlot(homeDir);
        if (slot != null) {
            // It's some product (i.e. not community AS).
            for (JBossProduct product : KNOWN_PRODUCTS.values()) {
                if (product.matchesSlot(slot)) {
                    return product;
                }
            }
            LOG.info("Unknown product type: [" + slot + "], plugin discovery callback is required");
            return JBossProduct.unknown(slot);
        }
        return null;
    }

    private static JBossProduct determineJBossProductViaHomeDir(File homeDir) {
        for (JBossProduct product : KNOWN_PRODUCTS.values()) {
            if (product.matchesHomeDir(homeDir)) {
                return product;
            }
        }
        throw new RuntimeException("Failed to determine product type for JBoss product installed at [" + homeDir + "].");
    }

    /**
     * Reads slot property from bin/product.conf located in given <strong>homeDir</strong>
     * @param homeDir
     * @return slot value or null if bin/product.conf file does not exist (or not readable)
     * @throws Exception on parse error or empty slot value
     */
    public static String readProductSlot(File homeDir) throws Exception {
        File productConfFile = new File(homeDir, "bin/product.conf");
        if (productConfFile.canRead()) {
            Properties productConfProps = new Properties();
            FileInputStream inputStream = new FileInputStream(productConfFile);
            try {
                productConfProps.load(inputStream);
            } catch (IOException e) {
                throw new Exception("Failed to parse " + productConfFile + ".", e);
            } finally {
                inputStream.close();
            }
            String slot = productConfProps.getProperty("slot", "").trim();
            if (slot.isEmpty()) {
                throw new Exception("'slot' property not found in " + productConfFile + ".");
            }
            return slot;
        } else {
            return null;
        }

    }


    public static JBossProduct getKnownProduct(String pluginConfigSetting) {
        JBossProduct product = KNOWN_PRODUCTS.get(pluginConfigSetting);
        if (product == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Requested product by pluginConfigSetting=" + pluginConfigSetting
                    + " which we don't know, returning UNKNOWN");
            }
            return JBossProduct.unknown();
        }

        return product;
    }

    public static JBossProduct getKnownProductByProductName(String productName) {
        for (JBossProduct product : KNOWN_PRODUCTS.values()) {
            if (product.PRODUCT_NAME.equals(productName)) {
                return product;
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("No product type with product-name '" + productName + "' is known. returning UNKNOWN");
        }
        return JBossProduct.unknown();
    }
}
