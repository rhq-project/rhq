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

package org.rhq.bindings.util;

import java.io.Reader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.rhq.scripting.ScriptSourceProvider;

/**
 * An implementation of the script source provider that uses a collection of another
 * providers to locate the scripts.
 * <p>
 * The providers are tried in the order they were supplied to the constructor of this class
 * and the first script source provider that manages to provide a non-null reader for
 * given URI "wins".
 *  
 * @author Lukas Krejci
 */
public class MultiScriptSourceProvider implements ScriptSourceProvider {

    private Collection<ScriptSourceProvider> providers;

    public MultiScriptSourceProvider(Collection<? extends ScriptSourceProvider> providers) {
        this.providers = new ArrayList<ScriptSourceProvider>(providers);
    }

    public MultiScriptSourceProvider(ScriptSourceProvider... providers) {
        this.providers = new ArrayList<ScriptSourceProvider>(Arrays.asList(providers));
    }

    @Override
    public Reader getScriptSource(URI scriptUri) {

        for (ScriptSourceProvider provider : providers) {
            Reader rdr = provider.getScriptSource(scriptUri);
            if (rdr != null) {
                return rdr;
            }
        }

        return null;
    }

}
