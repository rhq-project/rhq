/*
 * RHQ Management Platform
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.enterprise.clientapi;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.bindings.StandardBindings;
import org.rhq.bindings.client.RhqFacade;
import org.rhq.bindings.script.BaseRhqSchemeScriptSourceProvider;

/**
 * @author Lukas Krejci
 */
public class RhqDownloadsScriptSourceProvider extends BaseRhqSchemeScriptSourceProvider implements
    StandardBindings.RhqFacadeChangeListener {

    private static final Log LOG = LogFactory.getLog(RhqDownloadsScriptSourceProvider.class);

    private static final String PREFIX = "//downloads/";

    private RemoteClient remoteClient;

    @Override
    public void rhqFacadeChanged(StandardBindings bindings) {
        RhqFacade facade = bindings.getAssociatedRhqFacade();

        if (facade instanceof RemoteClient) {
            remoteClient = (RemoteClient) facade;
        } else {
            remoteClient = null;
        }
    }

    @Override
    protected Reader doGetScriptSource(URI scriptUri) {
        if (remoteClient == null) {
            return null;
        }

        String path = scriptUri.getSchemeSpecificPart();

        if (!path.startsWith(PREFIX)) {
            return null;
        }

        path.substring(1); //remove the leading '/'

        String urlString = remoteClient.getTransport() + "://" + remoteClient.getHost() + ":" + remoteClient.getPort()
            + path;

        try {
            URL downloadUrl = new URL(urlString);

            return new InputStreamReader(downloadUrl.openStream());
        } catch (MalformedURLException e) {
            LOG.debug("Failed to download the script from the RHQ server using URL: " + urlString, e);
        } catch (IOException e) {
            LOG.debug("Failed to download the script from the RHQ server using URL: " + urlString, e);
        }

        return null;
    }
}
