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
package org.rhq.enterprise.server.perspective.test;

import java.util.List;
import org.testng.annotations.Test;
import org.rhq.core.clientapi.descriptor.perspective.Perspective;
import org.rhq.core.clientapi.descriptor.perspective.Task;
import org.rhq.enterprise.server.perspective.PerspectiveManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * PerspectiveManagerBeanTest
 *
 * @version $Rev$
 */
@Test
public class PerspectiveManagerBeanTest extends AbstractEJB3Test {
    public void testGetPerspectives() {
        PerspectiveManagerLocal pm = LookupUtil.getPerspectiveManager();
        assert (pm != null);
        assert (pm.getAllPerspectives() != null);
        assert (pm.getAllPerspectives().size() > 0);
        for (int i = 0; i < 1000; i++) {
            assert (pm.getAllPerspectives() != null);
        }
    }

    public void testGetPerspective() throws Exception {
        PerspectiveManagerLocal pm = LookupUtil.getPerspectiveManager();

        Perspective p = pm.getPerspective("content");

        assert (p != null);
        assert (p.getName().equals("content"));
    }

    public void testByContext() throws Exception {
        PerspectiveManagerLocal pm = LookupUtil.getPerspectiveManager();

        // Test to make sure we get null for a random string
        assert (pm.getTasks(System.currentTimeMillis() + "", System.currentTimeMillis() + "") == null);

        List<Task> tasks = pm.getTasks("channel", "1", "234", "890234");

        assert (tasks != null);
        assert (tasks.size() > 0);

        Task t = tasks.get(0);
        assert (t.getPath().indexOf("?cid=1") > 0);
    }
}