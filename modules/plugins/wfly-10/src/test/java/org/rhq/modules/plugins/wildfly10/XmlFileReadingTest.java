package org.rhq.modules.plugins.wildfly10;

import java.io.File;
import java.net.URL;
import java.util.Map;

import org.testng.annotations.Test;

import org.rhq.modules.plugins.wildfly10.helper.HostConfiguration;
import org.rhq.modules.plugins.wildfly10.helper.HostPort;
import org.rhq.modules.plugins.wildfly10.helper.TruststoreConfig;

/**
 * Test the ability to read information from the AS7 standalone.xml or host.xml config files using
 * {@link HostConfiguration}.
 *
 * @author Heiko W. Rupp
 */
@Test(groups = "unit")
public class XmlFileReadingTest {

    public void hostPort70() throws Exception {
        URL url = getClass().getClassLoader().getResource("standalone70.xml");
        HostConfiguration hostConfig = new HostConfiguration(new File(url.getPath()));
        HostPort hp = hostConfig.getManagementHostPort(
                new AS7CommandLine(new String [] {"java", "foo.Main", "org.jboss.as.standalone"}), AS7Mode.STANDALONE);
        System.out.println(hp);
        assert hp.host.equals("127.0.0.70") : "Host is " + hp.host;
        assert hp.port==19990 : "Port is " + hp.port;
    }

    public void hostPort71() throws Exception {
        URL url = getClass().getClassLoader().getResource("standalone71.xml");
        HostConfiguration hostConfig = new HostConfiguration(new File(url.getPath()));
        HostPort hp = hostConfig.getManagementHostPort(
                new AS7CommandLine(new String[] {"java", "foo.Main", "org.jboss.as.standalone"}), AS7Mode.STANDALONE);
        System.out.println(hp);
        // hp : HostPort{host='localhost', port=9990, isLocal=true}
        assert hp.host.equals("127.0.0.71") : "Host is " + hp.host;
        assert hp.port==29990 : "Port is " + hp.port;
    }

    public void domainController1() throws Exception {
        URL url = getClass().getClassLoader().getResource("host1.xml");
        HostConfiguration hostConfig = new HostConfiguration(new File(url.getPath()));
        HostPort hp = hostConfig.getDomainControllerHostPort(new AS7CommandLine(new String[]{"java", "foo.Main",
                "org.jboss.as.host-controller"}));
        assert hp.isLocal : "DC is not local as expected: " + hp;
    }

    public void domainController2() throws Exception {
        URL url = getClass().getClassLoader().getResource("host2.xml");
        HostConfiguration hostConfig = new HostConfiguration(new File(url.getPath()));
        HostPort hp = hostConfig.getDomainControllerHostPort(new AS7CommandLine(new String[]{"java", "foo.Main",
                "org.jboss.as.host-controller"}));
        assert "192.168.100.1".equals(hp.host) : "DC is at " + hp.host;
        assert hp.port == 9559 : "DC port is at " + hp.port;
    }

    public void domainController3() throws Exception {
        URL url = getClass().getClassLoader().getResource("host3.xml");
        HostConfiguration hostConfig = new HostConfiguration(new File(url.getPath()));
        HostPort hp = hostConfig.getDomainControllerHostPort(new AS7CommandLine(new String[]{"java", "foo.Main",
                "org.jboss.as.host-controller", "--master-address=192.168.123.123"}));
        assert "192.168.123.123".equals(hp.host) : "DC is at " + hp.host;
        assert hp.port == 1234 : "DC port is at " + hp.port;
    }


    public void testXpath70() throws Exception {
        URL url = getClass().getClassLoader().getResource("standalone70.xml");
        HostConfiguration hostConfig = new HostConfiguration(new File(url.getPath()));
/*
        String realm = bd.obtainXmlPropertyViaXPath("/management/management-interfaces/http-interface/@security-realm");
        assert "ManagementRealm".equals(realm) : "Realm was " + realm;
*/
        String pathExpr = "//management/management-interfaces/http-interface/@port";
        String port = hostConfig.obtainXmlPropertyViaXPath(pathExpr);
        assert "19990".equals(port) : "Port was [" + port + "]";

        pathExpr = "//management/management-interfaces/http-interface/@interface";
        String interfName = hostConfig.obtainXmlPropertyViaXPath(pathExpr);
        assert "management".equals(interfName) : "Interface was " + interfName;

        pathExpr = "/server/interfaces/interface[@name='" + interfName + "']/inet-address/@value";
        String interfElem = hostConfig.obtainXmlPropertyViaXPath(pathExpr);
        assert "${jboss.bind.address.management:127.0.0.70}".equals(interfElem) : "InterfElem was " + interfElem;
    }


    public void testXpath71() throws Exception {
        URL url = getClass().getClassLoader().getResource("standalone71.xml");
        HostConfiguration hostConfig = new HostConfiguration(new File(url.getPath()));

        String realm = hostConfig.obtainXmlPropertyViaXPath("//management/management-interfaces/http-interface/@security-realm");
        assert "ManagementRealm".equals(realm) : "Realm was " + realm;
        String sbindingRef = hostConfig.obtainXmlPropertyViaXPath(
                ("//management/management-interfaces/http-interface/socket-binding/@http"));
        assert "management-http".equals(sbindingRef): "Socketbinding was " + sbindingRef;

        String pathExpr = "/server/socket-binding-group/socket-binding[@name='" + sbindingRef + "']/@port";
        String port = hostConfig.obtainXmlPropertyViaXPath(pathExpr);
        assert "29990".equals(port) : "Port was [" + port + "]";

        pathExpr = "/server/socket-binding-group/socket-binding[@name='" + sbindingRef + "']/@interface";
        String interfName = hostConfig.obtainXmlPropertyViaXPath(pathExpr);
        assert "management".equals(interfName) : "Interface was " + interfName;

        pathExpr = "/server/interfaces/interface[@name='" + interfName + "']/inet-address/@value";
        String interfElem = hostConfig.obtainXmlPropertyViaXPath(pathExpr);
        assert "${jboss.bind.address.management:127.0.0.71}".equals(interfElem) : "InterfElem was " + interfElem;
    }

