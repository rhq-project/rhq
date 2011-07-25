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

import java.util.Set;

import org.rhq.enterprise.server.sync.ExportException;
import org.rhq.enterprise.server.sync.validators.ConsistencyValidator;


/**
 * This is the interface a subsystem export must implement.
 *
 * @param <Entity> the type of entities exported by the exporter
 * @param <ExportedType> the type that represents the entity in the export
 * 
 * @author Lukas Krejci
 */
public interface Exporter<Entity, ExportedType> {

    /**
     * @return a possibly empty set of validators that will record the state
     * needed by this exporter to exist in the target RHQ installation.
     */
    Set<ConsistencyValidator> getRequiredValidators();
    
    Class<Entity> getExportedEntityType();
    
    /**
     * Initializes the exporter.
     * 
     * @throws ExportException
     */
    void init() throws ExportException;
    
    /**
     * Creates an iterator that is able to traverse the exported data and serialize them
     * one by one.
     * <p>
     * The implementation is free to preload the exported entities or to somehow load them
     * one by one as the callers request by calling {@link ExportingIterator#next()}.
     * 
     * @return
     */
    ExportingIterator<ExportedType> getExportingIterator();

    /**
     * This method can return some notes that the exporter gathered during exporting the
     * entities that are not specific to the individual entities.
     * <p>
     * This method is only called after the {@link #getExportingIterator() export iterator}
     * reached the end.
     */
    String getNotes();
}
