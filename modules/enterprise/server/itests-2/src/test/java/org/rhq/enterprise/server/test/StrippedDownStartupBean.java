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

package org.rhq.enterprise.server.test;

import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.rhq.enterprise.server.core.StartupBean;
import org.rhq.enterprise.server.naming.NamingHack;

/**
 * This is a replacement for the fullblown {@link StartupBean} of the actual RHQ server.
 * @author Lukas Krejci
 */
@Singleton
public class StrippedDownStartupBean {

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void secureNaming() {
        NamingHack.bruteForceInitialContextFactoryBuilder();
    }

    public void init() {
        secureNaming();
    }
}
