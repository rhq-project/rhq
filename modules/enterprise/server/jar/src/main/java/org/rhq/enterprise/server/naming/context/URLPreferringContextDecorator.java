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
import java.util.Hashtable;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.rhq.enterprise.server.naming.AccessCheckingInitialContextFactoryBuilder;

/**
 * This is a wrapper class around another {@link Context} implementation that
 * prefers to use an URL context for some operation if the JNDI name contains
 * a scheme rather than the original. This is the behavior of {@link InitialContext}
 * which we need to restore in the contexts created by the {@link AccessCheckingInitialContextFactoryBuilder}
 * (which an {@link InitialContext} uses exclusively if the builder is set).
 * <p>
 * This is important because RHQ server has its own initial context factory
 * builder that creates factories that in turn create contexts. If the default
 * {@link InitialContext} implementation was used, we'd never be able to lookup
 * scheme-based names because the default implementation of the {@link InitialContext}
 * always uses the default context of the builder if one is installed no matter 
 * the scheme in the name. 
 * <p>
 * The {@link AccessCheckingInitialContextFactoryBuilder} wraps the context returned
 * by the factory in an instance of this class and thus is restoring the original
 * intended behavior of the {@link InitialContext}. It looks at the name being looked
 * up (bound or whatever) and prefers to use the URL context factories if the name
 * contains the scheme (as does the {@link InitialContext} if no builder is installed).
 * If the name doesn't contain a scheme, the provided default context factory is used to 
 * look up the name.
 * 
 * @author Lukas Krejci
 */
public class URLPreferringContextDecorator implements Context, ContextDecorator, Serializable {

    private static final long serialVersionUID = 1L;
    
    private Context original;
    
    public URLPreferringContextDecorator() {
        
    }
    
    public URLPreferringContextDecorator(Context ctx) {
        original = ctx;
    }
    
    public void init(Context context) {
        original = context;
    }
    
    protected Context getOriginal() throws NamingException {
        return original;
    }
    
    protected Context getURLOrDefaultInitCtx(Name name) throws NamingException {
        @SuppressWarnings("unchecked")
        Context urlContext = URLPreferringContextDecoratorHelper.getURLContext(name, (Hashtable<Object, Object>) getEnvironment());
        return urlContext == null ? getOriginal() : urlContext;
    }

    protected Context getURLOrDefaultInitCtx(String name) throws NamingException {
        @SuppressWarnings("unchecked")
        Context urlContext = URLPreferringContextDecoratorHelper.getURLContext(name, (Hashtable<Object, Object>) getEnvironment());
        return urlContext == null ? getOriginal() : urlContext;
    }

    public Object lookup(Name name) throws NamingException {
        return getURLOrDefaultInitCtx(name).lookup(name);
    }

    public Object lookup(String name) throws NamingException {
        return getURLOrDefaultInitCtx(name).lookup(name);
    }

    public void bind(Name name, Object obj) throws NamingException {
        getURLOrDefaultInitCtx(name).bind(name, obj);
    }

    public void bind(String name, Object obj) throws NamingException {
        getURLOrDefaultInitCtx(name).bind(name, obj);
    }

    public void rebind(Name name, Object obj) throws NamingException {
        getURLOrDefaultInitCtx(name).rebind(name, obj);
    }

    public void rebind(String name, Object obj) throws NamingException {
        getURLOrDefaultInitCtx(name).rebind(name, obj);
    }

    public void unbind(Name name) throws NamingException {
        getURLOrDefaultInitCtx(name).unbind(name);
    }

    public void unbind(String name) throws NamingException {
        getURLOrDefaultInitCtx(name).unbind(name);
    }

    public void rename(Name oldName, Name newName) throws NamingException {
        getURLOrDefaultInitCtx(oldName).rename(oldName, newName);
    }

    public void rename(String oldName, String newName) throws NamingException {
        getURLOrDefaultInitCtx(oldName).rename(oldName, newName);
    }

    public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
        return getURLOrDefaultInitCtx(name).list(name);
    }

    public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
        return getURLOrDefaultInitCtx(name).list(name);
    }

    public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
        return getURLOrDefaultInitCtx(name).listBindings(name);
    }

    public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
        return getURLOrDefaultInitCtx(name).listBindings(name);
    }

    public void destroySubcontext(Name name) throws NamingException {
        getURLOrDefaultInitCtx(name).destroySubcontext(name);
    }

    public void destroySubcontext(String name) throws NamingException {
        getURLOrDefaultInitCtx(name).destroySubcontext(name);
    }

    public Context createSubcontext(Name name) throws NamingException {
        return getURLOrDefaultInitCtx(name).createSubcontext(name);
    }

    public Context createSubcontext(String name) throws NamingException {
        return getURLOrDefaultInitCtx(name).createSubcontext(name);
    }

    public Object lookupLink(Name name) throws NamingException {
        return getURLOrDefaultInitCtx(name).lookupLink(name);
    }

    public Object lookupLink(String name) throws NamingException {
        return getURLOrDefaultInitCtx(name).lookupLink(name);
    }

    public NameParser getNameParser(Name name) throws NamingException {
        return getURLOrDefaultInitCtx(name).getNameParser(name);
    }

    public NameParser getNameParser(String name) throws NamingException {
        return getURLOrDefaultInitCtx(name).getNameParser(name);
    }

    public Name composeName(Name name, Name prefix) throws NamingException {
        return getOriginal().composeName(name, prefix);
    }

    public String composeName(String name, String prefix) throws NamingException {
        return getOriginal().composeName(name, prefix);
    }

    public Object addToEnvironment(String propName, Object propVal) throws NamingException {
        return getOriginal().addToEnvironment(propName, propVal);
    }

    public Object removeFromEnvironment(String propName) throws NamingException {
        return getOriginal().removeFromEnvironment(propName);
    }

    public Hashtable<?, ?> getEnvironment() throws NamingException {
        return getOriginal().getEnvironment();
    }

    public void close() throws NamingException {
        if (getOriginal() != null) {
            getOriginal().close();
            original = null;
        }
    }

    public String getNameInNamespace() throws NamingException {
        return getOriginal().getNameInNamespace();
    }
       
    @Override
    public int hashCode() {
        return original == null ? super.hashCode() : original.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        return original == null ? super.equals(o) : original.equals(o);
    }    
}
