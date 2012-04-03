/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation version 2 of the License.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program; if not, write to the Free Software
 *  * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package org.rhq.enterprise.server.rest.reporting;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.PropertyUtils;

/**
* @author John Sanda
*/
class SimpleConverter<T> implements PropertyConverter<T> {
    @Override
    public Object convert(T object, String propertyName) {
        try {
            return PropertyUtils.getProperty(object, propertyName);
        } catch (IllegalAccessException e) {
            throw new CsvWriterException("Unable to get property value " + object.getClass().getName() + "." +
                propertyName + " for " + object, e);
        } catch (InvocationTargetException e) {
            throw new CsvWriterException("Unable to get property value " + object.getClass().getName() + "." +
                propertyName + " for " + object, e);
        } catch (NoSuchMethodException e) {
            throw new CsvWriterException("Unable to get property value " + object.getClass().getName() + "." +
                propertyName + " for " + object, e);
        }
    }
}
