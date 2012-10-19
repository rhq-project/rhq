package org.rhq.plugins.snmptrapd;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;
import java.util.Set;

import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSource;
import org.rhq.core.domain.event.transfer.EventReport;
import org.rhq.core.pc.event.EventSenderRunner;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static org.testng.AssertJUnit.*;

public class SnmpTrapdComponentTest extends ComponentTest {


    private Snmp snmp;
    private TransportMapping peer;
    private InetAddress address;
    private int port;
    public static final OctetString community = new OctetString("public");

    static final OID alertName = oid("1.3.6.1.4.1.18016.2.1.1");
    // private static final OID alertResourceName = oid("1.3.6.1.4.1.18016.2.1.2");
    // private static final OID alertPlatformName = oid("1.3.6.1.4.1.18016.2.1.3");
    private static final OID alertSeverity = oid("1.3.6.1.4.1.18016.2.1.5");
    // private static final OID alertUrl = oid("1.3.6.1.4.1.18016.2.1.6");

    private static OID oid(String string) {
        return new OID(string);
    }

    public SnmpTrapdComponentTest() {
        super(new SnmpTrapdComponent());
    }

    @BeforeTest
    @Override
    protected void before() throws Exception {
        super.before();
        port = configuration.getSimple("port").getIntegerValue();
        try {
            address = InetAddress.getLocalHost();
            peer = new DefaultUdpTransportMapping(); //new UdpAddress(address, getPort()));
            snmp = new Snmp(peer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void setConfiguration() {
        configuration.getSimple("pollInterval").setIntegerValue(1);
    }

    private void add(PDU pdu, OID oid, Object message) {
        String s = String.valueOf(message);
        pdu.add(new VariableBinding(oid, new OctetString(s)));
    }

    enum Severity {
        high, medium, info;
    }

    protected void sendTrap(String message) {
        PDU pdu = new PDU();
        pdu.setType(PDU.TRAP);
        add(pdu, alertName, message);
        add(pdu, alertSeverity, Severity.medium);

        CommunityTarget target = new CommunityTarget();
        target.setCommunity(community);
        target.setVersion(SnmpConstants.version2c);
        target.setAddress(new UdpAddress(address, port));
        target.setTimeout(1000);
        target.setRetries(2);

        try {
            snmp.send(pdu, target);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Test
    public void test() throws Exception {
        log.info("listening");
        sendTrap("hello, world");
        boolean success = false;
        for (int i = 0; i < 4; i++) {
            Thread.sleep(500);
            EventSenderRunner esr = new EventSenderRunner(eventManager);
            EventReport eventReport = esr.call();
            Map<EventSource, Set<Event>> events = eventReport.getEvents();
            log.info("events " + events);
            if (events.size() > 0) {
                success = true;
                break;
            }
        }
        assertTrue("Did not get event (in time)", success);
    }

}
