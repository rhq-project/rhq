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

package org.rhq.enterprise.server.sync;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.sync.ExportReport;
import org.rhq.core.domain.sync.ExportValidationReport;
import org.rhq.core.domain.sync.ExportWrapper;
import org.rhq.core.domain.sync.ExporterMessages;
import org.rhq.core.domain.sync.ImportReport;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.sync.exporters.Exporter;

/**
 * 
 *
 * @author Lukas Krejci
 */
@Stateless
public class SynchronizationManagerBean implements SynchronizationManagerLocal, SynchronizationManagerRemote {

    private static final Log LOG = LogFactory.getLog(SynchronizationManagerBean.class);
    
    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public ExportReport exportAllSubsystems(Subject subject) {
        ExportWrapper localExport = exportAllSubsystemsLocally(subject);
        
        byte[] buffer = new byte[65536];
        
        ByteArrayOutputStream out = new ByteArrayOutputStream(10240); //10KB is a reasonable minimum size of an export
        
        try {
            int cnt = 0;
            while ((cnt = localExport.getExportFile().read(buffer)) != -1) {
                out.write(buffer, 0, cnt);
            }
            
            return new ExportReport(localExport.getMessagesPerExporter(), out.toByteArray());
        } catch (Exception e) {
            return new ExportReport(e.getMessage());
        } finally {
            try {
                out.close();
            } catch (Exception e) {
                //this doesn't happen - out is backed by just an array
                LOG.error("Closing a byte array output stream failed. This should never happen.");
            }
            
            try {
                localExport.getExportFile().close();
            } catch (Exception e) {
                LOG.warn("Failed to close the export file stream.", e);
            }
        }
    }
    
    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public ExportWrapper exportAllSubsystemsLocally(Subject subject) {
        Set<Exporter<?, ?>> allExporters = new HashSet<Exporter<?, ?>>();
        Map<String, ExporterMessages> messages = new HashMap<String, ExporterMessages>();
        
        for(SynchronizedEntity e : SynchronizedEntity.values()) {
            Exporter<?, ?> exporter = e.getExporter();
            exporter.init(subject);
            allExporters.add(exporter);
        }
        
        try {
            return new ExportWrapper(messages, new ExportingInputStream(allExporters, messages));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize the export.", e);
        }
    }

    @Override
    public ImportReport importAllSubsystems(Subject subject, InputStream exportFile) {
        //TODO implement
        return null;
    }

    @Override
    public ImportReport importAllSubsystems(Subject subject, byte[] exportFile) {
        return importAllSubsystems(subject, new ByteArrayInputStream(exportFile));
    }
    
    @Override
    public ExportValidationReport validate(Subject subject, InputStream exportFile) {
        // TODO implement
        return null;
    }

    @Override
    public ExportValidationReport validate(Subject subject, byte[] exportFile) {
        return validate(subject, new ByteArrayInputStream(exportFile));
    }
}
