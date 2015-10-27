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
package org.rhq.bindings.output;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import au.com.bytecode.opencsv.CSVWriter;

import org.rhq.bindings.util.LazyLoadScenario;
import org.rhq.bindings.util.ShortOutput;
import org.rhq.bindings.util.SummaryFilter;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.ResourceAvailability;
import org.rhq.core.domain.resource.ResourceType;

/**
 * @author Greg Hinkle
 */
public class TabularWriter {

    private static final String CSV = "csv";

    String[] headers;
    int[] maxColumnLength;
    boolean[] noShrinkColumns;
    int width = 160;
    PrintWriter out;
    private String format = "raw";
    private CSVWriter csvWriter;
    private SummaryFilter summaryFilter = new SummaryFilter();
    boolean exportMode;
    boolean hideRowCount;

    static Set<String> IGNORED_PROPS = new HashSet<String>();

    static {
        IGNORED_PROPS.add("mtime");
        IGNORED_PROPS.add("ctime");
        IGNORED_PROPS.add("itime");
        IGNORED_PROPS.add("uuid");
        IGNORED_PROPS.add("parentResource");
    }

    static Set<Class> SIMPLE_TYPES = new HashSet<Class>();

    static {
        SIMPLE_TYPES.add(Byte.class);
        SIMPLE_TYPES.add(Byte.TYPE);
        SIMPLE_TYPES.add(Character.class);
        SIMPLE_TYPES.add(Character.TYPE);
        SIMPLE_TYPES.add(Short.class);
        SIMPLE_TYPES.add(Short.TYPE);
        SIMPLE_TYPES.add(Integer.class);
        SIMPLE_TYPES.add(Integer.TYPE);
        SIMPLE_TYPES.add(Long.class);
        SIMPLE_TYPES.add(Long.TYPE);
        SIMPLE_TYPES.add(Float.class);
        SIMPLE_TYPES.add(Float.TYPE);
        SIMPLE_TYPES.add(Double.class);
        SIMPLE_TYPES.add(Double.TYPE);
        SIMPLE_TYPES.add(Boolean.class);
        SIMPLE_TYPES.add(Boolean.TYPE);
        SIMPLE_TYPES.add(String.class);
    }

    public TabularWriter(PrintWriter out, String... headers) {
        this.headers = headers;
        this.out = out;
    }

    public TabularWriter(PrintWriter out) {
        this.out = out;
    }

    public TabularWriter(PrintWriter out, String format) {
        this.out = out;
        this.format = format;

        if (CSV.equals(format)) {
            csvWriter = new CSVWriter(out);
        }
    }

    public void setHideRowCount(boolean hideRowCount) {
        this.hideRowCount = hideRowCount;
    }

