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
package org.rhq.modules.plugins.wildfly10;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;

/**
 * Test current product detection.
 *
 * @author Michael Burman
 */
public class ProductTypeTest {

    @Rule
    public TemporaryFolder homeDir = new TemporaryFolder();

    private void createProductConf(String productName) throws IOException {
        File bin = homeDir.newFolder("bin");
        File productConf = new File(bin, "product.conf");
        FileOutputStream fos = new FileOutputStream(productConf);
        Properties productConfProps = new Properties();
        productConfProps.setProperty("slot", productName);
        productConfProps.store(fos, "JBoss product branding configuration");
        fos.flush();
        fos.close();
    }

    @Test
    public void testProductParsing() throws Exception {
        createProductConf("eap");
        JBossProductType jBossProductType = JBossProductType.determineJBossProductType(homeDir.getRoot(), "4.0");
        assertEquals(jBossProductType, JBossProductType.EAP);

        createProductConf("jdg");
        jBossProductType = JBossProductType.determineJBossProductType(homeDir.getRoot(), "4.0");
        assertEquals(jBossProductType, JBossProductType.JDG);

        createProductConf("undefined");
        jBossProductType = JBossProductType.determineJBossProductType(homeDir.getRoot(), "4.0");
        assertEquals(jBossProductType, JBossProductType.WILDFLY);

        createProductConf("eap");
        jBossProductType = JBossProductType.determineJBossProductType(homeDir.getRoot(), "1.0");
        assertEquals(jBossProductType, null);
    }
}
