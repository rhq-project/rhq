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
package org.rhq.core.gui.converter;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

/**
 * @author Ian Springer
 */
public class PropertySimpleValueConverter implements Converter
{
    /**
     * A special value for an input that tells the server-side that the corresponding value should be set to null.
     */
    public static final String NULL_INPUT_VALUE = " ";

    public Object getAsObject(FacesContext context, UIComponent component, String string)
    {
        //noinspection UnnecessaryLocalVariable
        Object object = NULL_INPUT_VALUE.equals(string) ? null : string;
        validateEmptyString(context, component, string);
        return object;
    }

    public String getAsString(FacesContext facesContext, UIComponent component, Object object)
    {
        //noinspection UnnecessaryLocalVariable
        String string = (String)object;
        return string;
    }

    private static void validateEmptyString(FacesContext context, UIComponent component, String string)
    {
        UIInput input = (UIInput)component;
        if (input.isValid() && string != null && string.length() == 0)
        {
            if (input.getValidators() != null)
            {
                for (Validator validator : input.getValidators())
                {
                    try
                    {
                        validator.validate(context, input, string);
                    }
                    catch (ValidatorException ve)
                    {
                        // If the validator throws an exception, we're
                        // invalid, and we need to add a message
                        input.setValid(false);
                        FacesMessage message;
                        String validatorMessageString = input.getValidatorMessage();
                        if (null != validatorMessageString)
                        {
                            message =
                                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                            validatorMessageString,
                                            validatorMessageString);
                            message.setSeverity(FacesMessage.SEVERITY_ERROR);
                        }
                        else
                        {
                            message = ve.getFacesMessage();
                        }
                        if (message != null)
                        {
                            context.addMessage(input.getClientId(context), message);
                        }
                    }
                }
            }
        }
    }
}