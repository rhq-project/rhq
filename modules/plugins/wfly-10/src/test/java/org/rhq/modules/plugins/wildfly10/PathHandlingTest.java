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
package org.rhq.modules.plugins.wildfly10;

import org.testng.annotations.Test;

import org.rhq.modules.plugins.wildfly10.json.Address;

/**
 * Test handling of path elements.
 * A path to a resource consists of pairs type=value that are
 * separated by commas
 * @author Heiko W. Rupp
 */
@Test(groups = "unit")
public class PathHandlingTest {

    public void emptyAddress() throws Exception {

        Address a = new Address();
        assert a.getPath() != null;
        assert a.getPath().isEmpty();
    }

    public void emptyAddress2() throws Exception {

        Address a = new Address((String) null);
        assert a.getPath() != null;
        assert a.getPath().isEmpty();
        assert a.isEmpty();
        assert a.size() == 0;
    }

    public void addressPath1() throws Exception {
        String path = "subsystem=jms,profile=default,queue=foo";
        Address a = new Address("/" + path);
        assert a.getPath().equals(path);
    }

    public void addressPath2() throws Exception {
        String path = "subsystem=jms,profile=default,queue=foo";
        Address a = new Address(path);
        assert a.getPath().equals(path);
        assert a.size() == 3 : "Size was not 3, but " + a.size();
        assert !a.isEmpty();
    }

    public void getParent1() throws Exception {
        String path = "subsystem=jms,profile=default,queue=foo";
        Address a = new Address(path);
        Address b = a.getParent();
        assert b != null;
        assert b.size() == 2;
        assert b.get(0).equals("subsystem=jms");
        assert b.get(1).equals("profile=default");
    }

    public void getParent2() throws Exception {
        Address a = new Address();
        Address b = a.getParent();
        assert b != null;
        assert b.isEmpty();
        assert b.size() == 0;
    }

    public void getParent3() throws Exception {
        Address a = new Address("foo=bar");
        Address b = a.getParent();
        assert b != null;
        assert b.isEmpty();
        assert b.size() == 0;
    }

    /** Same as parent1 but tests that '/' separator is also supported and not just ','.
     *  Address creation from path.
     * 
     * @throws Exception
     */
    public void getParent4() throws Exception {
        String path = "/subsystem=modcluster,mod-cluster-config=configuration/";
        Address a = new Address(path);
        //extract parent portion. Penultimate.
        Address b = a.getParent();
        assert a.size() == 2 : "A.size was not 2, but " + a.size();
        assert b != null;
        assert b.size() == 1 : "B.size was not 1, but " + b.size();
        assert b.get(0).equals("subsystem=modcluster");
    }

    public void pathWithColon() throws Exception {
        String path = "subsystem=mail,jndi=java:/mail";
        Address a = new Address(path);
        assert a.size() == 2: "A.size was not 2, but " + a.size();

        Address b = a.getParent();
        assert b.size() == 1: "B.size was not 1, but " + b.size();
        assert b.get(0).equals("subsystem=mail");

        String segment = a.get(1);
        assert !segment.isEmpty();
        assert segment.equals("jndi=java:/mail") : "Segment is " + segment;
    }

    public void pathWithColon2() throws Exception {
//        String path = "subsystem=mail,jndi=java:/mail";
        Address a = new Address();
        a.addSegment("subsystem=mail");
        a.addSegment("jndi=java:/mail");
        assert a.size() == 2: "A.size was not 2, but " + a.size();

        Address b = a.getParent();
        assert b.size() == 1: "B.size was not 1, but " + b.size();
        assert b.get(0).equals("subsystem=mail");

        String segment = a.get(1);
        assert !segment.isEmpty();
        assert segment.equals("jndi=java:/mail") : "Segment is " + segment;
    }

    public void pathWithColon3() throws Exception {
//        String path = "subsystem=mail,jndi=java:/mail";
        Address a = new Address();
        a.add("subsystem","mail");
        a.add("jndi","java:/mail");
        assert a.size() == 2: "A.size was not 2, but " + a.size();

        Address b = a.getParent();
        assert b.size() == 1: "B.size was not 1, but " + b.size();
        assert b.get(0).equals("subsystem=mail");

        String segment = a.get(1);
        assert !segment.isEmpty();
        assert segment.equals("jndi=java:/mail") : "Segment is " + segment;
    }


    public void pathWithSpecial2() throws Exception {
        String path = "subsystem=mail,jndi=java:\"mail";
        Address a = new Address(path);
        assert a.size() == 2: "A.size was not 2, but " + a.size();

        Address b = a.getParent();
        assert b.size() == 1: "B.size was not 1, but " + b.size();
        assert b.get(0).equals("subsystem=mail");

        String segment = a.get(1);
        assert !segment.isEmpty();
        assert segment.equals("jndi=java:\"mail") : "Segment is " + segment;
    }



}
