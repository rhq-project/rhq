/*
 * RHQ Management Platform
 * Copyright (C) 2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.plugins.jbossas5.helper;

import java.util.jar.Attributes;

/**
 * The product type of a JBoss 5.x/6.x application server installation - AS, EAP, EWP, or SOA.
 *
 * @author Jessica Sant
 * @author Ian Springer
 */
public enum JBossProductType {
    AS("JBoss AS", "JBoss Application Server", "default"),
    EAP("JBoss EAP", "JBoss Enterprise Application Platform", "default"),
    EWP("JBoss EWP", "JBoss Enterprise Web Platform", "default"),
    SOA("JBoss SOA-P", "JBoss Enterprise SOA Platform", "default"),
    BRMS("JBoss BRMS", "JBoss Business Rules Management System", "default"),
    EPP("JBoss EPP", "JBoss Enterprise Portal Platform", "default");

    public final String NAME;
    public final String DESCRIPTION;
    public final String DEFAULT_CONFIG_NAME;

    private static final String EAP_IMPLEMENTATION_TITLE = "JBoss [EAP]";
    private static final String EWP_IMPLEMENTATION_TITLE = "JBoss [EWP]";
    private static final String SOA_IMPLEMENTATION_TITLE = "JBoss [SOA]";
    private static final String BRMS_IMPLEMENTATION_TITLE = "JBoss [BRMS]";
    private static final String EPP_IMPLEMENTATION_TITLE = "JBoss [EPP]";

    JBossProductType(String name, String description, String defaultConfigName) {
        this.NAME = name;
        this.DESCRIPTION = description;
        this.DEFAULT_CONFIG_NAME = defaultConfigName;
    }

    /**
     * Determines the product type (AS or EAP or a layered product like BRMS) based on the
     * Implementation-Title MANIFEST.MF attribute.
     * <p>
     * Note that this method is <b>NOT</b> always correct about the actual version of the product, because
     * certain version of certain products don't advertise the correct product/version in the manifest.
     * <p>
     * Use {@link JBossInstallationInfo} for a more thorough detection of the type and version of a product.
     * 
     * @param attributes the attributes from a manifest file (typically run.jar or jboss-j2ee.jar)
     *
     * @return the product type (AS, EAP, EWP, SOA, BRMS, or EPP)
     */
    public static JBossProductType determineJBossProductType(Attributes attributes) {
        JBossProductType result = JBossProductType.AS;
        String implementationTitle = (attributes != null) ? attributes.getValue(Attributes.Name.IMPLEMENTATION_TITLE)
            : null;
        if (implementationTitle != null) {
            if (implementationTitle.equalsIgnoreCase(EAP_IMPLEMENTATION_TITLE)) {
                result = JBossProductType.EAP;
            } else if (implementationTitle.equalsIgnoreCase(EWP_IMPLEMENTATION_TITLE)) {
                result = JBossProductType.EWP;
            } else if (implementationTitle.equalsIgnoreCase(SOA_IMPLEMENTATION_TITLE)) {
                result = JBossProductType.SOA;
            } else if (implementationTitle.equalsIgnoreCase(BRMS_IMPLEMENTATION_TITLE)) {
                result = JBossProductType.BRMS;
            } else if (implementationTitle.equalsIgnoreCase(EPP_IMPLEMENTATION_TITLE)) {
               result = JBossProductType.EPP;
           }
        }
        return result;
    }

    @Override
    public String toString() {
        return this.NAME;
    }
}