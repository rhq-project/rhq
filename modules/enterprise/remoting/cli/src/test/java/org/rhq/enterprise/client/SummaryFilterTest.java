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
import org.rhq.core.domain.util.Summary;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.beans.PropertyDescriptor;
import java.beans.BeanInfo;
import java.beans.Introspector;

public class SummaryFilterTest {

    @Test
    public void filterShouldReturnTrueForIdProperty() throws Exception {
        SummaryFilter filter = new SummaryFilter();
        PropertyDescriptor idProperty = getPropertyDescriptor("id");

        assertTrue(filter.filter(idProperty), "Filter should return true for id property.");
    }

    @Test
    public void filterShouldReturnTrueForSummaryProperty() throws Exception {
        SummaryFilter filter = new SummaryFilter();
        PropertyDescriptor summaryProperty = getPropertyDescriptor("summaryField");

        assertTrue(filter.filter(summaryProperty), "Filter should return true for property of a @Summary field.");
    }

    @Test
    public void filterShouldReturnFalseForNonSummaryProperty() throws Exception {
        SummaryFilter filter = new SummaryFilter();
        PropertyDescriptor nonSummaryProperty = getPropertyDescriptor("nonSummaryField");

        assertFalse(filter.filter(nonSummaryProperty), "Filter should return false for property of a field that does " +
            "not have the @Summary annotation");
    }

    @Test
    public void filterShouldReturnFalseForPropertyThatDoesNotReferToAField() throws Exception {
        SummaryFilter filter = new SummaryFilter();
        PropertyDescriptor notAField = getPropertyDescriptor("nonField");

        assertFalse(filter.filter(notAField), "Filter should return false for a property that does not directly " +
                "correspond to a field");
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
        private int id;

        @Summary
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
