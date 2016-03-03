/*
 * RHQ Management Platform
 * Copyright (C) 2005-2015 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.modules.plugins.wildfly10.json;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.modules.plugins.wildfly10.ASConnection;

/**
 * @author lzoubek
 * Expression resovler can be used for possibly resolving given object value returned by {@link Result}. Expression resolver detects, 
 * if given object is expression and if so, resolves it (by asking AS7 server).
 */
public class ExpressionResolver {

    private static final Log LOG = LogFactory.getLog(ExpressionResolver.class);
    private final ASConnection connection;
    private final Address address;

    public ExpressionResolver(ASConnection connection, Address address) {
        this.connection = connection;
        this.address = address;
    }

    /**
     * @see ExpressionResolver#resolve(Object)
     */
    public Boolean getBoolean(Object value) {
        Object resolved = resolve(value);
        if (resolved == null) {
            return null;
        }
        if (resolved instanceof Boolean) {
            return (Boolean) resolved;
        }
        return Boolean.valueOf(resolved.toString());
    }

    /**
     * @see ExpressionResolver#resolve(Object)
     */
    public Integer getInteger(Object value) {
        Object resolved = resolve(value);
        if (resolved == null) {
            return null;
        }
        if (resolved instanceof Integer) {
            return (Integer) resolved;
        }
        return Integer.valueOf(resolved.toString());
    }

    /**
     * @see ExpressionResolver#resolve(Object)
     */
    public String getString(Object value) {
        return (String) resolve(value);
    }

    /**
     * Resolves given value if it is an expression (it must be a map with exactly 1 key called 'EXPRESSION_VALUE')
     * @param value to be resolved
     * @return resolved value or unchanged value if value is not an expression or failed to resolve
     */
    @SuppressWarnings("rawtypes")
    public Object resolve(Object value) {
        if (value == null) {
            return value;
        }
        if (value instanceof Map) {
            Map map = (Map) value;
            if (map.size() == 1) {
                String expression = (String) map.get("EXPRESSION_VALUE");
                if (expression != null) {
                    ResolveExpression resolveExpressionOperation = new ResolveExpression(expression, this.address);
                    Result result = connection.execute(resolveExpressionOperation);
                    if (!result.isSuccess()) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Could not resolve expression value " + value + " due to error "
                                + result.getFailureDescription());
                        }
                        return value;
                    }
                    return result.getResult();
                }
            }
        }
        return value;
    }
}
