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
package org.rhq.enterprise.server.core;

import java.io.File;
import java.util.Properties;

import javax.ejb.Local;

/**
 * Local interface to the {@link RemoteAgentClientManagerBean} SLSB. This manager performs
 * things related to the remote client (CLI) distribution.
 *
 * @author John Mazzitelli
 */
@Local
public interface RemoteClientManagerLocal {

    /**
     * Returns the path on the server's file system where the CLI version file is found.
     * The CLI version file contains information about the CLI binary, such
     * as what version it is.
     *
     * @return version file location
     *
     * @throws Exception if the file could not be created or found
     */
    File getRemoteClientVersionFile() throws Exception;

    /**
     * Returns the content of the CLI version file, which simply consists
     * of some name/value pairs.
     * The version file contains information about the CLI binary, such
     * as what version it is.
     *
     * @return version properties found in the version file.
     *
     * @throws Exception if cannot read the version file
     */
    Properties getRemoteClientVersionFileContent() throws Exception;

    /**
     * Returns the path on the server's file system where the CLI binary is found.
     * This is the actual CLI distribution that can be installed on remote client machines.
     *
     * @return binary location
     *
     * @throws Exception if the binary file does not exist
     */
    File getRemoteClientBinaryFile() throws Exception;
}