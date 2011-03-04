/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.core.domain.util;

import static org.testng.Assert.assertEquals;
import static org.rhq.core.domain.resource.composite.DisambiguationReport.*;

import java.util.ArrayList;
import java.util.Collections;

import org.testng.annotations.Test;

import org.rhq.core.domain.resource.composite.DisambiguationReport;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class DisambiguationReportRendererTest {

    private static final String RESOURCE_NAME = "testResource";
    private static final String RESOURCE_TYPE_NAME = "testResourceType";
    private static final String RESOURCE_TYPE_PLUGIN_NAME = "testPlugin";
    private static final int RESOURCE_ID = 0;
    private static final String PARENT_RESOURCE_NAME = "testResourceParent";
    private static final String PARENT_RESOURCE_TYPE_NAME = "testResourceTypeParent";
    private static final String PARENT_RESOURCE_TYPE_PLUGIN_NAME = "testPluginParent";

    private static final String TEST_NO_PARENTS_TEMPLATE = "seeing %[a resource called ]name[ ]%[with an id of ]id%[ in a type ]type.name%[ in a plugin ]type.plugin[].";
    private static final String EXPECTED_NO_PARENTS_RENDERING_FULL = "seeing a resource called " + RESOURCE_NAME
        + " with an id of " + RESOURCE_ID + " in a type " + RESOURCE_TYPE_NAME + " in a plugin "
        + RESOURCE_TYPE_PLUGIN_NAME + ".";
    private static final String EXPECTED_NO_PARENTS_RENDERING_NO_PLUGIN = "seeing a resource called " + RESOURCE_NAME
        + " with an id of " + RESOURCE_ID + " in a type " + RESOURCE_TYPE_NAME + ".";
    private static final String EXPECTED_NO_PARENTS_RENDERING_NO_TYPE = "seeing a resource called " + RESOURCE_NAME
        + " with an id of " + RESOURCE_ID + ".";

    private static final String TEST_PARENTS_TEMPLATE = "%type.name[ ]%[(]type.plugin[) ]%name";
    private static final String TEST_PARENTS_SINGLETON_TEMPLATE = "%name (\\%singleton)";
    private static final String ASCENDING_SEPARATOR = " -> ";
    private static final String DESCENDING_SEPARATOR = " <- ";
    private static final int NOF_PARENTS = 2;
    
    private static final String EXPECTED_PARENTS_RENDERING_FULL_NO_SINGLETON_ASCENDING = RESOURCE_TYPE_NAME + " (" + RESOURCE_TYPE_PLUGIN_NAME + ") " + RESOURCE_NAME +
        ASCENDING_SEPARATOR + PARENT_RESOURCE_TYPE_NAME + " (" + PARENT_RESOURCE_TYPE_PLUGIN_NAME + ") " + PARENT_RESOURCE_NAME + "1" +
        ASCENDING_SEPARATOR + PARENT_RESOURCE_TYPE_NAME + " (" + PARENT_RESOURCE_TYPE_PLUGIN_NAME + ") " + PARENT_RESOURCE_NAME + "2";
    
    private static final String EXPECTED_PARENTS_RENDERING_FULL_WITH_SINGLETON_ASCENDING = RESOURCE_TYPE_NAME + " (" + RESOURCE_TYPE_PLUGIN_NAME + ") " + RESOURCE_NAME +
        ASCENDING_SEPARATOR + PARENT_RESOURCE_NAME + "1 (%singleton)" +
        ASCENDING_SEPARATOR + PARENT_RESOURCE_TYPE_NAME + " (" + PARENT_RESOURCE_TYPE_PLUGIN_NAME + ") " + PARENT_RESOURCE_NAME + "2";

    private static final String EXPECTED_PARENTS_RENDERING_FULL_NO_SINGLETON_DESCENDING = 
        PARENT_RESOURCE_TYPE_NAME + " (" + PARENT_RESOURCE_TYPE_PLUGIN_NAME + ") " + PARENT_RESOURCE_NAME + "2" +
        DESCENDING_SEPARATOR + PARENT_RESOURCE_TYPE_NAME + " (" + PARENT_RESOURCE_TYPE_PLUGIN_NAME + ") " + PARENT_RESOURCE_NAME + "1" +
        DESCENDING_SEPARATOR + RESOURCE_TYPE_NAME + " (" + RESOURCE_TYPE_PLUGIN_NAME + ") " + RESOURCE_NAME;
    
    private static final String EXPECTED_PARENTS_RENDERING_FULL_WITH_SINGLETON_DESCENDING = 
        PARENT_RESOURCE_TYPE_NAME + " (" + PARENT_RESOURCE_TYPE_PLUGIN_NAME + ") " + PARENT_RESOURCE_NAME + "2" +
        DESCENDING_SEPARATOR + PARENT_RESOURCE_NAME + "1 (%singleton)" +
        DESCENDING_SEPARATOR + RESOURCE_TYPE_NAME + " (" + RESOURCE_TYPE_PLUGIN_NAME + ") " + RESOURCE_NAME;

    private static final String EXPECTED_PARENTS_RENDERING_MINIMAL_WITH_SINGLETON_ASCENDING = RESOURCE_NAME + 
        ASCENDING_SEPARATOR + PARENT_RESOURCE_NAME + "1 (%singleton)" +
        ASCENDING_SEPARATOR + PARENT_RESOURCE_NAME + "2";
    
    @Test
    public void testNoParents() {
        DisambiguationReport<?> report = getNoParentsTestReport(true, true);

        DisambiguationReportRenderer renderer = new DisambiguationReportRenderer();

        renderer.setIncludeParents(false);
        renderer.setSegmentTemplate(TEST_NO_PARENTS_TEMPLATE);
        renderer.setSingletonSegmentTemplate(TEST_NO_PARENTS_TEMPLATE);

        String rendering = renderer.render(report);
        assertEquals(rendering, EXPECTED_NO_PARENTS_RENDERING_FULL);

        report = getNoParentsTestReport(true, false);
        rendering = renderer.render(report);
        assertEquals(rendering, EXPECTED_NO_PARENTS_RENDERING_NO_PLUGIN);

        report = getNoParentsTestReport(false, false);
        rendering = renderer.render(report);
        assertEquals(rendering, EXPECTED_NO_PARENTS_RENDERING_NO_TYPE);
    }

    @Test
    public void testParents() {
        DisambiguationReport<?> report = getParentsTestReport(true, true, true, true, false);
        
        DisambiguationReportRenderer renderer = new DisambiguationReportRenderer();
        renderer.setSegmentTemplate(TEST_PARENTS_TEMPLATE);
        renderer.setSingletonSegmentTemplate(TEST_PARENTS_SINGLETON_TEMPLATE);
        renderer.setSegmentSeparator(ASCENDING_SEPARATOR);
        
        String rendering = renderer.render(report);
        assertEquals(rendering, EXPECTED_PARENTS_RENDERING_FULL_NO_SINGLETON_ASCENDING);
        
        renderer.setSegmentSeparator(DESCENDING_SEPARATOR);
        renderer.setRenderingOrder(DisambiguationReportRenderer.RenderingOrder.DESCENDING);
        rendering = renderer.render(report);
        assertEquals(rendering, EXPECTED_PARENTS_RENDERING_FULL_NO_SINGLETON_DESCENDING);
        
        report = getParentsTestReport(true, true, true, true, true);

        renderer.setSegmentSeparator(ASCENDING_SEPARATOR);
        renderer.setRenderingOrder(DisambiguationReportRenderer.RenderingOrder.ASCENDING);
        rendering = renderer.render(report);
        assertEquals(rendering, EXPECTED_PARENTS_RENDERING_FULL_WITH_SINGLETON_ASCENDING);
        
        renderer.setSegmentSeparator(DESCENDING_SEPARATOR);
        renderer.setRenderingOrder(DisambiguationReportRenderer.RenderingOrder.DESCENDING);
        rendering = renderer.render(report);
        assertEquals(rendering, EXPECTED_PARENTS_RENDERING_FULL_WITH_SINGLETON_DESCENDING);
        
        report = getParentsTestReport(false, false, false, false, true);

        renderer.setSegmentSeparator(ASCENDING_SEPARATOR);
        renderer.setRenderingOrder(DisambiguationReportRenderer.RenderingOrder.ASCENDING);
        rendering = renderer.render(report);
        assertEquals(rendering, EXPECTED_PARENTS_RENDERING_MINIMAL_WITH_SINGLETON_ASCENDING);
    }
    
    private <T> DisambiguationReport<T> getNoParentsTestReport(boolean includeType, boolean includePlugin) {
        return new DisambiguationReport<T>(null, Collections.<Resource> emptyList(),
            createResource(0, includeType, includePlugin, false));
    }
    
    private <T> DisambiguationReport<T> getParentsTestReport(boolean includeType, boolean includePlugin, boolean includeParentsType, boolean includeParentsPlugin, boolean firstParentSingleton) {
        Resource resource = createResource(0, includeType, includePlugin, false);
        Resource firstParent = createResource(1, includeParentsType, includeParentsPlugin, firstParentSingleton);
        
        ArrayList<Resource> parents = new ArrayList<Resource>();
        parents.add(firstParent);
        
        for(int i = 2; i <= NOF_PARENTS; ++i) {
            Resource parent = createResource(i, includeParentsType, includeParentsPlugin, false);
            parents.add(parent);
        }
        
        return new DisambiguationReport<T>(null, parents, resource);
    }
    
    private static Resource createResource(int parentage, boolean includeType, boolean includePlugin, boolean singleton) {
        ResourceType type = null;
        
        String resourceTypeName = parentage > 0 ? PARENT_RESOURCE_TYPE_NAME : RESOURCE_TYPE_NAME;
        String pluginName = parentage > 0 ? PARENT_RESOURCE_TYPE_PLUGIN_NAME : RESOURCE_TYPE_PLUGIN_NAME;
        
        if (includeType || singleton) {
            type = new ResourceType(resourceTypeName, includePlugin ? pluginName
                : null, singleton);
        }
        
        String name;
        
        if (parentage == 0) {
            name = RESOURCE_NAME;
        } else {
            name = PARENT_RESOURCE_NAME + parentage;
        }
        
        return new Resource(parentage, name, type);
    }
}
