/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.enterprise.gui.coregui.client.components.table;

import java.util.Collection;

import com.smartgwt.client.data.Record;

/**
 * Typically used for anaonymous class definition for extracting any data from a set of Records, typically
 * ListGridRecords.  For example, a utility that needs the resource ids extracted from the record set's attributes
 * but does not know what the attributes are, could be passed a RecordExtractor.  
 *  
 * @author Jay Shaughnessy
 *
 */
public interface RecordExtractor<T extends Object> {

    Collection<T> extract(Record[] records);

}
