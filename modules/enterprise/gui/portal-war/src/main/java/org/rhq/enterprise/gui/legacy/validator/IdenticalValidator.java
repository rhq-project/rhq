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
/*
 * PasswordValidtor.java
 */

package org.rhq.enterprise.gui.legacy.validator;

import javax.servlet.http.HttpServletRequest;
import org.apache.commons.validator.Field;
import org.apache.commons.validator.GenericValidator;
import org.apache.commons.validator.ValidatorAction;
import org.apache.commons.validator.util.ValidatorUtils;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.validator.Resources;

/**
 * This class is for use in the commons validation package to validate two identical fields.
 */
public class IdenticalValidator extends BaseValidator {
    /**
     * Validates if two fields are equal in terms of String's equal() function. Example of use: <code><field
     * property="password" depends="identical"> <arg0 key="password.displayName"/> <arg1
     * key="passwordConfirm.displayName"/> <var><var-name>secondProperty</var-name> <var-value>password2</var-value>
     * </var> </field></code>
     *
     * @return Returns true if the fields property and property2 are identical.
     */
    public boolean validate(Object bean, ValidatorAction va, Field field, ActionMessages msgs,
        HttpServletRequest request) {
        String value = ValidatorUtils.getValueAsString(bean, field.getProperty());
        String sProperty2 = field.getVarValue("secondProperty");
        String value2 = ValidatorUtils.getValueAsString(bean, sProperty2);

        if (GenericValidator.isBlankOrNull(value)) {
            if (GenericValidator.isBlankOrNull(value2)) {
                return true;
            }

            return false;
        }

        if (!value.equals(value2)) {
            ActionMessage msg = Resources.getActionMessage(request, va, field);
            msgs.add(field.getKey(), msg);
            return false;
        }

        return true;
    }
}