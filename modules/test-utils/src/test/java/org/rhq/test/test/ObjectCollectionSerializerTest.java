package org.rhq.test.test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.testng.annotations.Test;

import org.rhq.test.ObjectCollectionSerializer;

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

/**
 * 
 *
 * @author Lukas Krejci
 */
@Test
public class ObjectCollectionSerializerTest {

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class MyClass {
        public int id;
        public String name;

        public List<MyChildClass> children;

        public MyClass() {
            
        }
        
        public MyClass(int id, String name) {
            this.id = id;
            this.name = name;
            children = new ArrayList<MyChildClass>();
        }

        public int hashCode() {
            return id * (name == null ? 1 : name.hashCode());
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            
            if (!(o instanceof MyClass)) {
                return false;
            }
            
            MyClass other = (MyClass) o;
            
            if (id != other.id) {
                return false;
            }
            
            if (name == null) {
                if (other.name != null) {
                    return false;
                }
            } else {
                if (!name.equals(other.name)) {
                    return false;
                }
            }
            
            return children.equals(other.children);
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class MyChildClass {
        public int id;
        public String name;

        public MyChildClass() {
            
        }
        
        public MyChildClass(int id, String name, MyClass parent) {
            this.id = id;
            this.name = name;
            parent.children.add(this);
        }
        
        public int hashCode() {
            return id * (name == null ? 1 : name.hashCode());
        }
        
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            
            if (!(o instanceof MyChildClass)) {
                return false;
            }
            
            MyChildClass other = (MyChildClass) o;
            
            if (id != other.id) {
                return false;
            }
            
            if (name == null) {
                return other.name == null;
            } else {
                return name.equals(other.name);
            }
        }
    }

    private List<MyClass> getTestObjectTree() {
        MyClass m1 = new MyClass(1, "m1");
        new MyChildClass(1, "m1c1", m1);
        new MyChildClass(2, "m1c2", m1);
        new MyChildClass(3, "m1c3", m1);

        MyClass m2 = new MyClass(2, "m2");
        new MyChildClass(1, "m2c1", m2);
        new MyChildClass(2, "m2c2", m2);
        new MyChildClass(3, "m2c3", m2);

        return Arrays.asList(m1, m2);
    }

    private byte[] getSerializedTestObjectTree() throws IOException, JAXBException {
        ObjectCollectionSerializer serializer = new ObjectCollectionSerializer();

        serializer.addObjects(getTestObjectTree());

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        serializer.serialize(out);

        return out.toByteArray();
    }

    @Test
    public void testCanSerialize() throws Exception {
        byte[] out = getSerializedTestObjectTree();

        assert out.length > 0 : "The serialization should have produced some output";
    }

    @Test
    public void testCanDeserialize() throws Exception {
        ObjectCollectionSerializer serializer = new ObjectCollectionSerializer();

        ByteArrayInputStream in = new ByteArrayInputStream(getSerializedTestObjectTree());

        List<?> objects = serializer.deserialize(in);

        assert objects != null && objects.equals(getTestObjectTree()) : "The deserialized objects don't match the expected results";
    }
}
