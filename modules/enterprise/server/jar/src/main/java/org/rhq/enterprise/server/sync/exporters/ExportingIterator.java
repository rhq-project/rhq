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

import java.util.Iterator;

import org.rhq.enterprise.server.sync.ExportWriter;

/**
 * 
 *
 * @author Lukas Krejci
 */
public interface ExportingIterator<E> extends Iterator<E> {

    /**
     * Exports the current entity using the provided writer.
     * <p>
     * Can throw a runtime exception to denote a failure.
     * 
     * @param output
     */
    void export(ExportWriter output);
    
    /**
     * This is the opportunity for the exporter to submit some
     * messages that will be transfered to the export file. This
     * is to submit some human understandable information that is 
     * not otherwise formalized or to convey some other information
     * about otherwise successful export of the single entity.
     *  
     * @return
     */
    String getNotes();
}
