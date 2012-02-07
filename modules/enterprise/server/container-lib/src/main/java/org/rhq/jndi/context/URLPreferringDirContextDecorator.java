/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.jndi.context;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.naming.NotContextException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

/**
 * Akin to {@link URLPreferringContextDecorator} this class implements the similar logic
 * for {@link DirContext}s.
 *
 * @author Lukas Krejci
 */
public class URLPreferringDirContextDecorator extends URLPreferringContextDecorator implements DirContext {

    private static final long serialVersionUID = 1L;

    public URLPreferringDirContextDecorator() {
        super(null);
    }
    
    public URLPreferringDirContextDecorator(DirContext ctx) {
        super(ctx);
    }
    
    protected DirContext checkAndCast(Context ctx) throws NamingException {
        if (!(ctx instanceof DirContext)) {
            if (ctx == null) {
                throw new NoInitialContextException();
            } else {
                throw new NotContextException(
                    "Not an instance of DirContext");
            }
        }
        
        return (DirContext) ctx;
    }
    
    @Override
    protected DirContext getURLOrDefaultInitCtx(Name name) throws NamingException {
        Context ctx = super.getURLOrDefaultInitCtx(name);
        return checkAndCast(ctx);
    }

    @Override
    protected DirContext getURLOrDefaultInitCtx(String name) throws NamingException {
        Context ctx = super.getURLOrDefaultInitCtx(name);
        return checkAndCast(ctx);
    }

    public Attributes getAttributes(Name name) throws NamingException {
        return getURLOrDefaultInitCtx(name).getAttributes(name);
    }

    public Attributes getAttributes(String name) throws NamingException {
        return getURLOrDefaultInitCtx(name).getAttributes(name);
    }

    public Attributes getAttributes(Name name, String[] attrIds) throws NamingException {
        return getURLOrDefaultInitCtx(name).getAttributes(name, attrIds);
    }

    public Attributes getAttributes(String name, String[] attrIds) throws NamingException {
        return getURLOrDefaultInitCtx(name).getAttributes(name, attrIds);
    }

    public void modifyAttributes(Name name, int mod_op, Attributes attrs) throws NamingException {
        getURLOrDefaultInitCtx(name).modifyAttributes(name, mod_op, attrs);
    }

    public void modifyAttributes(String name, int mod_op, Attributes attrs) throws NamingException {
        getURLOrDefaultInitCtx(name).modifyAttributes(name, mod_op, attrs);
    }

    public void modifyAttributes(Name name, ModificationItem[] mods) throws NamingException {
        getURLOrDefaultInitCtx(name).modifyAttributes(name, mods);
    }

    public void modifyAttributes(String name, ModificationItem[] mods) throws NamingException {
        getURLOrDefaultInitCtx(name).modifyAttributes(name, mods);
    }

    public void bind(Name name, Object obj, Attributes attrs) throws NamingException {
        getURLOrDefaultInitCtx(name).bind(name, obj, attrs);
    }

    public void bind(String name, Object obj, Attributes attrs) throws NamingException {
        getURLOrDefaultInitCtx(name).bind(name, obj, attrs);
    }

    public void rebind(Name name, Object obj, Attributes attrs) throws NamingException {
        getURLOrDefaultInitCtx(name).rebind(name, obj, attrs);
    }

    public void rebind(String name, Object obj, Attributes attrs) throws NamingException {
        getURLOrDefaultInitCtx(name).rebind(name, obj, attrs);
    }

    public DirContext createSubcontext(Name name, Attributes attrs) throws NamingException {
        return getURLOrDefaultInitCtx(name).createSubcontext(name,  attrs);
    }

    public DirContext createSubcontext(String name, Attributes attrs) throws NamingException {
        return getURLOrDefaultInitCtx(name).createSubcontext(name,  attrs);
    }

    public DirContext getSchema(Name name) throws NamingException {
        return getURLOrDefaultInitCtx(name).getSchema(name);
    }

    public DirContext getSchema(String name) throws NamingException {
        return getURLOrDefaultInitCtx(name).getSchema(name);
    }

    public DirContext getSchemaClassDefinition(Name name) throws NamingException {
        return getURLOrDefaultInitCtx(name).getSchemaClassDefinition(name);
    }

    public DirContext getSchemaClassDefinition(String name) throws NamingException {
        return getURLOrDefaultInitCtx(name).getSchemaClassDefinition(name);
    }

    public NamingEnumeration<SearchResult>
        search(Name name, Attributes matchingAttributes, String[] attributesToReturn) throws NamingException {
        return getURLOrDefaultInitCtx(name).search(name, matchingAttributes, attributesToReturn);
    }

    public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes,
        String[] attributesToReturn) throws NamingException {
        return getURLOrDefaultInitCtx(name).search(name, matchingAttributes, attributesToReturn);
    }

    public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes) throws NamingException {
        return getURLOrDefaultInitCtx(name).search(name, matchingAttributes);
    }

    public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes) throws NamingException {
        return getURLOrDefaultInitCtx(name).search(name, matchingAttributes);
    }

    public NamingEnumeration<SearchResult> search(Name name, String filter, SearchControls cons) throws NamingException {
        return getURLOrDefaultInitCtx(name).search(name, filter, cons);
    }

    public NamingEnumeration<SearchResult> search(String name, String filter, SearchControls cons)
        throws NamingException {
        return getURLOrDefaultInitCtx(name).search(name, filter, cons);
    }

    public NamingEnumeration<SearchResult>
        search(Name name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException {
        return getURLOrDefaultInitCtx(name).search(name, filterExpr, filterArgs, cons);
    }

    public NamingEnumeration<SearchResult> search(String name, String filterExpr, Object[] filterArgs,
        SearchControls cons) throws NamingException {
        return getURLOrDefaultInitCtx(name).search(name, filterExpr, filterArgs, cons);
    }    
}
