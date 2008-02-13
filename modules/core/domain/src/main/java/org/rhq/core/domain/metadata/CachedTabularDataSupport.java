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
package org.rhq.core.domain.metadata;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Date;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

public class CachedTabularDataSupport extends TabularDataSupport implements Externalizable {
    public CachedTabularDataSupport(TabularType tabularType) {
        super(tabularType);
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        TabularType tt = getTabularType();

        //out.writeUTF(tt.getClass().getName());
        out.writeLong(size());

        CompositeType rowType = tt.getRowType();
        SortedSet<String> columns = new TreeSet(rowType.keySet());

        for (Object d : values()) {
            CompositeData data = (CompositeData) d;
            for (String column : columns) {
                OpenType ot = rowType.getType(column);
                if (ot instanceof SimpleType) {
                    SimpleType st = (SimpleType) ot;
                    Object o = data.get(column);
                    //SimpleType.
                }
            }
        }
    }

    private void write(ObjectOutput out, SimpleType st, Object o) throws IOException {
        if ((st == SimpleType.BIGDECIMAL) || (st == SimpleType.BIGINTEGER) || (st == SimpleType.OBJECTNAME)) {
            out.writeObject(o);
        } else if (st == SimpleType.BOOLEAN) {
            out.writeBoolean(((Boolean) o).booleanValue());
        } else if (st == SimpleType.BYTE) {
            out.writeByte(((Byte) o).byteValue());
        } else if (st == SimpleType.CHARACTER) {
            out.writeChar(((Character) o).charValue());
        } else if (st == SimpleType.DATE) {
            out.writeLong(((Date) o).getTime());
        } else if (st == SimpleType.DOUBLE) {
            out.writeDouble(((Double) o).doubleValue());
        }
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    }
}