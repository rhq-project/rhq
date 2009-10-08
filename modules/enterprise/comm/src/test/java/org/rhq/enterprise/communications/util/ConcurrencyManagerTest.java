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
package org.rhq.enterprise.communications.util;

import java.util.HashMap;
import org.testng.annotations.Test;
import org.rhq.enterprise.communications.util.ConcurrencyManager.Permit;

/**
 * Tests {@link ConcurrencyManager}.
 *
 * @author John Mazzitelli
 */
@Test
public class ConcurrencyManagerTest {
    public void testConcurrencyManager() {
        String foo = "foo";

        HashMap<String, Integer> map = new HashMap<String, Integer>();
        map.put(foo, 5);
        ConcurrencyManager cm = new ConcurrencyManager(map);

        assert null != cm.getPermit(null); // this is allowed and just a no-op
        cm.releasePermit(cm.getPermit(null)); // this is allowed and just a no-op
        cm.releasePermit(null); // this is allowed and just a no-op

        Permit permit = cm.getPermit(foo);
        assert permit != null;
        cm.releasePermit(permit);

        permit = cm.getPermit(foo); // 1
        assert permit != null;
        permit = cm.getPermit(foo); // 2
        assert permit != null;
        permit = cm.getPermit(foo); // 3
        assert permit != null;
        permit = cm.getPermit(foo); // 4
        assert permit != null;
        permit = cm.getPermit(foo); // 5
        assert permit != null;

        try {
            permit = cm.getPermit(foo);
            assert false : "should not have succeeded: " + permit;
        } catch (NotPermittedException e) {
            // this exception is the one that is to be expected
        }

        cm.releasePermit(permit);
        assert null != cm.getPermit(foo);

        ConcurrencyManager cm2 = new ConcurrencyManager(map);
        cm2.releasePermit(permit); // should be ignored since permit wasn't granted by cm2

        return;
    }

    public void testConcurrencyManagerNoLimits() {
        String foo = "foo";

        HashMap<String, Integer> map = new HashMap<String, Integer>();
        map.put(foo, 0);
        ConcurrencyManager cm1 = new ConcurrencyManager(map);

        Permit permit1 = cm1.getPermit(foo);
        assert permit1 != null;
        cm1.releasePermit(permit1);

        map = new HashMap<String, Integer>();
        map.put(foo, -1); // negative, just like it was 0
        ConcurrencyManager cm2 = new ConcurrencyManager(map);

        Permit permit2 = cm2.getPermit(foo);
        assert permit2 != null;
        cm2.releasePermit(permit2);
        cm2.releasePermit(permit1); // a no-op since permit1 wasn't granted by cm2
    }
}