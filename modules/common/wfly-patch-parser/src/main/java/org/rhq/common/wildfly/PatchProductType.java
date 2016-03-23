/*
 * RHQ Management Platform
 * Copyright (C) 2005-2016 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.common.wildfly;

/**
 * Simplified and combined version of JBossProductType as defined in the jboss-as-7 and wfly-10 agent plugins
 *
 * @author Michael Burman
 */
public enum PatchProductType {

    EAP("EAP", "JBoss EAP 6", "JBoss Enterprise Application Platform 6", "EAP"),
    EAP7("EAP", "EAP 7", "JBoss Enterprise Application Platform 7", "JBoss EAP"),
    ISPN("ISPN", "Infinispan Server", "Infinispan Server", "Infinispan Server"),
    JDG("JDG", "JBoss JDG 6", "JBoss Data Grid 6", "Data Grid"),
    JPP("JPP", "JBoss JPP 6", "JBoss Portal Platform 6", "Portal Platform"),
    SOA("SOA-P", "JBoss SOA-P 6", "Red Hat JBoss Fuse Service Works", "Red Hat JBoss Fuse Service Works"),
    BRMS("BRMS", "JBoss BRMS", "Red Hat JBoss BRMS", "BRMS"),
    BPMS("BPM Suite", "JBoss BPM Suite", "Red Hat JBoss BPM Suite", "BPM Suite"),
    JDV("JDV", "Data Virt", "Red Hat JBoss Data Virtualization", "Red Hat JBoss Data Virtualization"),
    WILDFLY10("WildFly", "WildFly 10", "WildFly Application Server 10", "WildFly Full");

    public final String SHORT_NAME;
    public final String NAME;
    public final String FULL_NAME;
    /** The value the server returns for the "product-name" attribute of the root resource. */
    public final String PRODUCT_NAME;

    PatchProductType(String shortName, String name, String fullName, String productName) {
        this.SHORT_NAME = shortName;
        this.NAME = name;
        this.FULL_NAME = fullName;
        this.PRODUCT_NAME = productName;
    }

    public static PatchProductType getValueByProductName(String productName) {
        for (PatchProductType productType : PatchProductType.values()) {
            if (productType.PRODUCT_NAME.equals(productName)) {
                return productType;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return this.NAME;
    }

}
