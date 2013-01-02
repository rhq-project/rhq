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
 * 
 * This version is a replacement for the original code (identical) code that uses the StrippedDownStartupBean instead
 * of the fullblown original.
 */
@Singleton
@Startup
public class StrippedDownStartupBeanPreparation {
    private Log log = LogFactory.getLog(this.getClass());

    @EJB
    private StrippedDownStartupBean startupBean;

    @Resource
    private TimerService timerService; // needed to schedule our startup bean init call

    @PostConstruct
    public void initWithTransactionBecauseAS75530() throws RuntimeException {
        log.info("Scheduling the initialization of the testing RHQ deployment");
        timerService.createSingleActionTimer(1, new TimerConfig(null, false)); // call StartupBean in 1ms
    }

    @Timeout
    public void initializeServer() throws RuntimeException {
        try {
            log.info("Initializing the testing RHQ deployment");
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
