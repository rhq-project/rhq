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

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author John Sanda
 */
public class CsvWriter<T> {

    public final static String DELIMITER = ",";

    public final PropertyConverter<T> SIMPLE_CONVERTER = new SimpleConverter<T>();

    public final PropertyConverter<T> DATE_CONVERTER = new DateConverter<T>();

    private Map<String, PropertyConverter<T>> converters = new LinkedHashMap<String, PropertyConverter<T>>();

    public void setColumns(String... columns) {
        for (String column : columns) {
            converters.put(column, SIMPLE_CONVERTER);
        }
    }

    public void setPropertyConverter(String propertyName, PropertyConverter<T> propertyConverter) {
        if (!converters.containsKey(propertyName)) {
            throw new CsvWriterException(propertyName + " is not a registered property name");
        }
        converters.put(propertyName, propertyConverter);
    }

    public void write(T object, OutputStream outputStream) throws IOException {
        StringBuilder buffer = new StringBuilder();
        for (String property : converters.keySet()) {
            PropertyConverter<T> converter = converters.get(property);
            Object value = converter.convert(object, property);
            if (value == null) {
                buffer.append(DELIMITER);
            } else {
                buffer.append(ReportFormatHelper.quoteIfInvalidCharacters(value.toString())).append(DELIMITER);
            }
        }
        buffer.delete(buffer.length() - 1, buffer.length()).append("\n");
        outputStream.write(buffer.toString().getBytes());
    }

}