    public void print(Object object) {

        if (object == null) {
            this.out.println("null");
            return;
        }

        if (object instanceof Map) {
            printMap((Map) object);
            return;
        }

        if (object instanceof Collection) {
            printCollection((Collection) object);
            return;
        }

        if (object instanceof Configuration) {
            printConfiguration((Configuration) object);
            return;
        }

        if (object instanceof String[][]) {
            printMultidimensionalStringArray((String[][])object);
            return;
        }

        if (object != null && object.getClass().isArray()) {
            if (!object.getClass().getComponentType().isPrimitive()) {
                printArray((Object[]) object);
            } else {
                Class<?> oClass = object.getClass();
                // note: we assume single-dimension arrays!
                out.println("Array of " + (oClass.getComponentType().getName()));
                if (oClass == byte[].class) {
                    for (byte i : (byte[]) object) {
                        this.out.println(i);
                    }
                } else if (oClass == short[].class) {
                    for (short i : (short[]) object) {
                        this.out.println(i);
                    }
                } else if (oClass == int[].class) {
                    for (int i : (int[]) object) {
                        this.out.println(i);
                    }
                } else if (oClass == long[].class) {
                    for (long i : (long[]) object) {
                        this.out.println(i);
                    }
                } else if (oClass == char[].class) {
                    for (char i : (char[]) object) {
                        this.out.println(i);
                    }
                } else if (oClass == float[].class) {
                    for (float i : (float[]) object) {
                        this.out.println(i);
                    }
                } else if (oClass == double[].class) {
                    for (double i : (double[]) object) {
                        this.out.println(i);
                    }
                } else if (oClass == boolean[].class) {
                    for (boolean i : (boolean[]) object) {
                        this.out.println(i);
                    }
                } else {
                    this.out.println("*Printing of this data type is not supported*");
                }
            }
            return;
        }

        try {

            if (SIMPLE_TYPES.contains(object.getClass())) {
                this.out.println(String.valueOf(object));
                return;
            }

            out.println(object.getClass().getSimpleName() + ":");
            Map<String, PropertyInfo> properties = new LinkedHashMap<String, PropertyInfo>();
            int maxLength = 0;

            for (PropertyDescriptor pd : summaryFilter.getPropertyDescriptors(object, exportMode)) {
                Method m = pd.getReadMethod();
                Object val = null;
                if (m != null) {
                    val = invoke(object, m);
                }

                if (val == null) {
                    maxLength = Math.max(maxLength, pd.getName().length());
                    properties.put(pd.getName(), new PropertyInfo(pd.getName(), null));
                } else {
                    try {
                        String str = shortVersion(val);
                        maxLength = Math.max(maxLength, pd.getName().length());
                        properties.put(pd.getName(), new PropertyInfo(str, pd.getPropertyType()));
                    } catch (Exception e) {
                    }
                }
            }

            for (String key : properties.keySet()) {
                printProperty(key, properties.get(key), maxLength);
            }

        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IntrospectionException e) {
            e.printStackTrace();
        }

    }

    private static class PropertyInfo {
        String value;

        Class<?> type;

        PropertyInfo(String propertyValue, Class<?> propertyType) {
            value = propertyValue;
            type = propertyType;
        }
    }

    private void printProperty(String name, PropertyInfo propertyInfo, int maxLength) {
        out.print("\t");
        printPreSpaced(out, name, maxLength);
        out.print(": ");

        if (propertyInfo.type == null) {
            out.println("");
        } else if (exportMode || !String.class.equals(propertyInfo.type)) {
            out.println(propertyInfo.value);
        } else {
            out.println(abbreviate(propertyInfo.value, width - 12 - maxLength));
        }
    }

    // This method is taken verbatim from the Commons Lang project in the org.apache.commons.lang.StringUtils class
    // TODO Should this method go into one our StringUtil classes?
    private String abbreviate(String string, int maxWidth) {
        int offset = 0;

        if (string == null) {
            return null;
        }

        if (maxWidth < 4) {
            throw new IllegalArgumentException("Minimum abbreviation width is 4");
        }

        if (string.length() <= maxWidth) {
            return string;
        }

        if (offset > string.length()) {
            offset = string.length();
        }

        if ((string.length() - offset) < (maxWidth - 3)) {
            offset = string.length() - (maxWidth - 3);
        }

        if (offset <= 4) {
            return string.substring(0, maxWidth - 3) + "...";
        }

        if (maxWidth < 7) {
            throw new IllegalArgumentException("Minimum abbreviation width with offset is 7");
        }

        if ((offset + (maxWidth - 3)) < string.length()) {
            return "..." + abbreviate(string.substring(offset), maxWidth - 3);
        }

        return "..." + string.substring(string.length() - (maxWidth - 3));
    }

    public void printMap(Map map) {

        String[][] data = new String[map.size()][];
        int i = 0;
        for (Object key : map.keySet()) {
            Object value = map.get(key);
            data[i] = new String[2];
            data[i][0] = shortVersion(key);
            data[i][1] = shortVersion(value);
            i++;
        }
        this.headers = new String[] { "Key", "Value" };
        printArray(data);
    }

