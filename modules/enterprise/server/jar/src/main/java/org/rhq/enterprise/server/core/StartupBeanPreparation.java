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
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This startup singleton EJB is here to work around bug AS7-5530 and to
 * schedule the real StartupBean's work in a delayed fashion (this is to allow
 * AS7 to complete its deployment work before we do our work).
 * 
 * See https://issues.jboss.org/browse/AS7-5530
 */
@Singleton
@Startup
public class StartupBeanPreparation {
    private Log log = LogFactory.getLog(this.getClass());

    @EJB
    private StartupLocal startupBean;

    @Resource
    private TimerService timerService; // needed to schedule our startup bean init call

    @PostConstruct
    public void initWithTransactionBecauseAS75530() throws RuntimeException {
        timerService.createSingleActionTimer(10000, new TimerConfig(null, false)); // call StartupBean in 10s
    }

    @Timeout
    public void initializeServer() throws RuntimeException {
        try {
            this.startupBean.init();
        } catch (Throwable t) {
            // do NOT allow exceptions to bubble out of our method because then
            // the EJB container would simply re-trigger the timer and call us again
            // and we don't want to keep failing over and over filling the logs
            // in an infinite loop.
            log.fatal("The server failed to start up properly", t);
        }
    }
}
