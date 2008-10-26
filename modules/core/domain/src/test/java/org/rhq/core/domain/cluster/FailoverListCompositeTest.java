/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.domain.cluster;

import java.util.ArrayList;

import org.testng.annotations.Test;

import org.rhq.core.domain.cluster.composite.FailoverListComposite;
import org.rhq.core.domain.cluster.composite.FailoverListComposite.ServerEntry;

@Test
public class FailoverListCompositeTest {
    public void testEquals() {
        ArrayList<ServerEntry> servers = new ArrayList<ServerEntry>();
        ArrayList<ServerEntry> servers2 = new ArrayList<ServerEntry>();

        FailoverListComposite list = new FailoverListComposite(servers);
        FailoverListComposite list2 = new FailoverListComposite(servers2);
        assert list != null;
        assert list.equals(list2);

        servers.add(new FailoverListComposite.ServerEntry("addr1", 111, 222));
        list = new FailoverListComposite(servers);
        assert !list.equals(list2);
        assert !list2.equals(list);

        servers2.add(new FailoverListComposite.ServerEntry("addr1", 111, 222));
        list2 = new FailoverListComposite(servers2);
        assert list.equals(list2);
        assert list2.equals(list);

        servers.add(new FailoverListComposite.ServerEntry("addr2.com", 7777, 12345));
        list = new FailoverListComposite(servers);
        assert !list.equals(list2);
        assert !list2.equals(list);

        servers2.add(new FailoverListComposite.ServerEntry("addr2.com", 7777, 12345));
        list2 = new FailoverListComposite(servers2);
        assert list.equals(list2);
        assert list2.equals(list);

        servers.add(new FailoverListComposite.ServerEntry("addrX.com", 7777, 12345));
        servers.add(new FailoverListComposite.ServerEntry("addrY.com", 7, 1));
        list = new FailoverListComposite(servers);
        assert !list.equals(list2);
        assert !list2.equals(list);

        // reverse X and Y - lists should not be equal because the order isn't the same
        servers2.add(new FailoverListComposite.ServerEntry("addrY.com", 7, 1));
        servers2.add(new FailoverListComposite.ServerEntry("addrX.com", 7777, 12345));
        list2 = new FailoverListComposite(servers2);
        assert !list.equals(list2);
        assert !list2.equals(list);
    }

    public void testTextForm() {
        ArrayList<ServerEntry> servers = new ArrayList<ServerEntry>();
        FailoverListComposite listToTest;
        ServerEntry server;

        FailoverListComposite list = new FailoverListComposite(servers);
        String text = list.writeAsText();
        assert text != null;
        assert text.length() == 0 : text.length();
        listToTest = FailoverListComposite.readAsText(text);
        assert listToTest != null;
        assert listToTest.size() == 0 : listToTest.size();

        servers.add(new FailoverListComposite.ServerEntry("addr1", 111, 222));
        list = new FailoverListComposite(servers);
        text = list.writeAsText();
        assert text != null;
        assert text.equals("addr1:111/222") : text;
        listToTest = FailoverListComposite.readAsText(text);
        assert listToTest != null;
        assert listToTest.size() == 1 : listToTest.size();
        server = listToTest.next();
        assert server != null;
        assert server.address.equals("addr1") : server;
        assert server.port == 111 : server;
        assert server.securePort == 222 : server;

        servers.add(new FailoverListComposite.ServerEntry("addr2.com", 7777, 12345));
        list = new FailoverListComposite(servers);
        text = list.writeAsText();
        assert text != null;
        assert text.equals("addr1:111/222\naddr2.com:7777/12345") : text;
        listToTest = FailoverListComposite.readAsText(text);
        assert listToTest != null;
        assert listToTest.size() == 2 : listToTest.size();
        server = listToTest.next();
        assert server != null;
        assert server.address.equals("addr1") : server;
        assert server.port == 111 : server;
        assert server.securePort == 222 : server;
        server = listToTest.next();
        assert server != null;
        assert server.address.equals("addr2.com") : server;
        assert server.port == 7777 : server;
        assert server.securePort == 12345 : server;

        servers.add(new FailoverListComposite.ServerEntry("another.host.net", 80, 90));
        list = new FailoverListComposite(servers);
        text = list.writeAsText();
        assert text != null;
        assert text.equals("addr1:111/222\naddr2.com:7777/12345\nanother.host.net:80/90") : text;
        listToTest = FailoverListComposite.readAsText(text);
        assert listToTest != null;
        assert listToTest.size() == 3 : listToTest.size();
        server = listToTest.next();
        assert server != null;
        assert server.address.equals("addr1") : server;
        assert server.port == 111 : server;
        assert server.securePort == 222 : server;
        server = listToTest.next();
        assert server != null;
        assert server.address.equals("addr2.com") : server;
        assert server.port == 7777 : server;
        assert server.securePort == 12345 : server;
        server = listToTest.next();
        assert server != null;
        assert server.address.equals("another.host.net") : server;
        assert server.port == 80 : server;
        assert server.securePort == 90 : server;
    }
}
