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

package org.rhq.scripting.python;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.imp;

import org.rhq.scripting.ScriptSourceProvider;

/**
 * This class translates the requests for modules in python using the import
 * statement into calls to RHQ's script source providers.
 * <p>
 * For a script to be downloadable using RHQ, one must add a path prefix to 
 * <code>sys.path</code> so that RHQ is aware of the available locations it should
 * look into.
 * <p>
 * For example, if you have the RHQ repository script source provider available on 
 * the classpath of the CLI, you can add the following to the <code>sys.path</code>:
 * <pre>
 * <code>
 * import sys
 * sys.path.append("__rhq__:rhq:/repositories/my_repository")
 * </code>
 * </pre>
 * and then you can import a module from that repository by the ordinary import statement:
 * <pre>
 * <code>
 * import my_module
 * </code>
 * </pre>
 * This will translate into a download of the script from the following location:
 * <code>rhq://repositories/my_repository/my_module.py</code>.
 * 
 * @author Lukas Krejci
 */
public class PythonSourceProvider extends PyObject {

    private static final long serialVersionUID = 1L;

    private static final String RHQ_PATH_EXTENSION_PREFIX = "__rhq__:";

    private ScriptSourceProvider scriptSourceProvider;
    private String currentPathPrefix;

    public PyObject __call__(PyObject args[], String keywords[]) {
        if (args[0].toString().startsWith(RHQ_PATH_EXTENSION_PREFIX)) {
            currentPathPrefix = args[0].toString().substring(RHQ_PATH_EXTENSION_PREFIX.length());
            return this;
        }
        throw Py.ImportError("unable to handle");
    }

    private static class ReaderInputStream extends InputStream {
        private Reader rdr;

        public ReaderInputStream(Reader rdr) {
            this.rdr = rdr;
        }

        @Override
        public int read() throws IOException {
            return rdr.read();
        }

    }

    public class Loader extends PyObject {

        private static final long serialVersionUID = 1L;

        private String prefix;

        public Loader(String prefix) {
            this.prefix = prefix;
        }

        public PyObject load_module(String name) {
            try {
                URI uri = new URI(prefix + name + ".py");
                Reader rdr = scriptSourceProvider.getScriptSource(uri);
                return imp.createFromSource(name, new ReaderInputStream(rdr), uri.toString());
            } catch (URISyntaxException e) {
                return Py.None;
            }
        }
    }

    public PythonSourceProvider(ScriptSourceProvider scriptSourceProvider) {
        this.scriptSourceProvider = scriptSourceProvider;
    }

    public PyObject find_module(String name) {
        return find_module(name, Py.None);
    }

    public PyObject find_module(String name, PyObject path) {
        try {
            URI uri = new URI(currentPathPrefix + name + ".py");

            return scriptSourceProvider.getScriptSource(uri) == null ? Py.None : new Loader(currentPathPrefix);
        } catch (URISyntaxException e) {
            return Py.None;
        }
    }

    @Override
    public String toString() {
        return getType().toString();
    }
}
