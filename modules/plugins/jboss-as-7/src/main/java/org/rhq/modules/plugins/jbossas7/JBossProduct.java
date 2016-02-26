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
import java.io.FileFilter;

/**
 * this class tends to be replacement of {@link JBossProductType} which was designed as enum and we're hitting enum
 * limitations in AS7-based plugins
 * @author lzoubek
 *
 */
@SuppressWarnings("deprecation")
public class JBossProduct {

    private static FileFilter homeDirMatchFilter(final String... substrings) {
        return new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                if (substrings != null) {
                    String homeDir = pathname.getName();
                    for (String s : substrings) {
                        if (s != null && homeDir.contains(s)) {
                            return true;
                        }
                    }
                }
                return false;
            }
        };
    }

    public static final JBossProduct EAP = new JBossProduct(JBossProductType.EAP, "eap", homeDirMatchFilter("-eap-"));

    public static final JBossProduct WILDFLY8 = new JBossProduct(JBossProductType.WILDFLY8, null,
        homeDirMatchFilter("wildfly"));

    public static final JBossProduct ISPN = new JBossProduct(JBossProductType.ISPN, "ispn",
        homeDirMatchFilter("infinispan-server"));

    public static final JBossProduct JDG = new JBossProduct(JBossProductType.JDG, "jdg", homeDirMatchFilter("-jdg-",
        "datagrid-server"));

    public static final JBossProduct JPP = new JBossProduct(JBossProductType.JPP, "jpp", homeDirMatchFilter("-jpp-"));

    public static final JBossProduct SOA = new JBossProduct(JBossProductType.SOA, "soa", null);

    public static final JBossProduct AS = new JBossProduct(JBossProductType.AS, null, homeDirMatchFilter("-as-"));

    public static final JBossProduct JDV = new JBossProduct(JBossProductType.JDV, "dv", null);

    public static final JBossProduct unknown() {
        return unknown(null);
    }

    /**
     * creates "special" product instance which is going to have <strong>all</strong> properties set to given productName.
     * This is used as a fallback when we discover manually added server of an unknown product type (unknown by AS7 plugin)
     * @param productName
     * @return
     */
    public static JBossProduct fromProductName(String productName) {
        return new JBossProduct(productName, productName, productName, productName, productName, null, null);
    }

    public static JBossProduct unknown(String slotValue) {
        return new JBossProduct("UNKNOWN", "unknown", "Unknown", "Unknown", "Unknown", slotValue, null);
    }

    /**
     * name of product as referred in pluginConfiguration (ie. AS, EAP, JDG)
     */
    public final String PLUGIN_CONFIG_NAME;
    /**
     * product short-name is usually used to build new resource name
     */
    public final String SHORT_NAME;
    /**
     * product name
     */
    public final String NAME;
    /**
     * full name is usually used to build new resource desctiprion (ie. JBoss Enterprise Application Platform 6)
     */
    public final String FULL_NAME;
    /**
     * The value the server returns for the "product-name" attribute of the root resource.
     */
    public final String PRODUCT_NAME;
    /**
     * corresponding value of <strong>slot</strong> property within product.conf. Can be null
     */
    public final String SLOT_VALUE;
    /**
     * File filter in case we fail to detect using {@link #SLOT_VALUE}, we fall-back to detecting the product based on homeDir. Can be null
     */
    private final FileFilter homeDirFilter;

    public JBossProduct(JBossProductType productType) {
        this(productType, null, null);
    }

    public JBossProduct(JBossProductType productType, String slotValue, FileFilter homeDirFilter) {
        this.PLUGIN_CONFIG_NAME = productType.name();
        this.SHORT_NAME = productType.SHORT_NAME;
        this.NAME = productType.NAME;
        this.FULL_NAME = productType.FULL_NAME;
        this.PRODUCT_NAME = productType.PRODUCT_NAME;
        this.SLOT_VALUE = slotValue;
        this.homeDirFilter = homeDirFilter;
    }

    /**
     * create new instance
     * 
     * @param pluginConfigName @see {@link #PLUGIN_CONFIG_NAME}
     * @param shortName @see {@link #SHORT_NAME}
     * @param name @see {@link #NAME}
     * @param fullName @see {@link #FULL_NAME}
     * @param productName @see {@link #PRODUCT_NAME}
     * @param slotValue @see {@link #SLOT_VALUE}
     */
    public JBossProduct(String pluginConfigName, String shortName, String name, String fullName, String productName,
        String slotValue) {
        this(pluginConfigName, shortName, name, fullName, productName, slotValue, null);
    }

    /**
     * create new instance
     * 
     * @param pluginConfigName @see {@link #PLUGIN_CONFIG_NAME}
     * @param shortName @see {@link #SHORT_NAME}
     * @param name @see {@link #NAME}
     * @param fullName @see {@link #FULL_NAME}
     * @param productName @see {@link #PRODUCT_NAME}
     * @param slotValue @see {@link #SLOT_VALUE}
     * @param homeDirFilter @see {@link #homeDirFilter}
     */
    JBossProduct(String pluginConfigName, String shortName, String name, String fullName, String productName,
        String slotValue, FileFilter homeDirFilter) {
        this.PLUGIN_CONFIG_NAME = pluginConfigName;
        this.SHORT_NAME = shortName;
        this.NAME = name;
        this.FULL_NAME = fullName;
        this.PRODUCT_NAME = productName;
        this.SLOT_VALUE = slotValue;
        this.homeDirFilter = homeDirFilter;
    }

    public boolean matchesSlot(String slotValue) {
        return slotValue == null ? false : slotValue.equals(this.SLOT_VALUE);
    }

    public boolean matchesHomeDir(File homeDir) {
        return homeDirFilter == null ? false : homeDirFilter.accept(homeDir);
    }


    @Override
    public String toString() {
        return this.NAME;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((FULL_NAME == null) ? 0 : FULL_NAME.hashCode());
        result = prime * result + ((NAME == null) ? 0 : NAME.hashCode());
        result = prime * result + ((PLUGIN_CONFIG_NAME == null) ? 0 : PLUGIN_CONFIG_NAME.hashCode());
        result = prime * result + ((PRODUCT_NAME == null) ? 0 : PRODUCT_NAME.hashCode());
        result = prime * result + ((SHORT_NAME == null) ? 0 : SHORT_NAME.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        JBossProduct other = (JBossProduct) obj;
        if (FULL_NAME == null) {
            if (other.FULL_NAME != null)
                return false;
        } else if (!FULL_NAME.equals(other.FULL_NAME))
            return false;
        if (NAME == null) {
            if (other.NAME != null)
                return false;
        } else if (!NAME.equals(other.NAME))
            return false;
        if (PLUGIN_CONFIG_NAME == null) {
            if (other.PLUGIN_CONFIG_NAME != null)
                return false;
        } else if (!PLUGIN_CONFIG_NAME.equals(other.PLUGIN_CONFIG_NAME))
            return false;
        if (PRODUCT_NAME == null) {
            if (other.PRODUCT_NAME != null)
                return false;
        } else if (!PRODUCT_NAME.equals(other.PRODUCT_NAME))
            return false;
        if (SHORT_NAME == null) {
            if (other.SHORT_NAME != null)
                return false;
        } else if (!SHORT_NAME.equals(other.SHORT_NAME))
            return false;
        return true;
    }

}
