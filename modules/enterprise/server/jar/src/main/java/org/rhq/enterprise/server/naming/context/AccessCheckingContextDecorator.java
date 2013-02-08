/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.enterprise.server.naming.context;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.rhq.enterprise.server.AllowRhqServerInternalsAccessPermission;

/**
 * This is the "meat" of the RHQ's secured JNDI access. This {@link Context} decorator
 * applied security checks in each method (lookups, (un)bindings, etc).
 * <p>
 * The security check consists of checking if the current callstack has the {@link AllowRhqServerInternalsAccessPermission}.
 * <p>
 * This decorator applies the security check on any JNDI name without a scheme and
 * on any name that has a scheme listed in the {@link #checkedSchemes} list supplied
 * in the constructor.
 *
 * @author Lukas Krejci
 */
public class AccessCheckingContextDecorator implements Context, ContextDecorator, Serializable {

    private static final long serialVersionUID = 1L;
    
    private static final AllowRhqServerInternalsAccessPermission PERM = new AllowRhqServerInternalsAccessPermission();
    private Context original;
    private List<String> checkedSchemes;
    
    public AccessCheckingContextDecorator(String... checkedSchemes) {
        this.checkedSchemes = Arrays.asList(checkedSchemes);
    }
    
    public AccessCheckingContextDecorator(Context original, String... checkedSchemes) {
        this.original = original;
        this.checkedSchemes = Arrays.asList(checkedSchemes);
    }
    
    public void init(Context ctx) {
        this.original = ctx;
    }
    
    protected Context getOriginal() {
        return original;
    }
    
    protected static void check() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) sm.checkPermission(PERM);              
    }
    
    private void checkScheme(String scheme) {
        if (scheme == null || checkedSchemes.contains(scheme)) {
            check();
        }        
    }
    
    protected void check(String name) {
        checkScheme(getURLScheme(name));
    }
    
    protected void check(Name name) {
        if (name.size() == 0) {
            check();
        } else {
            String first = name.get(0);
            checkScheme(getURLScheme(first));            
        }
    }
    
    public Object lookup(Name name) throws NamingException {
        check(name);
        return original.lookup(name);
    }

    public Object lookup(String name) throws NamingException {
        check(name);
        return original.lookup(name);
    }

    public void bind(Name name, Object obj) throws NamingException {
        check(name);
        original.bind(name, obj);
    }

    public void bind(String name, Object obj) throws NamingException {
        check(name);
        original.bind(name, obj);
    }

    public void rebind(Name name, Object obj) throws NamingException {
        check(name);
        original.rebind(name, obj);
    }

    public void rebind(String name, Object obj) throws NamingException {
        check(name);
        original.rebind(name, obj);
    }

    public void unbind(Name name) throws NamingException {
        check(name);
        original.unbind(name);
    }

    public void unbind(String name) throws NamingException {
        check(name);
        original.unbind(name);
    }

    public void rename(Name oldName, Name newName) throws NamingException {
        check(oldName);
        check(newName);
        original.rename(oldName, newName);
    }

    public void rename(String oldName, String newName) throws NamingException {
        check(oldName);
        check(newName);
        original.rename(oldName, newName);
    }

    public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
        check(name);
        return original.list(name);
    }

    public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
        check(name);
        return original.list(name);
    }

    public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
        check(name);
        return original.listBindings(name);
    }

    public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
        check(name);
        return original.listBindings(name);
    }

    public void destroySubcontext(Name name) throws NamingException {
        check(name);
        original.destroySubcontext(name);
    }

    public void destroySubcontext(String name) throws NamingException {
        check(name);
        original.destroySubcontext(name);
    }

    public Context createSubcontext(Name name) throws NamingException {
        check(name);
        return original.createSubcontext(name);
    }

    public Context createSubcontext(String name) throws NamingException {
        check(name);
        return original.createSubcontext(name);
    }

    public Object lookupLink(Name name) throws NamingException {
        check(name);
        return original.lookupLink(name);
    }

    public Object lookupLink(String name) throws NamingException {
        check(name);
        return original.lookupLink(name);
    }

    public NameParser getNameParser(Name name) throws NamingException {
        check(name);
        return original.getNameParser(name);
    }

    public NameParser getNameParser(String name) throws NamingException {
        check(name);
        return original.getNameParser(name);
    }

    public Name composeName(Name name, Name prefix) throws NamingException {
        check(name);
        return original.composeName(name, prefix);
    }

    public String composeName(String name, String prefix) throws NamingException {
        check(name);
        return original.composeName(name, prefix);
    }

    public Object addToEnvironment(String propName, Object propVal) throws NamingException {
        check();
        return original.addToEnvironment(propName, propVal);
    }

    public Object removeFromEnvironment(String propName) throws NamingException {
        check();
        return original.removeFromEnvironment(propName);
    }

    public Hashtable<?, ?> getEnvironment() throws NamingException {
        check();
        return original.getEnvironment();
    }

    public void close() throws NamingException {
        check();
        original.close();
    }

    public String getNameInNamespace() throws NamingException {
        check();
        return original.getNameInNamespace();
    }    
    
    //copied from InitialContext
    private static String getURLScheme(String str) {
        int colon_posn = str.indexOf(':');
        int slash_posn = str.indexOf('/');

        if (colon_posn > 0 && (slash_posn == -1 || colon_posn < slash_posn))
            return str.substring(0, colon_posn);
        return null;
    }            
    
    @Override
    public int hashCode() {
        return getOriginal() == null ? super.hashCode() : getOriginal().hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        return getOriginal() == null ? super.equals(o) : getOriginal().equals(o);
    }
}
