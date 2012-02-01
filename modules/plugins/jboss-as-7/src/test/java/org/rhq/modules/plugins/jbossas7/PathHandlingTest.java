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
package org.rhq.modules.plugins.jbossas7;

import org.testng.annotations.Test;

import org.rhq.modules.plugins.jbossas7.json.Address;

/**
 * Test handling of path elements.
 * A path to a resource consists of pairs type=value that are
 * separated by commas
 * @author Heiko W. Rupp
 */
@Test
public class PathHandlingTest {

    public void emptyAddress() throws Exception {

        Address a = new Address();
        assert a.getPath() != null;
        assert a.getPath().isEmpty();
    }

    public void emptyAddress2() throws Exception {

        Address a = new Address((String)null);
        assert a.getPath() != null;
        assert a.getPath().isEmpty();
        assert a.isEmpty();
        assert a.size()==0;
    }

    public void addressPath1() throws Exception {
        String path = "subsystem=jms,profile=default,queue=foo";
        Address a = new Address("/"+path);
        assert a.getPath().equals(path);
    }

    public void addressPath2() throws Exception {
        String path = "subsystem=jms,profile=default,queue=foo";
        Address a = new Address(path);
        assert a.getPath().equals(path);
        assert a.size()==3 : "Size was not 3, but "+ a.size();
        assert !a.isEmpty();
    }

    public void getParent1() throws Exception {
        String path = "subsystem=jms,profile=default,queue=foo";
        Address a = new Address(path);
        Address b = a.getParent();
        assert b!=null;
        assert b.size()==2;
        assert b.get(0).equals("subsystem=jms");
        assert b.get(1).equals("profile=default");
    }

    public void getParent2() throws Exception {
        Address a = new Address();
        Address b = a.getParent();
        assert b!=null;
        assert b.isEmpty();
        assert b.size()==0;
    }

    public void getParent3() throws Exception {
        Address a = new Address("foo=bar");
        Address b = a.getParent();
        assert b!=null;
        assert b.isEmpty();
        assert b.size()==0;
    }

}
