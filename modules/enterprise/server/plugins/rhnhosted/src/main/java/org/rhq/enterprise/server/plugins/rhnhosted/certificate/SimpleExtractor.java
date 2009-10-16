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
package org.rhq.enterprise.server.plugins.rhnhosted.certificate;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.jdom.Element;
import org.jdom.JDOMException; 

/**
 * @author pkilambi
 *
 */
/**
 * SimpleExtractor
 */
class SimpleExtractor implements FieldExtractor {

    private String fieldName;
    private String propertyName;
    private boolean required;

    public SimpleExtractor(String name) {
        this(name, name, false);
    }

    public SimpleExtractor(String fieldName0, String propertyName0) {
        this(fieldName0, propertyName0, false);
    }

    public SimpleExtractor(String name, boolean required0) {
        this(name, name, required0);
    }

    /**
     * 
     */
    public SimpleExtractor(String fieldName0, String propertyName0, boolean required0) {
        fieldName = fieldName0;
        propertyName = propertyName0;
        required = required0;
    }

    /**
     * {@inheritDoc}
     * @throws JDOMException
     */
    public void extract(Certificate target, Element field) throws JDOMException {
        if (!PropertyUtils.isWriteable(target, propertyName)) {
            throw new JDOMException("Property " + propertyName +
                    " is not writable in target " + target);
        }

        try {
            BeanUtils.setProperty(target, propertyName, field.getTextTrim());
        }
        catch (IllegalAccessException e) {
            throw new JDOMException("Could not set value of property " + propertyName, e);
        }
        catch (InvocationTargetException e) {
            throw new JDOMException("Could not set value of property " + propertyName, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * {@inheritDoc}
     */
    public String getFieldName() {
        return fieldName;
    }

}
