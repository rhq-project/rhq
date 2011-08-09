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

package org.rhq.core.domain.sync;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * This is a remotable version of the {@link ExportWrapper}.
 *
 * @author Lukas Krejci
 */
public class ExportReport implements Serializable {

    private static final long serialVersionUID = 1L;

    private Map<String, ExporterMessages> messagesPerExporter;
    
    private String errorMessage;
    
    private byte[] exportFile;

    protected ExportReport() {
        
    }
    
    /**
     * @param messagesPerExporter
     * @param exportFile
     */
    public ExportReport(Map<String, ExporterMessages> messagesPerExporter, byte[] exportFile) {
        super();
        this.messagesPerExporter = messagesPerExporter;
        this.exportFile = exportFile;
    }

    public ExportReport(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    /**
     * @return the messagesPerExporter
     */
    public Map<String, ExporterMessages> getMessagesPerExporter() {
        return messagesPerExporter;
    }

    /**
     * @return the exportFile
     */
    public byte[] getExportFile() {
        return exportFile;
    }
    
    /**
     * @return the errorMessage
     */
    public String getErrorMessage() {
        return errorMessage;
    }
}
