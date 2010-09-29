/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.core.pc.upgrade;

import java.util.Arrays;
import java.util.Collection;

import org.testng.annotations.Test;

/**
 * The plugins and their resource types form the following dependency structure:
 * <pre>
 *                                       Root(root)
 *                                       /        \
 *               ParentDependency(parentdep)  ParentDepSibling(parentsibling)
 *                      /          \
 *            TestResource(test) TestResourceSibling(sibling)  
 * </pre>
 * The dependencies in the above "chart" are formed using either the <code>&lt;runs-inside&gt;</code>
 * or <code>sourcePlugin/sourceType</code> approaches just to test that both are handled correctly.
 *                    
 * @author Lukas Krejci
 */
@Test(sequential = true, invocationCount = 1)
public class ResourceUpgradeFailureHandlingTest extends ResourceUpgradeTestBase {

    //plugin names
    private static final String BASE_PLUGIN_NAME = "/resource-upgrade-test-plugin-multi-base-1.0.0.jar";
    private static final String PARENT_DEP_V1_PLUGIN_NAME = "/resource-upgrade-test-plugin-multi-parentdep-1.0.0.jar";
    private static final String PARENT_DEP_V2_PLUGIN_NAME = "/resource-upgrade-test-plugin-multi-parentdep-2.0.0.jar";
    private static final String PARENT_SIBLING_V1_PLUGIN_NAME = "/resource-upgrade-test-plugin-multi-parentsibling-1.0.0.jar";
    private static final String PARENT_SIBLING_V2_PLUGIN_NAME = "/resource-upgrade-test-plugin-multi-parentsibling-2.0.0.jar";
    private static final String ROOT_PLUGIN_NAME = "/resource-upgrade-test-plugin-multi-root-1.0.0.jar";
    private static final String SIBLING_V1_PLUGIN_NAME = "/resource-upgrade-test-plugin-multi-sibling-1.0.0.jar";
    private static final String SIBLING_V2_PLUGIN_NAME = "/resource-upgrade-test-plugin-multi-sibling-2.0.0.jar";
    private static final String TEST_V1_PLUGIN_NAME = "/resource-upgrade-test-plugin-multi-test-1.0.0.jar";
    private static final String TEST_V2_PLUGIN_NAME = "/resource-upgrade-test-plugin-multi-test-2.0.0.jar";

    protected Collection<String> getRequiredPlugins() {
        return Arrays.asList(BASE_PLUGIN_NAME, PARENT_DEP_V1_PLUGIN_NAME, PARENT_DEP_V2_PLUGIN_NAME,
            PARENT_SIBLING_V1_PLUGIN_NAME, PARENT_SIBLING_V2_PLUGIN_NAME, ROOT_PLUGIN_NAME, SIBLING_V1_PLUGIN_NAME,
            SIBLING_V2_PLUGIN_NAME, TEST_V1_PLUGIN_NAME, TEST_V2_PLUGIN_NAME);
    }
    
    @Test
    public void testSuccess() {
        //TODO implement
        //check that all the stuff is properly upgraded on success
    }
    
    @Test
    public void testFailureOnLeaf() {
        //TODO implement
        //check that the system behaves correctly if there is an upgrade failure
        //at the leaf node of the plugin dep graph
    }
    
    @Test
    public void testFailureOnDependencies() {
        //TODO implement
        //check that stuff works if there is an upgrade failure on some of the resources
        //in the plugin some "in the middle" of the plugin dep graph
    }
}
