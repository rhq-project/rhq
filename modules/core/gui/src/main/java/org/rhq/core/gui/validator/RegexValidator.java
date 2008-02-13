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
package org.rhq.core.gui.validator;

import javax.faces.application.FacesMessage;
import javax.faces.component.StateHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

/**
 * A validator that validates that the given value matches a regular expression.
 *
 * @author Mark Spritzler
 */
public class RegexValidator implements Validator, StateHolder {
    private static final String FAILED_VALIDATION = "Invalid value - does not match regular expression: ";

    private String regex;
    private boolean isTransient;

    // A public no-arg constructor is required by the JSF spec.
    public RegexValidator() {
    }

    public RegexValidator(String regex) {
        this.regex = regex;
    }

    public void validate(FacesContext facesContext, UIComponent component, Object value) throws ValidatorException {
        if (value != null) {
            String stringValue = value.toString();
            if (!stringValue.matches(this.regex)) {
                throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR, FAILED_VALIDATION
                    + this.regex, null));
            }
        }
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public boolean isTransient() {
        return isTransient;
    }

    public void restoreState(FacesContext facesContext, Object object) {
        Object[] values = (Object[]) object;
        this.regex = (String) values[0];
    }

    public Object saveState(FacesContext facesContext) {
        Object[] values = new Object[1];
        values[0] = this.regex;
        return (values);
    }

    public void setTransient(boolean isTransient) {
        this.isTransient = isTransient;
    }
}