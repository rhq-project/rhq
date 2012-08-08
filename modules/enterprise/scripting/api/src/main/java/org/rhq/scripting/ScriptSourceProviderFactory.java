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

package org.rhq.scripting;

import java.util.ArrayList;
import java.util.ServiceLoader;

/**
 * A factory class for the ScriptSourceProvider implementations.
 * 
 * @author Lukas Krejci
 */
public final class ScriptSourceProviderFactory {

    private ScriptSourceProviderFactory() {

    }

    /**
     * Loads the set of the available {@link ScriptSourceProvider} implementations
     * present on the classpath and registered in META-INF/services.
     * 
     * @param classLoader the classloader to be used to locate the impls or null if the
     * context class loader of the current thread should be used.
     * 
     * @return the set of the script source providers available
     */
    public static ScriptSourceProvider[] get(ClassLoader classLoader) {
        ArrayList<ScriptSourceProvider> ps = new ArrayList<ScriptSourceProvider>();

        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }

        ServiceLoader<ScriptSourceProvider> loader = ServiceLoader.load(ScriptSourceProvider.class, classLoader);

        for (ScriptSourceProvider provider : loader) {
            ps.add(provider);
        }

        return ps.toArray(new ScriptSourceProvider[ps.size()]);
    }
}
