/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.enterprise.gui.coregui.server.listener;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.ejb.EJB;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.rhq.enterprise.server.core.ShutdownListener;
import org.rhq.enterprise.server.core.StartupLocal;

/**
 * Listens to {@link ServletContextEvent}s to initialize or shutdown RHQ server.
 *
 * @author Thomas Segismont
 */
@WebListener
public class CoreGuiServletContextListener implements ServletContextListener {

    private ScheduledExecutorService scheduledExecutorService;

    @EJB
    StartupLocal startupBean;

    @EJB
    ShutdownListener shutdownListener;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    startupBean.init();
                } catch (Exception e) {
                    shutdownListener.handleNotification();
                }
            }
        }, 10, SECONDS);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        shutdownListener.handleNotification();
        scheduledExecutorService.shutdownNow();
    }
}
