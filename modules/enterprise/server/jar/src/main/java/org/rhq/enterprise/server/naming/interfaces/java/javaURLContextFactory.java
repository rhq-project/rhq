package org.rhq.enterprise.server.naming.interfaces.java;

import java.util.Hashtable;

import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;

import org.jboss.as.naming.NamingContext;

import org.rhq.enterprise.server.AllowRhqServerInternalsAccessPermission;

/** @deprecated this is not used after all */
@Deprecated
public class javaURLContextFactory implements ObjectFactory {

    private static final AllowRhqServerInternalsAccessPermission PERM = new AllowRhqServerInternalsAccessPermission();

    @Override
    @SuppressWarnings("unchecked")
    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment)
        throws Exception {

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(PERM);
        }

        return new NamingContext(name != null ? name : new CompositeName(""), (Hashtable<String, Object>) environment);
    }
}