    public void printCollection(Collection list) {
        // List of arbitrary objects
        if (list == null || list.size() == 0) {
            if (!hideRowCount) {
                out.println("0 rows");
            }
        } else if (list.size() == 1 && !CSV.equals(format)) {
            if (!hideRowCount) {
                out.println("one row");
            }

            print(list.iterator().next());
        } else {
            String[][] data;

            if (!allOneType(list)) {
                printStrings(list);
            } else {

                Object firstObject = list.iterator().next();
                try {

                    if (firstObject instanceof String) {
                        headers = new String[] { "Value" };
                        data = new String[list.size()][1];
                        int i = 0;
                        for (Object object : list) {
                            data[i++][0] = (String) object;
                        }
                        this.printArray(data);
                    } else {

                        if (consistentMaps(list)) {
                            // results printed

                        } else {

                            int i = 0;

                            List<PropertyDescriptor> pdList = new ArrayList<PropertyDescriptor>();
                            for (PropertyDescriptor pd : summaryFilter.getPropertyDescriptors(firstObject, exportMode)) {
                                try {
                                    boolean allNull = true;
                                    for (Object row : list) {
                                        Method m = pd.getReadMethod();
                                        Object val = null;
                                        if (m != null) {
                                            val = invoke(row, pd.getReadMethod());
                                        }
                                        if ((val != null && !(val instanceof Collection))
                                            || ((val != null && (val instanceof Collection) && !((Collection) val)
                                                .isEmpty())))
                                            allNull = false;
                                    }
                                    if (!allNull && !IGNORED_PROPS.contains(pd.getName())) {
                                        pdList.add(pd);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            if (pdList.isEmpty()) {
                                printStrings(list);
                            } else {
                                headers = new String[pdList.size()];
                                data = new String[list.size()][pdList.size()];

                                for (PropertyDescriptor pd : pdList) {
                                    headers[i++] = pd.getName();
                                }
                                i = 0;
                                for (Object row : list) {
                                    int j = 0;
                                    for (PropertyDescriptor pd : pdList) {

                                        Object val = "?";
                                        val = invoke(row, pd.getReadMethod());
                                        if (val == null) {
                                            data[i][j++] = "";
                                        } else {
                                            data[i][j++] = shortVersion(val);
                                        }
                                    }
                                    i++;
                                }

                                this.printArray(data);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace(); //To change body of catch statement use File | Settings | File Templates.
                } finally {
                    headers = null;
                }
            }

        }

    }

    private boolean consistentMaps(Collection list) {
        List<String> keys = null;
        String[][] data = new String[list.size()][];
        int i = 0;
        for (Object row : list) {
            if (!(row instanceof Map) && !(row instanceof PropertyMap)) {
                return false;
            }
            if (keys == null) {
                keys = new ArrayList<String>();

                //insert check for PropertyMap as they are not instances of Map 
                if (row instanceof PropertyMap) {
                    //TODO: spinder 1/15/10. PropertyMap is arbitrarily complex. Only serialize simple props?
                    for (Object key : ((PropertyMap) row).getMap().keySet()) {
                        String headerKey = stringValueOf(key);
                        keys.add(headerKey);
                    }
                } else {//else row is a Map
                    for (Object key : ((Map) row).keySet()) {
                        String headerKey = stringValueOf(key);
                        keys.add(headerKey);
                    }
                }
                //conditionally put order on the headers to mimic core gui listing style/order
                //Ex. Pid   Name    Size    User Time       Kernel Time
                if (keys.contains("pid")) {
                    String[] processAttribute = { "pid", "name", "size", "userTime", "kernelTime" };
                    List<String> newKeyOrder = new ArrayList<String>();
                    for (String attribute : processAttribute) {
                        newKeyOrder.add(attribute);
                        keys.remove(attribute);
                    }
                    //postpend remaining keys if any to the newHeader list
                    for (String key : keys) {
                        newKeyOrder.add(key);
                    }
                    keys = newKeyOrder;
                }
            }

            data[i] = new String[keys.size()];
            if (row instanceof PropertyMap) {
                for (String key : keys) {
                    if (!keys.contains(stringValueOf(key))) {
                        return false;
                    }
                    data[i][keys.lastIndexOf(stringValueOf(key))] = shortVersion(((PropertyMap) row).get(String
                        .valueOf(key)));
                }
            } else {//else row is a Map
                for (Object key : ((Map) row).keySet()) {
                    if (!keys.contains(stringValueOf(key))) {
                        return false;
                    }
                    data[i][keys.lastIndexOf(stringValueOf(key))] = shortVersion(((Map) row).get(key));
                }
            }
            i++;
        }

        if (keys != null) {
            headers = keys.toArray(new String[keys.size()]);
            printArray(data);
            return true;
        } else {
            return false;
        }

    }

    public void printConfiguration(Configuration config) {
        out.println("Configuration [" + config.getId() + "] - " + config.getNotes());
        for (PropertySimple p : config.getSimpleProperties().values()) {
            print(p, 1);
        }
        for (PropertyList p : config.getListProperties().values()) {
            print(p, 1);
        }
        for (PropertyMap p : config.getMapProperties().values()) {
            print(p, 1);
        }

    }

    public void print(PropertySimple p, int depth) {
        out.println(indent(depth) + p.getName() + " = " + p.getStringValue());
    }

    public void print(PropertyList p, int depth) {
        out.println(indent(depth) + p.getName() + " [" + p.getList().size() + "] {");
        if (p.getList().size() > 0 && p.getList().get(0) instanceof PropertyMap) {
            consistentMaps(p.getList());

        } else {
            for (Property entry : p.getList()) {
                if (entry instanceof PropertySimple) {
                    print((PropertySimple) entry, depth + 1);
                } else if (entry instanceof PropertyMap) {
                    print((PropertyMap) entry, depth + 1);
                }
            }
        }

        out.println(indent(depth) + "}");
    }

    public void print(PropertyMap p, int depth) {
        out.println(indent(depth) + p.getName() + " [" + p.getMap().size() + "] {");
        for (String key : p.getMap().keySet()) {
            Property entry = p.getMap().get(key);
            if (entry instanceof PropertySimple) {
                print((PropertySimple) entry, depth + 1);
            } else if (entry instanceof PropertyMap) {
                print((PropertyMap) entry, depth + 1);
            }
        }
        out.println(indent(depth) + "}");
    }

    private String indent(int x) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < x; i++) {
            buf.append("  ");
        }
        return buf.toString();
    }

    private void printStrings(Collection list) {
        for (Object object : list) {
            out.println(stringValueOf(object));
        }
    }

    private boolean allOneType(Collection list) {
        Class lastClass = null;
        for (Object object : list) {
            if (lastClass == null) {
                lastClass = object.getClass();
            } else if (!object.getClass().equals(lastClass)) {
                return false;
            }
        }
        return true;
    }

    public void printArray(Object[] data) {
        if (data == null || data.length == 0) {
            if (!hideRowCount) {
                out.println("0 rows");
            }
            return;
        }
        out.println("Array of " + (data.getClass().getComponentType().getName()));

        printCollection(Arrays.asList(data));
    }

    private void resizeColumns(int[] actualColumnWidths, int maxColumnWidth, List<Integer> columns) {
        int extraSpace = 0;

        Iterator<Integer> iterator = columns.iterator();
        while (iterator.hasNext()) {
            int col = iterator.next();

            if (actualColumnWidths[col] < maxColumnWidth) {
                extraSpace += maxColumnWidth - actualColumnWidths[col];
                iterator.remove();
            }
        }

        if (extraSpace == 0) {
            // There is no extra space with which to work so at this point we have to
            // truncate any columns that still need more space
            for (Integer col : columns) {
                actualColumnWidths[col] = maxColumnWidth;
            }
        } else if (columns.size() == 0) {
            // If the columns list is empty then that means that there is enough available
            // space for each column so we are done.
            return;
        } else if (extraSpace > 0) {
            // Since we have extra space, we will go ahead and recalculate the widths for
            // those columns still needing space
            int newMaxColumnWidth = (maxColumnWidth + extraSpace) / columns.size();
            resizeColumns(actualColumnWidths, newMaxColumnWidth, columns);
        }
    }

    public void printMultidimensionalStringArray(String[][] data) {

        if (data == null || data.length == 0) {
            if (!hideRowCount) {
                out.println("0 rows");
            }
            return;
        }

        int numberOfColumns = data[0].length;
        int maxColumnWidth = width / numberOfColumns;
        int[] actualColumnWidths = new int[numberOfColumns];

        for (String[] row : data) {
            for (int col = 0; col < row.length; ++col) {
                if (row[col] == null) {
                    row[col] = "";
                }
                actualColumnWidths[col] = Math.max(actualColumnWidths[col], row[col].length());
            }
        }

        if (headers != null) {
            for (int col = 0; col < headers.length; ++col) {
                actualColumnWidths[col] = Math.max(actualColumnWidths[col], headers[col].length());
            }
        }

        List<Integer> columns = new LinkedList();
        for (int col = 0; col < actualColumnWidths.length; ++col) {
            columns.add(col);
        }
        resizeColumns(actualColumnWidths, maxColumnWidth, columns);

        if (headers != null) {
            if (CSV.equals(format)) {
                csvWriter.writeNext(headers);
            } else {
                for (int i = 0; i < actualColumnWidths.length; i++) {
                    int colSize = actualColumnWidths[i];
                    printSpaced(out, headers[i], colSize);
                    if (i < actualColumnWidths.length - 1) {
                        out.print(" ");
                    }
                }

                out.println("");

                for (int i = 1; i < width; i++) {
                    out.print("-");
                }
            }
            out.println("");

        }

        if (CSV.equals(format)) {
            for (String[] row : data) {
                csvWriter.writeNext(row);
            }
        } else {
            for (String[] row : data) {
                for (int i = 0; i < actualColumnWidths.length; i++) {
                    int colSize = actualColumnWidths[i];

                    printSpaced(out, row[i], colSize);
                    if (i < actualColumnWidths.length - 1) {
                        out.print(" ");
                    }
                }
                out.println("");
            }
        }

        if (!hideRowCount) {
            out.print(data.length + " rows");
            out.println("");
        }
    }

    private void printSpaced(PrintWriter out, String data, int length) {
        int dataLength = data.length();
        if (dataLength > length) {
            out.print(data.substring(0, length));
        } else {
            out.print(data);

            for (int i = dataLength; i < length; i++) {
                out.print(" ");
            }
        }

    }

    private void printPreSpaced(PrintWriter out, String data, int length) {
        int dataLength = data.length();
        if (dataLength > length) {
            out.print(data.substring(0, length));
        } else {
            for (int i = dataLength; i < length; i++) {
                out.print(" ");
            }
            out.print(data);
        }

    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public boolean isExportMode() {
        return exportMode;
    }

    public void setExportMode(boolean exportMode) {
        this.exportMode = exportMode;
    }

    private static String shortVersion(Object object) {

        if (object instanceof ShortOutput) {
            return ((ShortOutput) object).getShortOutput();
        } else if (object instanceof PropertySimple) {
            return ((PropertySimple) object).getStringValue();
        } else if (object instanceof ResourceType) {
            return ((ResourceType) object).getName();
        } else if (object instanceof ResourceAvailability) {
            AvailabilityType availType = ((ResourceAvailability) object).getAvailabilityType();
            return (availType == null) ? "?" : availType.getName();
        } else if (object != null && object.getClass().isArray()) {
            return Arrays.toString((Object[]) object);
        } else {
            return stringValueOf(object);
        }
    }

    private static Object invoke(Object o, Method m) throws IllegalAccessException, InvocationTargetException {
        boolean access = m.isAccessible();
        m.setAccessible(true);
        try {
            LazyLoadScenario.setShouldLoad(false);

            return m.invoke(o);
        } catch (Exception e) {
            // That's fine
            return null;
        } finally {
            LazyLoadScenario.setShouldLoad(true);
            m.setAccessible(access);
        }
    }

    private static String stringValueOf(Object object) {
        try {
            LazyLoadScenario.setShouldLoad(false);
            return String.valueOf(object);
        } catch (Exception e) {
            return "null";
        } finally {
            LazyLoadScenario.setShouldLoad(true);
        }
    }
}
