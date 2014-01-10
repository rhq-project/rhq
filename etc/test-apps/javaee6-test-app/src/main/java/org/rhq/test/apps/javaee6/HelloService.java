/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

package org.rhq.test.apps.javaee6;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.management.ObjectName;

import static java.lang.management.ManagementFactory.getPlatformMBeanServer;

/**
 * @author Thomas Segismont
 */
@Singleton
@Startup
public class HelloService implements HelloServiceMBean {
    private static final String BEAN_NAME = "myapp:service=" + HelloService.class.getSimpleName();

    @PostConstruct
    public void init() {
        try {
            getPlatformMBeanServer().registerMBean(this, new ObjectName(BEAN_NAME));
            System.out.println("Registered = " + BEAN_NAME);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    public void destroy() {
        try {
            getPlatformMBeanServer().unregisterMBean(new ObjectName(BEAN_NAME));
            System.out.println("Unregistered = " + BEAN_NAME);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String helloTo(String somebody) {
        return String.format("Hello %s!", somebody);
    }
}
