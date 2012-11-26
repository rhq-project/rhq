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

package org.rhq.enterprise.client.script;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.Charset;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.bindings.script.BaseRhqSchemeScriptSourceProvider;

/**
 * @author Lukas Krejci
 */
public class SamplesScriptSourceProvider extends BaseRhqSchemeScriptSourceProvider {

    private static final Log LOG = LogFactory.getLog(SamplesScriptSourceProvider.class);

    private static final String AUTHORITY = "samples";

    public SamplesScriptSourceProvider() {
        super(AUTHORITY);
    }
    
    @Override
    protected Reader doGetScriptSource(URI scriptUri) {
        String path = scriptUri.getPath();

        path = path.substring(1); //remove the leading '/';

        //here we suppose that the CLI was started using the rhq-cli.(sh|bat) script
        //which sets the working directory to the root of the CLI deployment
        File file = new File(AUTHORITY, path);

        try {
            return new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8"));
        } catch (FileNotFoundException e) {
            LOG.debug("Failed to locate the script at: " + scriptUri, e);
            return null;
        }
    }

}
