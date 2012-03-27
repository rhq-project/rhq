/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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
package org.rhq.modules.plugins.jbossas7;

import java.io.File;
import java.util.Arrays;

/**
 * A product based on JBoss 7.x.
 *
 * @author Ian Springer
 */
public enum JBossProductType {

    AS("AS7", "JBoss AS 7", "JBoss Application Server 7"),
    EAP("EAP6", "JBoss EAP 6", "JBoss Enterprise Application Platform 6"),
    EDG("EDG6", "JBoss EDG 6", "JBoss Enterprise Data Grid 6"),
    EPP("EPP6", "JBoss EAP 6", "JBoss Enterprise Portal Platform 6"),
//    EWP("EWP6", "JBoss EWP 6", "JBoss Enterprise Web Platform 6"),
    SOA("SOA-P6", "JBoss SOA-P 6", "JBoss Enterprise SOA Platform (ESB)");

    public final String SHORT_NAME;
    public final String NAME;
    public final String FULL_NAME;

    private static final String EAP_IMPLEMENTATION_TITLE = "JBoss Enterprise Application Platform";

    JBossProductType(String shortName, String name, String fullName) {
        this.SHORT_NAME = shortName;
        this.NAME = name;
        this.FULL_NAME = fullName;
    }

    /**
     * Determines the product type of a JBoss product installation.
     *
     * @param homeDir the JBoss product installation directory (e.g. /opt/jboss-as-7.1.1.Final) 
     *
     * @return the product type
     */
    public static JBossProductType determineJBossProductType(File homeDir) {
        JBossProductType productType;                
        File productDir = new File(homeDir, "modules/org/jboss/as/product");
        if (productDir.exists()) {
            // It's some product (i.e. not community AS).
            File[] files = productDir.listFiles();
            if (files.length == 0) {
                throw new RuntimeException("Unable to determine product type - [" + productDir 
                        + "] exists but is empty.");
            }
            if (files.length > 1) {
                throw new RuntimeException("Unable to determine product type - [" + productDir 
                        + "] contains multiple product subdirectories: " + Arrays.toString(files));
            }
            File productTypeDir = files[0];
            String productName = productTypeDir.getName();
            if (productName.equals("eap")) {
                productType = JBossProductType.EAP;
            } else if (productName.equals("edg")) {
                productType = JBossProductType.EDG;
            } else {
                throw new RuntimeException("Unknown product type: " + productName);
            }
        } else {
            productType = JBossProductType.AS;
        }

        return productType;
    }

    @Override
    public String toString() {
        return this.NAME;
    }

}
