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
package org.rhq.enterprise.server.core;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 * This startup singleton EJB is here solely to work around bug AS7-5530:
 * https://issues.jboss.org/browse/AS7-5530
 *
 * When that bug is fixed, delete this class and uncomment the appropriate things in StartupBean.
 */
@Singleton
@Startup
public class StartupBeanToWorkaroundAS7_5530 {
    @EJB
    private StartupBean startupBean;

    @PostConstruct
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    // this @TransactionAttribute annotation is ignored because of bug AS7-5530
    public void initWithTransactionBecauseAS75530() throws RuntimeException {
        this.startupBean.init(); // call init() which is NOT_SUPPORTED so it suspends the tx bug AS7-5530 gives us erroneously
    }
}