    public void testXpath711() throws Exception {
        URL url = getClass().getClassLoader().getResource("standalone711.xml");
        HostConfiguration hostConfig = new HostConfiguration(new File(url.getPath()));

        String realm = hostConfig.obtainXmlPropertyViaXPath("//management/management-interfaces/http-interface/@security-realm");
        assert "ManagementRealm".equals(realm) : "Realm was " + realm;
        String sbindingRef = hostConfig.obtainXmlPropertyViaXPath(
                ("//management/management-interfaces/http-interface/socket-binding/@http"));
        assert "management-http".equals(sbindingRef): "Socketbinding was " + sbindingRef;

        String pathExpr = "/server/socket-binding-group/socket-binding[@name='" + sbindingRef + "']/@port";
        String port = hostConfig.obtainXmlPropertyViaXPath(pathExpr);
        assert "${jboss.management.http.port:9990}".equals(port) : "Port was [" + port + "]";

        pathExpr = "/server/socket-binding-group/socket-binding[@name='" + sbindingRef + "']/@interface";
        String interfName = hostConfig.obtainXmlPropertyViaXPath(pathExpr);
        assert "management".equals(interfName) : "Interface was " + interfName;

        pathExpr = "/server/interfaces/interface[@name='" + interfName + "']/inet-address/@value";
        String interfElem = hostConfig.obtainXmlPropertyViaXPath(pathExpr);
        assert "${jboss.bind.address.management:127.0.0.71}".equals(interfElem) : "InterfElem was " + interfElem;

        String socketBindingGroupName = "standard-sockets";
        pathExpr = "/server/socket-binding-group[@name='" + socketBindingGroupName + "']/@port-offset";
        String offsetAttr = hostConfig.obtainXmlPropertyViaXPath(pathExpr);
        assert "${jboss.socket.binding.port-offset:123}".equals(offsetAttr) : "Port-Offset was " + offsetAttr;
    }

    public void testGetRealm() throws Exception {
        URL url = getClass().getClassLoader().getResource("standalone71.xml");
        HostConfiguration hostConfig = new HostConfiguration(new File(url.getPath()));

        String realm = hostConfig.obtainXmlPropertyViaXPath("//management/management-interfaces/http-interface/@security-realm");
        assert "ManagementRealm".equals(realm) : "Realm was " + realm;

        String xpathExpression = "//management//security-realm[@name ='%s']/authentication/properties/@path";

        String propsFileName = hostConfig.obtainXmlPropertyViaXPath(String.format(xpathExpression,realm));
        assert "mgmt-users.properties".equals(propsFileName) : "File name was " + propsFileName;

        String propsFilePathRel = hostConfig.obtainXmlPropertyViaXPath("//management//security-realm[@name ='" + realm + "']/authentication/properties/@relative-to");
        assert "jboss.server.config.dir".equals(propsFilePathRel) : "Path was " + propsFileName;
    }

    public void testIsLocalNativeAuth() throws Exception {
        URL url = getClass().getClassLoader().getResource("standalone71.xml");
        HostConfiguration hostConfig = new HostConfiguration(new File(url.getPath()));
        assert hostConfig.isNativeLocalOnly() == false;

        url = getClass().getClassLoader().getResource("standalone-1.5.xml");
        hostConfig = new HostConfiguration(new File(url.getPath()));
        assert hostConfig.isNativeLocalOnly() == false;

        url = getClass().getClassLoader().getResource("standalone-1.5-local-native-only.xml");
        hostConfig = new HostConfiguration(new File(url.getPath()));
        assert hostConfig.isNativeLocalOnly() == true;
    }

    public void testReadVault() throws Exception {
        URL url = getClass().getClassLoader().getResource("standalone71.xml");
        HostConfiguration hostConfig = new HostConfiguration(new File(url.getPath()));
        assert hostConfig.getVault() == null;

        url = getClass().getClassLoader().getResource("standalone711.xml");
        hostConfig = new HostConfiguration(new File(url.getPath()));
        Map<String, String> vaultOptions = hostConfig.getVault();
        System.out.println(vaultOptions);
        assert vaultOptions != null;
        assert vaultOptions.size() == 6;

        assert vaultOptions.containsKey("SALT");
        assert vaultOptions.get("SALT").equals("1234abcd");
    }

    public void testReadTruststoreConfigs() throws Exception {
        URL url = getClass().getClassLoader().getResource("standalone71.xml");
        HostConfiguration hostConfig = new HostConfiguration(new File(url.getPath()));
        assert hostConfig.getClientAuthenticationTruststore() == null;
        assert hostConfig.getServerIdentityKeystore() == null;

        url = getClass().getClassLoader().getResource("standalone711.xml");
        hostConfig = new HostConfiguration(new File(url.getPath()));
        TruststoreConfig serverIdentity = hostConfig.getServerIdentityKeystore();
        assert serverIdentity != null;
        assert serverIdentity.getPath().equals("keystore.jks");
        assert serverIdentity.getAlias().equals("as7");
        assert serverIdentity.getKeystorePassword().equals("secure");

        TruststoreConfig clientTruststore = hostConfig.getClientAuthenticationTruststore();
        assert clientTruststore != null;
        assert clientTruststore.getPath().equals("truststore.jks");
        assert clientTruststore.getAlias() == null;
        assert clientTruststore.getKeystorePassword().equals("secured");
    }

}
