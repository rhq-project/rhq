/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

import java.io.File;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.Test;

import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.helpers.alertMigration.Alert13Parser;

/**
 * Test for the alert13 parser
 * @author Heiko W. Rupp
 */
@Test
public class Alert13ParserTest {

    private final Log log = LogFactory.getLog(Alert13ParserTest.class);



    public void testParser() throws Exception {

        File input = new File("src/test/resources/alertDef.csv");

        Alert13Parser parser = new Alert13Parser(input);
        List<AlertNotification> notifications = parser.parse();

        assert notifications != null;
        assert notifications.size()==15 : "Did not find 15 notifications, but " + notifications.size();

        for (AlertNotification n : notifications) {

            switch (n.getAlertNotificationId()) {
                case 10001:
                    assert n.getAlertDefinitionId() == 10001;
                    assert n.getSenderName().equals(Alert13Parser.RHQ_USERS);
                    break;
                case 10011:
                    assert n.getAlertDefinitionId() == 10011;
                    assert n.getSenderName().equals("Roles");
                    break;
                case 10002:
                    assert n.getAlertDefinitionId() == 10011;
                    assert n.getSenderName().equals("Email");
                    assert n.getConfiguration().getSimpleValue("emailAddress","").equals("hwr@bsd.de");
                    break;
                case 10031:
                    assert n.getAlertDefinitionId() == 10011;
                    assert n.getSenderName().equals("Email");
                    assert n.getConfiguration().getSimpleValue("emailAddress","").equals("hwr@pilhuhn.de");
                    break;
                case 10032:
                    assert n.getAlertDefinitionId() == 10051;
                    assert n.getSenderName().equals("SNMP");
                    Configuration c = n.getConfiguration();
                    assert c.getSimpleValue("host","").equals("localhost");
                    assert c.getSimpleValue("OID","").equals("1.2.3.4.5.6.7");

            }
        }
    }
}
