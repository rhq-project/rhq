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
package org.rhq.core.gui.util;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.el.ELException;
import javax.faces.context.FacesContext;
import javax.faces.FacesException;

/**
 * A set of utility methods for working with EL {@link javax.el.Expression}s within a JSF application.
 *
 * @author Ian Springer
 */
public abstract class FacesExpressionUtility {
    public static ValueExpression createValueExpression(String expression, Class<?> expectedType) {
        ValueExpression valueExpression = getExpressionFactory().createValueExpression(getELContext(), expression,
            expectedType);
        return valueExpression;
    }

    public static <T> T getValue(ValueExpression valueExpression, Class<T> expectedType) {
        try {
            return (T) valueExpression.getValue(getELContext());
        }
        catch (ELException e) {
            throw new FacesException(e);
        }
    }

    public static <T> T getValue(String expressionString, Class<T> expectedType) {
        return getValue(createValueExpression(expressionString, expectedType), expectedType);
    }

    public static void setValue(ValueExpression valueExpression, Object value) {
        valueExpression.setValue(getELContext(), value);
    }

    public static MethodExpression createMethodExpression(String expression, Class<?> expectedReturnType,
        Class<?>[] expectedParamTypes) {
        ExpressionFactory expressionFactory = getExpressionFactory();
        return expressionFactory.createMethodExpression(getELContext(), expression, expectedReturnType,
            expectedParamTypes);
    }

    private static ExpressionFactory getExpressionFactory() {
        ExpressionFactory expressionFactory = FacesContext.getCurrentInstance().getApplication().getExpressionFactory();
        return expressionFactory;
    }

    private static ELContext getELContext() {
        return FacesContext.getCurrentInstance().getELContext();
    }
}