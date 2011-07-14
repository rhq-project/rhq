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

package org.rhq.bindings.util;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

import org.rhq.bindings.util.SummaryFilter;
import org.rhq.core.domain.util.Summary;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.beans.PropertyDescriptor;
import java.beans.BeanInfo;
import java.beans.Introspector;

public class SummaryFilterTest {

    @Test
    public void testFilterAndReturnOrder() throws Exception {
        SummaryFilter filter = new SummaryFilter();
        PropertyDescriptor[] properties = filter.getPropertyDescriptors(new TestEntity(),false);

        assert (properties.length == 2);
        assert (properties[0].getName().equals("id"));
        assert (properties[1].getName().equals("summaryField"));


    }

    PropertyDescriptor getPropertyDescriptor(String name) throws Exception {
        BeanInfo beanInfo = Introspector.getBeanInfo(TestEntity.class);
        for (PropertyDescriptor descriptor : beanInfo.getPropertyDescriptors()) {
            if (descriptor.getName().equals(name)) {
                return descriptor;
            }
        }
        return null;
    }

    @Entity
    static class TestEntity {
        @Id
        @Summary(index=1)
        private int id;

        @Summary(index=2)
        private String summaryField;

        private String nonSummaryField;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getSummaryField() {
            return summaryField;
        }

        public void setSummaryField(String summaryField) {
            this.summaryField = summaryField;
        }

        public String getNonSummaryField() {
            return nonSummaryField;
        }

        public void setNonSummaryField(String nonSummaryField) {
            this.nonSummaryField = nonSummaryField;
        }

        public String getNonField() {
            return null;
        }
    }

}
