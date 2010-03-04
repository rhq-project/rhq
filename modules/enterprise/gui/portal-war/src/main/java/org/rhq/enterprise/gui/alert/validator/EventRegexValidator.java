/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.alert.validator;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.faces.Validator;

@Name("eventRegexValidator")
@Validator
public class EventRegexValidator implements javax.faces.validator.Validator {

    @In
    private AlertValidatorUtil validatorUtil;

    public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        String stringValue = (String)value;

        if (stringValue != null && stringValue.length() > 0) {
            try {
                Pattern.compile(stringValue);
            } catch (PatternSyntaxException e) {
                validatorUtil.templateError("alert.config.error.InvalidEventDetails");
            }
        }
    }
}