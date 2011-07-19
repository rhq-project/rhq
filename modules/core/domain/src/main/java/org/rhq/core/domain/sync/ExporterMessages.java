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

/**
 * 
 *
 * @author Lukas Krejci
 */
public class ExporterMessages implements Serializable {

    private static final long serialVersionUID = 1L;

    private String errorMessage;
    private String exporterNotes;
    private List<String> perEntityErrorMessages;
    private List<String> perEntityNotes;

    /**
     * @return the errorMessage
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * @param errorMessage the errorMessage to set
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * @return the exporterNotes
     */
    public String getExporterNotes() {
        return exporterNotes;
    }

    /**
     * @param exporterNotes the exporterNotes to set
     */
    public void setExporterNotes(String exporterNotes) {
        this.exporterNotes = exporterNotes;
    }

    /**
     * @return the perEntityErrorMessages
     */
    public List<String> getPerEntityErrorMessages() {
        return perEntityErrorMessages;
    }

    /**
     * @param perEntityErrorMessages the perEntityErrorMessages to set
     */
    public void setPerEntityErrorMessages(List<String> perEntityErrorMessages) {
        this.perEntityErrorMessages = perEntityErrorMessages;
    }

    /**
     * @return the perEntityNotes
     */
    public List<String> getPerEntityNotes() {
        return perEntityNotes;
    }

    /**
     * @param perEntityNotes the perEntityNotes to set
     */
    public void setPerEntityNotes(List<String> perEntityNotes) {
        this.perEntityNotes = perEntityNotes;
    }
}
