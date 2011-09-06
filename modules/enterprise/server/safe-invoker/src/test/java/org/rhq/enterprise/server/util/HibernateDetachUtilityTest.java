/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.enterprise.server.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import org.testng.annotations.Test;

import org.rhq.enterprise.server.util.HibernateDetachUtility.SerializationType;

@Test
public class HibernateDetachUtilityTest {
    /**
     * This tests the rare, but very possible, condition where two objects
     * have the same identity hashcode (System.identityHashCode(A) == System.identityHashCode(B))
     * but are not identical objects (A != B). We have seen this as a valid condition
     * on both SUN and IBM JRE implementations. 
     */
    public void testIdenticalHashCodesNonIdenticalObjects() throws Exception {
        class ArrayObject implements Serializable {
            private static final long serialVersionUID = 1L;
            long id;
            Integer[] array;
            ArrayObject object; // needed for self-referencing

            ArrayObject(long key, ArrayObject obj, Integer... members) {
                id = key;
                object = obj;
                array = members;
            }

            @Override
            public String toString() {
                StringBuilder builder = new StringBuilder();
                builder.append("ArrayObject [id=").append(id).append(", object=").append(
                    (object == null) ? "null" : object.id).append(", array=").append(Arrays.toString(array))
                    .append("]");
                return builder.toString();
            }

        }

        class DuplicateHashCodeGenerator implements HibernateDetachUtility.HashCodeGenerator {
            public Integer getHashCode(Object value) {
                if (value instanceof Integer && ((Integer) value).intValue() >= 2) {
                    return 11111;
                } else if (value instanceof ArrayObject) {
                    return 22222;
                } else {
                    return System.identityHashCode(value);
                }
            }
        }

        ArrayObject array1 = new ArrayObject(1, null, 1, 2, 3);
        ArrayObject array2 = new ArrayObject(2, array1, 2, 3);
        array1.object = array2; // now 1 references 2 and 2 reference 1, circular dependency
        ArrayObject array3 = new ArrayObject(3, array2, 3);
        array3.object = array3;
        ArrayList<ArrayObject> array = new ArrayList<ArrayObject>(3);
        array.add(array1);
        array.add(array2);
        array.add(array3);

        // SANITY CHECK - make sure our setup is as we expect it to be
        assert array.get(0).array[0] == Integer.valueOf(1);
        assert array.get(0).array[1] == Integer.valueOf(2);
        assert array.get(0).array[2] == Integer.valueOf(3);
        assert array.get(1).array[0] == Integer.valueOf(2);
        assert array.get(1).array[1] == Integer.valueOf(3);
        assert array.get(2).array[0] == Integer.valueOf(3);

        // make sure array1 still references array2 and vice versa
        assertObjectEquals(array.get(0).object, array.get(1));
        assertObjectEquals(array.get(1).object, array.get(0));

        // make sure array3 still self-references
        assertObjectEquals(array.get(2).object, array.get(2));

        // simulate rare condition
        HibernateDetachUtility.hashCodeGenerator = new DuplicateHashCodeGenerator();
        HibernateDetachUtility.nullOutUninitializedFields(array, SerializationType.SERIALIZATION);
        assert array.get(0).array[0] == Integer.valueOf(1);
        assert array.get(0).array[1] == Integer.valueOf(2);
        assert array.get(0).array[2] == Integer.valueOf(3);
        assert array.get(1).array[0] == Integer.valueOf(2);
        assert array.get(1).array[1] == Integer.valueOf(3);
        assert array.get(2).array[0] == Integer.valueOf(3);

        // make sure array1 still references array2 and vice versa
        assertObjectEquals(array.get(0).object, array.get(1));
        assertObjectEquals(array.get(1).object, array.get(0));

        // make sure array3 still self-references
        assertObjectEquals(array.get(2).object, array.get(2));

        // put it back the way it was
        HibernateDetachUtility.hashCodeGenerator = new HibernateDetachUtility.SystemHashCodeGenerator();
        HibernateDetachUtility.nullOutUninitializedFields(array, SerializationType.SERIALIZATION);
        assert array.get(0).array[0] == Integer.valueOf(1);
        assert array.get(0).array[1] == Integer.valueOf(2);
        assert array.get(0).array[2] == Integer.valueOf(3);
        assert array.get(1).array[0] == Integer.valueOf(2);
        assert array.get(1).array[1] == Integer.valueOf(3);
        assert array.get(2).array[0] == Integer.valueOf(3);

        // make sure array1 still references array2 and vice versa
        assertObjectEquals(array.get(0).object, array.get(1));
        assertObjectEquals(array.get(1).object, array.get(0));

        // make sure array3 still self-references
        assertObjectEquals(array.get(2).object, array.get(2));
    }

    private void assertObjectEquals(Object o1, Object o2) {
        assert o1 == o2 : "Object [" + o1 + "] does not equal object [" + o2 + "]";
    }
}