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

package org.rhq.jndi;

import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

import org.rhq.jndi.context.ContextDecoratorPicker;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class DecoratingInitialContextFactory implements InitialContextFactory {

    private List<ContextDecoratorPicker> pickers;
    private InitialContextFactory factory;
    
    public DecoratingInitialContextFactory(InitialContextFactory factory, List<ContextDecoratorPicker> decoratorPickers) {
        this.factory = factory;
        this.pickers = decoratorPickers;
    }
    
    public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
        Context ctx = factory.getInitialContext(environment);
        
        try {
            for(ContextDecoratorPicker picker : pickers) {
                ctx = picker.wrapInAppropriateDecorator(ctx);
            }
        } catch (IllegalArgumentException e) {
            NamingException ex = new NamingException();
            ex.initCause(ex);
            
            throw e;
        }
        
        return ctx;
    }

}
