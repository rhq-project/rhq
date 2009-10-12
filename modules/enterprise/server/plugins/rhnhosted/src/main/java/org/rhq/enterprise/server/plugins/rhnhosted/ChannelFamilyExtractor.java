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
package org.rhq.enterprise.server.plugins.rhnhosted;

import org.jdom.Element;
/**
 * @author pkilambi
 *
 */
/**
 * ChannelFamilyExtractor
 */
class ChannelFamilyExtractor implements FieldExtractor {

    private String fieldName;

    /**
     * 
     */
    public ChannelFamilyExtractor(String fieldName0) {
        fieldName = fieldName0;
    }

    /**
     * {@inheritDoc}
     */
    public void extract(Certificate target, Element field) {
        String quantity = field.getAttributeValue("quantity");
        String family = field.getAttributeValue("family");
        ChannelFamilyDescriptor cf = new ChannelFamilyDescriptor(family, quantity);
        target.addChannelFamily(cf);
    }

    public boolean isRequired() {
        return false;
    }

    public String getFieldName() {
        return fieldName;
    }

}
