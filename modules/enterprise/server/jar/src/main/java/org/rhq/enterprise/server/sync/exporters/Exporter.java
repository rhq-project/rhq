/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.enterprise.server.sync.exporters;

import org.rhq.enterprise.server.sync.ExportException;


/**
 * 
 *
 * @author Lukas Krejci
 */
public interface Exporter<T> {

    Class<T> exportedEntityType();
        
    void init() throws ExportException;
    
    ExportingIterator<T> getExportingIterator();

    /**
     * This method can return some notes that the exporter gathered during exporting the
     * entities that are not specific to the individual entities.
     * <p>
     * This method is only called after the {@link #getExportingIterator() export iterator}
     * reached the end.
     */
    String getNotes();
}
