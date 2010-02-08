/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.util;

import java.beans.Introspector;
import java.lang.reflect.Method;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.core.Init;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * A common Seam component that wraps the {@link LookupUtil}, so that any Seam
 * components can use the common DI mechanism to obtain EJB references.
 *
 * This component primarily exists to keep the JNDI lookup in the {@link LookupUtil}
 * so that it will not conflict with Seam's JNDI lookup mechanism.  Eventually, all
 * session beans can simply be annotated as Seam components and the {@link LookupUtil}
 * (as well as this component) can be deprecated.
 *
 * For every getter method defined in {@link LookupUtil}, a Seam component will
 * be created that delegates component creation to that getter method.
 *
 * Example Usage:
 * <code>public static ResourceManagerLocal getResourceManager()</code> will be
 * called any time that a component with name "resourceManager" is injected:
 *
 * <code>@In private ResourceManagerLocal resourceManager;</code> is equivalent to
 * <code>private ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();</code>
 *
 * @author Justin Harris
 */
@Startup
@Scope(ScopeType.APPLICATION)
@Name("lookupComponent")
public class LookupComponent {

    @In
    private Init init;

    @In
    private Context applicationContext;

    @Create
    public void scanComponents() {
        for (Method method : LookupUtil.class.getDeclaredMethods()) {
            String methodName = method.getName();

            // only worried about getters
            if (methodName.startsWith("get")) {
                String componentName = getPropertyName(methodName);
                addComponent(componentName, method);
            }
        }
    }

    /**
     * Returns e.g. "foo" for <code>getterMethodName</code> "getFoo"
     *
     * @param getterMethodName
     * @return
     */
    private String getPropertyName(String getterMethodName) {
        return Introspector.decapitalize(getterMethodName.substring(3));
    }

    /**
     * Registers a new Seam component that uses the <code>creationMethod</code>
     * to "instantiate" a component with the given <code>componentName</code>
     *
     * @param componentName
     * @param creationMethod
     */
    private void addComponent(String componentName, Method creationMethod) {
        LookupUtilComponent component = new LookupUtilComponent(componentName, creationMethod);
        this.applicationContext.set(componentName + ".component", component);

        // set the new component to @AutoCreate
        this.init.addAutocreateVariable(componentName);
    }

    /**
     * Custom Seam component that delegates component intantiation to a creation
     * method.  This is similar in concept to Seam @Factory methods.
     */
    class LookupUtilComponent extends Component {

        private Method creationMethod;

        Method getCreationMethod() {
            return this.creationMethod;
        }

        private LookupUtilComponent(String name, Method creationMethod) {
            super(Object.class, name);

            this.creationMethod = creationMethod;
        }

        @Override
        protected Object instantiate() throws Exception {
            return this.creationMethod.invoke(null);
        }
    }
}