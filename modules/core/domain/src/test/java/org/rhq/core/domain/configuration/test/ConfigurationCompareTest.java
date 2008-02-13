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
package org.rhq.core.domain.configuration.test;

import java.util.ArrayList;
import java.util.HashMap;
import javax.persistence.EntityManager;
import org.testng.annotations.Test;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.test.AbstractEJB3Test;

/**
 * Tests the ability to compare configurations with one another.
 *
 * @author John Mazzitelli
 */
@Test
public class ConfigurationCompareTest extends AbstractEJB3Test {
    public void testNullCompare() {
        Configuration c1 = new Configuration();
        Configuration c2 = new Configuration();
        c2.getProperties().clear();

        assert c1.equals(c1); // matches itself
        assert c1.equals(c2); // null config matches an empty config
        assert c2.equals(c1);
        c1.put(new PropertySimple("one", null));
        c2.put(new PropertySimple("one", null));
        assert c1.equals(c2); // null property matches a null property
        assert c2.equals(c1);

        PropertyList list1 = new PropertyList("listA");
        list1.setList(new ArrayList<Property>());
        c1.put(list1);
        PropertyList list2 = new PropertyList("listA");
        list2.setList(null);
        c2.put(list2);
        assert c1.equals(c2); // empty list is the same as a null list
        assert c2.equals(c1);

        list1.add(new PropertySimple("x", "y"));
        assert !c1.equals(c2);
        assert !c2.equals(c1);

        list2.add(new PropertySimple("x", "y"));
        assert c1.equals(c2);
        assert c2.equals(c1);

        PropertyMap map1 = new PropertyMap("mapB");
        map1.setMap(new HashMap<String, Property>());
        c1.put(map1);
        PropertyMap map2 = new PropertyMap("mapB");
        map1.setMap(null);
        c2.put(map2);
        assert c1.equals(c2); // empty map is the same as a null map
        assert c2.equals(c1);

        map1.put(new PropertySimple("m", "n"));
        assert !c1.equals(c2);
        assert !c2.equals(c1);

        map2.put(new PropertySimple("m", "n"));
        assert c1.equals(c2);
        assert c2.equals(c1);

        map1.put(list1);
        map2.put(list2);
        assert c1.equals(c2); // innerList is empty, so its same as a null
        assert c2.equals(c1);
        list1.add(new PropertySimple("x", "z"));
        assert !c1.equals(c2);
        assert !c2.equals(c1);
    }

    @Test(groups = "integration.ejb3")
    public void testPersistedNullCompare() throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();

