/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.resource.metadata.test;

import org.testng.annotations.Test;

/**
 * Various testing around subCategories
 *
 * @author Heiko W. Rupp
 */
public class SubcategoryTest extends UpdateSubsytemTestBase {

    @Override
    protected String getSubsystemDirectory() {
        return "resource";
    }

    @Test
    public void testAddResourcTypeWithKnownSubCategory() throws Exception {
        // Note, plugins are registered in new transactions. for tests, this means
        // you can't do everything in a trans and roll back at the end. You must clean up
        // manually.
        try {
            registerPlugin("test-subcategories.xml");
            registerPlugin("test-subcategories3.xml");
        } finally {
            // clean up
            try {
                cleanupTest();
            } catch (Exception e) {
                System.out.println("CANNNOT CLEAN UP TEST: " + this.getClass().getSimpleName()
                    + ".testAddResourcTypeWithKnownSubCategory");
            }
        }
    }

}
