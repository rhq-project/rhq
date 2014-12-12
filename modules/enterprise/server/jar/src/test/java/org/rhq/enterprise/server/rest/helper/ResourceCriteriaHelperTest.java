/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
package org.rhq.enterprise.server.rest.helper;

import javax.ws.rs.core.MultivaluedMap;

import org.testng.annotations.Test;

import org.jboss.resteasy.specimpl.MultivaluedMapImpl;

import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.enterprise.server.rest.BadArgumentException;

@Test
public class ResourceCriteriaHelperTest {

    @Test(expectedExceptions = { BadArgumentException.class }, expectedExceptionsMessageRegExp = ".*does not exist.*")
    public void testInvalidParam() {
        MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
        params.putSingle("foo", "bar");
        ResourceCriteriaHelper.create(params);
    }

    @Test(expectedExceptions = { BadArgumentException.class }, expectedExceptionsMessageRegExp = ".*inventoryStatus is bad.*")
    public void testEnumParamInvalidValue() {
        MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
        params.putSingle("inventoryStatus", "FOO");
        ResourceCriteriaHelper.create(params);
    }

    @Test(expectedExceptions = { BadArgumentException.class }, expectedExceptionsMessageRegExp = ".*is not valid.*")
    public void testNumericParamInvalidValue() {
        MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
        params.putSingle("id", "FOO");
        ResourceCriteriaHelper.create(params);
    }

    public void testSpecialParamValue() {
        MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
        params.putSingle("strict", "FOO");
        assert ResourceCriteriaHelper.create(params).isStrict() == false;
        params.clear();
        params.putSingle("strict", "true");
        assert ResourceCriteriaHelper.create(params).isStrict() == true;
    }

    public void testNumericParamValue() {
        MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
        params.putSingle("id", "1234");
        params.putSingle("startItime", "2");
        assert ResourceCriteriaHelper.create(params) != null;
    }

    public void testEnumParamValue() {
        MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
        params.putSingle("inventoryStatus", "NEW");
        assert ResourceCriteriaHelper.create(params) != null;
    }

    public void testStringParamValue() {
        MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
        params.putSingle("name", "1234");
        assert ResourceCriteriaHelper.create(params) != null;
    }

    public void testStringParamWithoutValue() {
        MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
        params.put("name", null);
        assert ResourceCriteriaHelper.create(params) != null;
    }

    public void testParamShortcuts() {
        MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
        params.putSingle("status", "NEW");
        params.putSingle("availability", "up");
        params.putSingle("type", "FOO");
        params.putSingle("category", ResourceCategory.SERVER.getName());
        params.putSingle("plugin", "FOO");
        params.putSingle("type", "FOO");
        params.putSingle("parentName", "FOO");
        params.putSingle("parentId", "1");
        assert ResourceCriteriaHelper.create(params) != null;
    }
}
