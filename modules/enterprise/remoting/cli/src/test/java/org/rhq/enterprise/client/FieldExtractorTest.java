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

package org.rhq.enterprise.client;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

import java.lang.reflect.Field;

public class FieldExtractorTest {

    @Test
    public void primitivesShouldBeReturnedUnchanged() throws Exception {
        TestBean src = new TestBean();
        FieldExtractor extractor = new FieldExtractor();

        Field field = getField(src, "myInt");

        String expected = "1";
        String actual = extractor.getValue(field, src);

        assertEquals(actual, expected, "Failed to get value for an int");
    }

    @Test
    public void toStringValueShouldBeReturnedForObjectField() throws Exception {
        TestBean src = new TestBean();
        FieldExtractor extractor = new FieldExtractor();

        Field field = getField(src, "myTestBean");

        String expected = "ANOTHER TEST BEAN";
        String actual = extractor.getValue(field, src);

        assertEquals(actual, expected, "Failed to return correct String value for a field that is an object and not a primitive");
    }

    @Test
    public void theStringNullShouldBeReturnedForObjectFieldThatIsNull() throws Exception {
        TestBean src = new TestBean();
        src.setMyString(null);
        FieldExtractor extractor = new FieldExtractor();

        Field field = getField(src, "myString");

        String expected = "null";
        String actual = extractor.getValue(field, src);

        assertEquals(actual, expected, "The string 'null' should be returned when a field is null");
    }

    Field getField(Object src, String fieldName) throws Exception {
        return src.getClass().getDeclaredField(fieldName);
    }

    static class TestBean {
        private int myInt = 1;

        private String myString = "string";

        private AnotherTestBean myTestBean = new AnotherTestBean();

        public int getMyInt() {
            return myInt;
        }

        public void setMyInt(int myInt) {
            this.myInt = myInt;
        }

        public String getMyString() {
            return myString;
        }

        public void setMyString(String myString) {
            this.myString = myString;
        }        
    }

    static class AnotherTestBean {
        @Override
        public String toString() {
            return "ANOTHER TEST BEAN";
        }
    }

}
