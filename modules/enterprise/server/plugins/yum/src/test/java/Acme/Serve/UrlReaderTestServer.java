/*
 * RHQ Management Platform
 * Copyright (C) 2013 Red Hat, Inc.
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

package Acme.Serve;

import java.io.PrintStream;
import java.util.Map;
import java.util.Properties;

/**
 * This needs to be in the {@code Acme.Serve} package so that authentication realm can be defined.
 *
 * @author Lukas Krejci
 * @since 4.9
 */
public class UrlReaderTestServer extends Serve {
    private static final long serialVersionUID = 1L;


    public static class AuthRealm extends BasicAuthRealm {

        private static final long serialVersionUID = 1L;

        public AuthRealm(String name) {
            super(name);
        }
    }

    public UrlReaderTestServer(Map arguments, PrintStream logStream) {
        super(arguments, logStream);
    }

    @Override
    public void setMappingTable(PathTreeDictionary mappingTable) {
        super.setMappingTable(mappingTable);
    }

    @Override
    protected void initMime() {
        mime = new Properties();
        mime.put("file", "text/plain");
    }

    @Override
    public void setRealms(PathTreeDictionary realms) {
        super.setRealms(realms);
    }
}
