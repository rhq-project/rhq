/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.plugins.postfix;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.plugins.augeas.test.AbstractAugeasConfigurationComponentTest;

/**
 * An integration test for {@link PostfixComponent}.
 */
public class PostfixComponentTest extends AbstractAugeasConfigurationComponentTest {
    @Override
    protected String getPluginName() {
        return "Postfix";
    }

    @Override
    protected String getResourceTypeName() {
        return "Postfix Server";
    }

    @Override
    protected Configuration getExpectedResourceConfig() {

        /**
         * myhostname = virtual.domain.tld
        myorigin = $myhostname
        mynetworks = 168.100.189.0/28, 127.0.0.0/8
        mydestination = $myhostname, localhost.$mydomain, localhost, $mydomain,
        inet_interfaces = localhost
        smtpd_banner = $myhostname ESMTP
        disable_vrfy_command = no
        smtpd_helo_required = no
        smtp_always_send_ehlo = yes
        in_flow_delay=100
        local_destination_concurrency_limit = 2
         */
        Configuration config = new Configuration();

        config.put(new PropertySimple("myhostname", "virtual.domain.tld"));
        config.put(new PropertySimple("myorigin", "$myhostname"));
        config.put(new PropertySimple("mynetworks", "168.100.189.0/28, 127.0.0.0/8"));
        config.put(new PropertySimple("mydestination", "$myhostname, localhost.$mydomain, localhost, $mydomain,"));
        config.put(new PropertySimple("inet_interfaces", "localhost"));
        config.put(new PropertySimple("smtpd_banner", "$myhostname ESMTP"));
        config.put(new PropertySimple("disable_vrfy_command", false));
        config.put(new PropertySimple("smtpd_helo_required", false));
        config.put(new PropertySimple("smtp_always_send_ehlo", true));
        config.put(new PropertySimple("in_flow_delay", 100));
        config.put(new PropertySimple("local_destination_concurrency_limit", 2));

        return config;
    }

    @Override
    protected Configuration getUpdatedResourceConfig() {
        Configuration config = new Configuration();
        config.put(new PropertySimple("myhostname", "virtual.domain.com"));
        config.put(new PropertySimple("myorigin", "$myhostname"));
        config.put(new PropertySimple("mynetworks", "168.100.189.0/28"));
        config.put(new PropertySimple("inet_interfaces", "$myhostname, localhost.$mydomain, localhost, $mydomain"));
        config.put(new PropertySimple("smtpd_banner", "$myhostname ESMTP"));
        config.put(new PropertySimple("disable_vrfy_command", false));
        config.put(new PropertySimple("smtpd_helo_required", false));
        config.put(new PropertySimple("smtp_always_send_ehlo", true));
        config.put(new PropertySimple("in_flow_delay", 101));
        config.put(new PropertySimple("local_destination_concurrency_limit", 88));
        return config;
    }
}
