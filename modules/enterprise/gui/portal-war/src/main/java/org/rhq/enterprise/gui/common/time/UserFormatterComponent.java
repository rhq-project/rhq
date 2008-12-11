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
package org.rhq.enterprise.gui.common.time;

import java.util.Arrays;

import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;

import org.rhq.core.gui.util.FacesComponentUtility;

/**
 * @author Joseph Marques
 */
public class UserFormatterComponent extends UIComponentBase {
    /*
     * currently, this class only formats dates and times according to the formatted
     * strings setup in the user's web preferences, but it is intended to be a general
     * purpose class that can be leveraged to format anything that we want to be able
     * to give user-level control over.
     */

    public enum Type {
        DATE, //
        TIME, //
        DATETIME;
    }

    public final static String VALUE_ATTRIBUTE = "value";
    public final static String TYPE_ATTRIBUTE = "type";
    public final static String OPTIONS_ATTRIBUTE = "options";

    private Long value;
    private String type;
    private String options;

    public long getValue() {
        if (value == null) {
            value = FacesComponentUtility.getExpressionAttribute(this, VALUE_ATTRIBUTE, Long.class);
        }
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public String getType() {
        if (type == null) {
            type = FacesComponentUtility.getExpressionAttribute(this, TYPE_ATTRIBUTE, String.class);
        }
        if (type == null) {
            type = Type.DATETIME.toString(); // default if not specified
        }
        return type;
    }

    public Type getEnumType() {
        return Type.valueOf(getType());
    }

    public void setType(String type) {
        type = type.toUpperCase();
        Type.valueOf(type); // let IllegalArgumentException bubble out
        this.type = type;
    }

    public String getOptions() {
        if (options == null) {
            options = FacesComponentUtility.getExpressionAttribute(this, OPTIONS_ATTRIBUTE, String.class);
        }
        if (options != null) {
            options = options.toLowerCase(); // normalize
        } else {
            options = ""; // default if not specified, prevents callers from having to deal with null
        }
        return options;
    }

    public boolean isLocal() {
        return Arrays.asList(getOptions().split(",")).contains("local");
    }

    public void setOptions(String options) {
        this.options = options;
    }

    public static final String COMPONENT_TYPE = "org.jboss.on.UserFormatter";
    public static final String COMPONENT_FAMILY = "org.jboss.on.UserFormatter";

    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    public Object saveState(FacesContext facesContext) {
        Object[] state = new Object[4];
        state[0] = super.saveState(facesContext);
        state[1] = this.value;
        state[2] = this.type;
        state[3] = this.options;
        return state;
    }

    public void restoreState(FacesContext context, Object stateValues) {
        Object[] state = (Object[]) stateValues;
        super.restoreState(context, state[0]);
        this.value = (Long) state[1];
        this.type = (String) state[2];
        this.options = (String) state[3];
    }

}
