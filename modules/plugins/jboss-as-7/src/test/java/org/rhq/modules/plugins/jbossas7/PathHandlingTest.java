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

import java.util.List;

import org.testng.annotations.Test;

import org.rhq.modules.plugins.jbossas7.json.PROPERTY_VALUE;

/**
 * Test handling of path elements.
 * A path to a resource consists of pairs type=value that are
 * separated by commas
 * @author Heiko W. Rupp
 */
@Test
public class PathHandlingTest {

    public void buildPath1() throws Exception {

        String path = "/subsystem=jms,profile=default,queue=foo";

        BaseComponent bc = new BaseComponent();
        List<PROPERTY_VALUE> list = bc.pathToAddress(path);

        int found = 0;
        for (PROPERTY_VALUE p : list) {
            if (p.getKey().equals("subsystem")) {
                assert p.getValue().equals("jms");
                found++;
            }
            if (p.getKey().equals("queue")) {
                found++;
                assert p.getValue().equals("foo");
            }
        }
        assert found == 2 : "did not find both keys, but " + found;
    }

    public void buildPath2() throws Exception {

        String path = "/subsystem=jms,profile=default,queue=java:/foo";

        BaseComponent bc = new BaseComponent();
        List<PROPERTY_VALUE> list = bc.pathToAddress(path);

        assert list.size()==3 : "We don't have 3 components, but " + list.size();
        for (PROPERTY_VALUE p : list) {
            if (p.getKey().equals("subsystem"))
                assert p.getValue().equals("jms");
            if (p.getKey().equals("queue"))
                assert p.getValue().equals("java:/foo") : "Queue value is wrong: " + p.getValue();

        }
    }

    public void buildIncomplete() throws Exception {

        String path = "/subsystem=jms,profile=default,queue=java:/foo,topic";

        BaseComponent bc = new BaseComponent();
        List<PROPERTY_VALUE> list = bc.pathToAddress(path);

        assert list.size()==3 : "We don't have 3 components, but " + list.size();
        int found = 0;
        for (PROPERTY_VALUE p : list) {
            if (p.getKey().equals("subsystem")) {
                assert p.getValue().equals("jms");
                found++;
            }
            if (p.getKey().equals("queue")) {
                assert p.getValue().equals("java:/foo") : "Queue value is wrong: " + p.getValue();
                found++;
            }
        }
        assert found == 2 : "did not find both keys, but " + found;
    }
}
