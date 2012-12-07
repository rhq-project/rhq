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

package org.rhq.enterprise.server.naming.context;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.spi.NamingManager;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class URLPreferringContextDecoratorHelper {

    private URLPreferringContextDecoratorHelper() {
        
    }
    
    public static Context getURLContext(String name, Hashtable<Object, Object> env) throws NamingException {
        String scheme = getURLScheme(name);
        if (scheme != null) {
            Context ctx = NamingManager.getURLContext(scheme, env);
            if (ctx != null) {
                return ctx;
            }
        }
        
        return null;
    }
    
    public static Context getURLContext(Name name, Hashtable<Object, Object> env) throws NamingException {
        if (name.size() > 0) {
            String first = name.get(0);
            String scheme = getURLScheme(first);
            if (scheme != null) {
                Context ctx = NamingManager.getURLContext(scheme, env);
                if (ctx != null) {
                    return ctx;
                }
            } 
        }
        
        return null;
    }
    
    //copied from InitialContext
    private static String getURLScheme(String str) {
        int colon_posn = str.indexOf(':');
        int slash_posn = str.indexOf('/');

        if (colon_posn > 0 && (slash_posn == -1 || colon_posn < slash_posn))
            return str.substring(0, colon_posn);
        return null;
    }            
}
