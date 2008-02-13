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
package org.rhq.enterprise.communications.command.param;

/**
 * This class indicates that a parameter should be rendering using a File-Upload mechanism that will allow users to
 * select a file on the user's local machine and upload it to the server.
 *
 * @author John Mazzitelli
 */
public class FileUploadRenderingInformation extends ParameterRenderingInformation {
    /**
     * the UID to identify the serializable version of this class
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor for {@link FileUploadRenderingInformation}.
     *
     * @see ParameterRenderingInformation#ParameterRenderingInformation(String, String)
     */
    public FileUploadRenderingInformation() {
        super();
    }

    /**
     * Constructor for {@link FileUploadRenderingInformation}.
     *
     * @param labelKey       key to a resource bundle that defines this parameter's label string
     * @param descriptionKey key to a resource bundle that defines this parameter's description string
     *
     * @see   ParameterRenderingInformation#ParameterRenderingInformation(String, String)
     */
    public FileUploadRenderingInformation(String labelKey, String descriptionKey) {
        super(labelKey, descriptionKey);
    }
}