/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
package org.rhq.modules.plugins.wildfly10.json;

import java.io.Serializable;

import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import org.rhq.modules.plugins.wildfly10.json.serializer.PropertyValueDeserializer;
import org.rhq.modules.plugins.wildfly10.json.serializer.PropertyValueSerializer;

/**
 * @author Heiko W. Rupp
 */
@JsonSerialize(using = PropertyValueSerializer.class)
@JsonDeserialize(using = PropertyValueDeserializer.class)
public class PROPERTY_VALUE implements Serializable{

    private String key;
    private String value;

    public PROPERTY_VALUE(String key, String value) {
        if (key!=null) {
            this.key = key.intern();
        }
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public void setKey(String key) {
        if (key!=null) {
            this.key = key.intern();
        }
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "PROPERTY_VALUE{" +
                "key='" + key + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
