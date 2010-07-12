/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.pc.plugin;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;

@Test
public class CanonicalResourceKeyTest {
    private final String KEY1 = "key1";
    private final String KEY2 = "key2";
    private final String KEY3 = "key3";
    private final String KEY4 = "key4";
    private final String KEY5 = "key5";

    private ResourceType resourceType1;
    private ResourceType resourceType2;
    private ResourceType resourceType3;
    private ResourceType resourceType4;
    private ResourceType resourceType5;

    private Resource resource1a;
    private Resource resource1b;
    private Resource resource2a;
    private Resource resource2b;
    private Resource resource3a;
    private Resource resource3b;
    private Resource resource4a;
    private Resource resource4b;
    private Resource resource5a;
    private Resource resource5b;

    private final String KEYA = "keyA";
    private ResourceType resourceTypeA;
    private Resource resourceA_1235; // resource whose ancestors's keys are 1,2,3,5
    private Resource resourceA_1245; // resource whose ancestors's keys are 1,2,4,5

    @BeforeClass
    public void setup() {
        this.resourceType1 = new ResourceType("type1", "plugin", ResourceCategory.SERVER, null);
        this.resourceType2 = new ResourceType("type2", "plugin", ResourceCategory.SERVER, null);
        this.resourceType3 = new ResourceType("type3", "plugin", ResourceCategory.SERVER, null);
        this.resourceType4 = new ResourceType("type4", "plugin", ResourceCategory.SERVER, null);
        this.resourceType5 = new ResourceType("type5", "plugin", ResourceCategory.SERVER, null);
        this.resourceTypeA = new ResourceType("typeA", "plugin", ResourceCategory.SERVER, null);

        this.resource1a = new Resource(KEY1, KEY1, this.resourceType1);
        this.resource2a = new Resource(KEY2, KEY2, this.resourceType2);
        this.resource3a = new Resource(KEY3, KEY3, this.resourceType3);
        this.resource4a = new Resource(KEY4, KEY4, this.resourceType4);
        this.resource5a = new Resource(KEY5, KEY5, this.resourceType5);

        this.resource1b = new Resource(KEY1, KEY1, this.resourceType1);
        this.resource2b = new Resource(KEY2, KEY2, this.resourceType2);
        this.resource3b = new Resource(KEY3, KEY3, this.resourceType3);
        this.resource4b = new Resource(KEY4, KEY4, this.resourceType4);
        this.resource5b = new Resource(KEY5, KEY5, this.resourceType5);

        this.resourceA_1235 = new Resource(KEYA, KEYA, this.resourceTypeA);
        this.resourceA_1235.setParentResource(resource1a);
        this.resource1a.setParentResource(resource2a);
        this.resource2a.setParentResource(resource3a);
        this.resource3a.setParentResource(resource5a);

        this.resourceA_1245 = new Resource(KEYA, KEYA, this.resourceTypeA);
        this.resourceA_1245.setParentResource(resource1b);
        this.resource1b.setParentResource(resource2b);
        this.resource2b.setParentResource(resource4b);
        this.resource4b.setParentResource(resource5b);
    }

    public void testError() {
        try {
            new CanonicalResourceKey(null, null);
            assert false : "should not be allowed to pass null";
        } catch (PluginContainerException ok) {
        }

        try {
            new CanonicalResourceKey(this.resource3b, null);
            assert false : "should not be allowed to pass null";
        } catch (PluginContainerException ok) {
        }

        try {
            new CanonicalResourceKey(null, this.resource4a);
            assert false : "should not be allowed to pass null";
        } catch (PluginContainerException ok) {
        }

        try {
            new CanonicalResourceKey(this.resource3b, this.resource4a);
        } catch (Exception bad) {
            throw new RuntimeException("should not throw error - both resources are valid", bad);
        }
    }

    public void testSimple() throws Exception {
        CanonicalResourceKey key1a = new CanonicalResourceKey(this.resource1a, this.resource1a.getParentResource());
        assert key1a.equals(key1a);
        assert key1a.hashCode() == key1a.hashCode();

        CanonicalResourceKey key1a_dup = new CanonicalResourceKey(this.resource1a, this.resource1a.getParentResource());
        assert key1a.equals(key1a_dup);
        assert key1a.hashCode() == key1a_dup.hashCode();

        CanonicalResourceKey key2a = new CanonicalResourceKey(this.resource2a, this.resource2a.getParentResource());
        assert !key1a.equals(key2a);
        assert key1a.hashCode() != key2a.hashCode();
    }

    public void testAncestorDiff() throws Exception {
        // this test shows that even if a resource and its direct parent are the same as another resource/parent pair, that
        // the equals and hashCode methods will know that the ancestors make each canonical resource key unique
        CanonicalResourceKey key1a = new CanonicalResourceKey(this.resourceA_1235, this.resourceA_1235
            .getParentResource());
        assert key1a.equals(key1a);
        assert key1a.hashCode() == key1a.hashCode();

        CanonicalResourceKey key1a_dup = new CanonicalResourceKey(this.resourceA_1235, this.resourceA_1235
            .getParentResource());
        assert key1a.equals(key1a_dup);
        assert key1a.hashCode() == key1a_dup.hashCode();

        CanonicalResourceKey key2a = new CanonicalResourceKey(this.resourceA_1245, this.resourceA_1245
            .getParentResource());
        assert !key1a.equals(key2a);
        assert key1a.hashCode() != key2a.hashCode();
    }
}
