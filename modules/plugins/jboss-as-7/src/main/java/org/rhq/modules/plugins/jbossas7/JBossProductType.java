/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
import java.util.Properties;

/**
 * Metadata describing a product based on JBoss AS 7.x.
 *
 * @author Ian Springer
 */
public enum JBossProductType {

    AS("AS", "JBoss AS 7", "JBoss Application Server 7", "AS"),
    EAP("EAP", "JBoss EAP 6", "JBoss Enterprise Application Platform 6", "EAP"),
    ISPN("ISPN", "Infinispan Server", "Infinispan Server", "Infinispan Server"),
    JDG("JDG", "JBoss JDG 6", "JBoss Data Grid 6", "Data Grid"),
    JPP("JPP", "JBoss JPP 6", "JBoss Portal Platform 6", "Portal Platform"),
    SOA("SOA-P", "JBoss SOA-P 6", "Red Hat JBoss Fuse Service Works", "Red Hat JBoss Fuse Service Works"),
    BRMS("BRMS", "JBoss BRMS", "Red Hat JBoss BRMS", "BRMS"),
    BPMS("BPM Suite", "JBoss BPM Suite", "Red Hat JBoss BPM Suite", "BPM Suite"),
    JDV("JDV", "Data Virt", "Red Hat JBoss Data Virtualization", "Red Hat JBoss Data Virtualization"),
    UNSUPPORTED("", "", "", "");

    public final String SHORT_NAME;
    public final String NAME;
    public final String FULL_NAME;
    /** The value the server returns for the "product-name" attribute of the root resource. */
    public final String PRODUCT_NAME;

    JBossProductType(String shortName, String name, String fullName, String productName) {
        this.SHORT_NAME = shortName;
        this.NAME = name;
        this.FULL_NAME = fullName;
        this.PRODUCT_NAME = productName;
    }

    /**
     * Determine the product type of a JBoss install. This implies an api version of 1.x which
     * is a JBossAS 7.x
     * @param homeDir the JBoss product installation directory (e.g. /opt/jboss-as-7.1.1.Final)
     * @return the product type
     * @deprecated "Use the version with the apiVersion"
     */
    @Deprecated
    public static JBossProductType determineJBossProductType(File homeDir) {
        return determineJBossProductType(homeDir,"1.0");
    }

    /**
     * Determines the product type of a JBoss product installation.
     *
     * @param homeDir the JBoss product installation directory (e.g. /opt/jboss-as-7.1.1.Final)
     *
     * @param apiVersion Api version of the domain api.
     * @return the product type
     */
    public static JBossProductType determineJBossProductType(File homeDir, String apiVersion) {
        try {
            JBossProductType jBossProductType = determineJBossProductTypeViaProductConfFile(homeDir, apiVersion);
            if (jBossProductType==null) {
                // Wildfly and The Server Formerly Known AS JBossAS share the same absence of a slot
                // and thus have no product type. So we need to check differently
                // AS 7.0/1 use a domain api version of 1.x, while WildFly uses version 2.0+
                // like 2.0 in "urn:jboss:domain:2.0" from <server xmlns="..." > element in standalone.xml
                if (apiVersion.startsWith("1")) {
                    jBossProductType = JBossProductType.AS;
                }
            }
            return jBossProductType;
        } catch (Exception e) {
            // TODO: Log an error.
            return determineJBossProductTypeViaHomeDirName(homeDir);
        }
    }

    public static JBossProductType getValueByProductName(String productName) {
        for (JBossProductType productType : JBossProductType.values()) {
             if (productType.PRODUCT_NAME.equals(productName)) {
                 return productType;
             }
        }
        throw new IllegalArgumentException("No product type with product-name '" + productName + "' is known.");
    }

    private static JBossProductType determineJBossProductTypeViaProductConfFile(File homeDir, String apiVersion) throws Exception {
        JBossProductType productType = null;
        File productConfFile = new File(homeDir, "bin/product.conf");
        if (productConfFile.exists()) {
            // It's some product (i.e. not community AS).
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

            if(apiVersion.startsWith("1")) {
                try {
                    String searchQuery = slot.toUpperCase();
                    if(searchQuery.equals("DV")) {
                        searchQuery = "JDV";
                    }
                    productType = valueOf(searchQuery);
                } catch(IllegalArgumentException e) {
                    productType = UNSUPPORTED;
                }
            }
        }

        return productType;
    }

    private static JBossProductType determineJBossProductTypeViaHomeDirName(File homeDir) {
        JBossProductType productType;
        String homeDirName = homeDir.getName();
        if (homeDirName.contains("-as-")) {
            productType = JBossProductType.AS;
        } else if (homeDirName.contains("-eap-")) {
            productType = JBossProductType.EAP;
        } else if (homeDirName.contains("infinispan-server")) {
            productType = JBossProductType.ISPN;
        } else if (homeDirName.contains("-jdg-")||(homeDirName.contains("datagrid-server"))) {
            productType = JBossProductType.JDG;
        } else if (homeDirName.contains("-jpp-")) {
            productType = JBossProductType.JPP;
        } else {
             throw new RuntimeException("Failed to determine product type for JBoss product installed at [" + homeDir + "].");
        }

        return productType;
    }

    @Override
    public String toString() {
        return this.NAME;
    }

}