        try {
            Configuration c1 = new Configuration();
            Configuration c2 = new Configuration();

            em.persist(c2);

            c2.getProperties().clear();

            em.flush();
            em.refresh(c2);

            assert c1.equals(c1); // matches itself
            assert c1.equals(c2); // null config matches an empty config
            assert c2.equals(c1);
            c1.put(new PropertySimple("one", null));
            c2.put(new PropertySimple("one", null));
            assert c1.equals(c2); // null property matches a null property
            assert c2.equals(c1);

            PropertyList list1 = new PropertyList("listA");
            list1.setList(new ArrayList<Property>());
            c1.put(list1);
            PropertyList list2 = new PropertyList("listA");
            list2.setList(null);
            c2.put(list2);

            em.flush();
            em.refresh(c2);

            assert c1.equals(c2); // empty list is the same as a null list
            assert c2.equals(c1);

            list1.add(new PropertySimple("x", "y"));
            assert !c1.equals(c2);
            assert !c2.equals(c1);

            list2.add(new PropertySimple("x", "y"));
            em.flush();
            em.refresh(c2);
            assert c1.equals(c2);
            assert c2.equals(c1);

            PropertyMap map1 = new PropertyMap("mapB");
            map1.setMap(new HashMap<String, Property>());
            c1.put(map1);
            PropertyMap map2 = new PropertyMap("mapB");
            map1.setMap(null);
            c2.put(map2);
            em.flush();
            em.refresh(c2);
            assert c1.equals(c2); // empty map is the same as a null map
            assert c2.equals(c1);

            map1.put(new PropertySimple("m", "n"));
            assert !c1.equals(c2);
            assert !c2.equals(c1);

            map2.put(new PropertySimple("m", "n"));
            em.flush();
            em.refresh(c2);
            assert c1.equals(c2);
            assert c2.equals(c1);

            map1.put(list1);
            map2.put(list2);
            em.flush();
            em.refresh(c2);
            assert c1.equals(c2); // innerList is empty, so its same as a null
            assert c2.equals(c1);
            list1.add(new PropertySimple("x", "z"));
            assert !c1.equals(c2);
            assert !c2.equals(c1);
            list2.add(new PropertySimple("x", "z"));
            em.flush();
            em.refresh(c2);

            assert c1.equals(c2);
            assert c2.equals(c1);

            c1.put(new PropertySimple("n1", ""));
            c2.put(new PropertySimple("n1", ""));
            em.flush();
            em.refresh(c2);
            assert c1.equals(c2);
            assert c2.equals(c1);

            c1.put(new PropertySimple("n1", null));
            c2.put(new PropertySimple("n1", null));
            em.flush();
            em.refresh(c2);
            assert c1.equals(c2);
            assert c2.equals(c1);
        } finally {
            getTransactionManager().rollback();
        }
    }

    public void testSimpleCompare() {
        Configuration c1 = new Configuration();
        Configuration c2 = new Configuration();
        assert c1.equals(c1); // can compare itself
        assert c1.equals(c2);
        assert c2.equals(c1);

        c1.put(new PropertySimple("one", "111"));
        assert c1.equals(c1); // can compare itself
        assert !c1.equals(c2);
        assert !c2.equals(c1);

        c2.put(new PropertySimple("two", "222"));
        assert !c1.equals(c2);
        assert !c2.equals(c1);

        c2.put(new PropertySimple("one", "111"));
        c1.put(new PropertySimple("two", "222"));
        assert c1.equals(c1); // can compare itself
        assert c2.equals(c2); // can compare itself
        assert c1.equals(c2);
        assert c2.equals(c1);
    }

    public void testMapCompare() {
        Configuration c1 = new Configuration();
        Configuration c2 = new Configuration();

        c1.put(new PropertyMap("mapA", new Property[] { new PropertySimple("uno", "111") }));
        c2.put(new PropertyMap("mapA", new Property[] { new PropertySimple("dos", "222") }));

        assert c1.equals(c1); // can compare itself
        assert c2.equals(c2); // can compare itself
        assert !c1.equals(c2);
        assert !c2.equals(c1);

        c1.getMap("mapA").put(new PropertySimple("dos", "222"));
        c2.getMap("mapA").put(new PropertySimple("uno", "111"));
        assert c1.equals(c1); // can compare itself
        assert c2.equals(c2); // can compare itself
        assert c1.equals(c2);
        assert c2.equals(c1);
    }

    public void testMapInMapCompare() {
        Configuration c1 = new Configuration();
        Configuration c2 = new Configuration();

        PropertyMap mapInner1 = new PropertyMap("inner", new Property[] { new PropertySimple("uno", 111) });
        PropertyMap mapInner2 = new PropertyMap("inner", new Property[] { new PropertySimple("uno", 111) });
        PropertyMap mapOuter1 = new PropertyMap("outer", new Property[] { mapInner1 });
        PropertyMap mapOuter2 = new PropertyMap("outer", new Property[] { mapInner2 });

        c1.put(mapOuter1);
        c2.put(mapOuter2);

        assert c1.equals(c1); // can compare itself
        assert c2.equals(c2); // can compare itself
        assert c1.equals(c2);
        assert c2.equals(c1);

        c1.getMap("outer").getMap("inner").put(new PropertySimple("foo", "bar"));
        assert c1.equals(c1); // can compare itself
        assert !c1.equals(c2);
        assert !c2.equals(c1);
    }

    public void testListCompare() {
        Configuration c1 = new Configuration();
        Configuration c2 = new Configuration();

        c1.put(new PropertyList("listA", new Property[] { new PropertySimple("foo", "111") }));
        c2.put(new PropertyList("listA", new Property[] { new PropertySimple("foo", "111") }));

        assert c1.equals(c1); // can compare itself
        assert c2.equals(c2); // can compare itself
        assert c1.equals(c2);
        assert c2.equals(c1);

        c2.getList("listA").add(new PropertySimple("foo", "222"));
        assert c2.equals(c2); // can compare itself
        assert !c1.equals(c2);
        assert !c2.equals(c1);
    }

    public void testListInListCompare() {
        Configuration c1 = new Configuration();
        Configuration c2 = new Configuration();

        PropertyList listInner1 = new PropertyList("inner", new Property[] { new PropertySimple("uno", 111) });
        PropertyList listInner2 = new PropertyList("inner", new Property[] { new PropertySimple("uno", 111) });
        PropertyList listOuter1 = new PropertyList("outer", new Property[] { listInner1 });
        PropertyList listOuter2 = new PropertyList("outer", new Property[] { listInner2 });

        c1.put(listOuter1);
        c2.put(listOuter2);

        assert c1.equals(c1); // can compare itself
        assert c2.equals(c2); // can compare itself
        assert c1.equals(c2);
        assert c2.equals(c1);

        ((PropertyList) (c1.getList("outer").getList().get(0))).add(new PropertySimple("uno", 222));
        assert c1.equals(c1); // can compare itself
        assert !c1.equals(c2);
        assert !c2.equals(c1);
    }

    public void testMapAndListCompare() {
        Configuration c1 = new Configuration();
        Configuration c2 = new Configuration();

        PropertyList listInner1 = new PropertyList("inner", new Property[] { new PropertySimple("uno", 111) });
        PropertyList listInner2 = new PropertyList("inner", new Property[] { new PropertySimple("uno", 111) });
        PropertyMap mapOuter1 = new PropertyMap("outer", new Property[] { listInner1,
            new PropertySimple("hello", "world!") });
        PropertyMap mapOuter2 = new PropertyMap("outer", new Property[] { listInner2,
            new PropertySimple("hello", "world!") });

        c1.put(mapOuter1);
        c2.put(mapOuter2);

        assert c1.equals(c1); // can compare itself
        assert c2.equals(c2); // can compare itself
        assert c1.equals(c2);
        assert c2.equals(c1);

        c1.getMap("outer").getList("inner").add(new PropertySimple("uno", "bar"));
        assert c1.equals(c1); // can compare itself
        assert !c1.equals(c2);
        assert !c2.equals(c1);
    }
}