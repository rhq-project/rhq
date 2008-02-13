package net.hyperic.hq.product.apache.tool;

import net.hyperic.snmp.SNMPClient;
import net.hyperic.snmp.SNMPSession;
import net.hyperic.snmp.SNMPValue;
import net.hyperic.snmp.SNMPException;

import java.util.Properties;
import java.util.List;
import java.util.ArrayList;

public class ApacheSNMPStatus
{

   public static void main(String[] args) throws Exception
   {
      String snmpIp;
      if (args.length >= 1)
      {
         snmpIp = args[0];
      }
      else
      {
         snmpIp = "127.0.0.1";
      }
      String snmpPort;
      if (args.length >= 2)
      {
         snmpPort = args[1];
      }
      else
      {
         snmpPort = "1610";
      }

      System.out.println("Connecting to Apache HTTP Server SNMP agent at " + snmpIp + ":" + snmpPort + " ...");
      SNMPClient snmpClient = new SNMPClient();
      snmpClient.addMIBs(SNMPClient.class, getMibResourcePaths());
      Properties props = new Properties();
      props.setProperty(SNMPClient.PROP_IP, snmpIp);
      props.setProperty(SNMPClient.PROP_PORT, snmpPort);
      SNMPSession session = snmpClient.getSession(props);

      try
      {
         SNMPValue version = session.getNextValue("wwwServiceDescription");
         System.out.println("Connected!");
         System.out.println("Version: " + version);
      }
      catch (SNMPException e)
      {
         System.err.println("Failed to connect to Apache HTTP Server SNMP agent at " + snmpIp + ":" + snmpPort + ".");
         System.err.println("Details...");
         e.printStackTrace();
         System.exit(1);
      }

      System.out.println("VirtualHosts...");
      List vHostNames = session.getBulk("wwwServiceName");
      List vHostProtocols = session.getBulk("wwwServiceProtocol");
      for (int i = 0; i < vHostNames.size(); i++)
      {
         String vHostProtocol = vHostProtocols.get(i).toString();
         String vHostPort = vHostProtocol.substring(vHostProtocol.lastIndexOf('.') + 1);
         System.out.println("    * " + vHostNames.get(i) + ":" + vHostPort);
      }
   }

   private static String[] getMibResourcePaths()
   {
      List mibs = new ArrayList();

      String baseMibsPrefix;
      // The below hack allows this program to run from the source tree, where the base MIBs are under an ietf-mibs subdir.
      if (SNMPClient.class.getResource("/mibs/ietf-mibs") != null)
      {
         baseMibsPrefix = "ietf-mibs/";
      }
      else
      {
         baseMibsPrefix = "";
      }
      mibs.add(baseMibsPrefix + "SNMPv2-SMI");
      mibs.add(baseMibsPrefix + "SNMPv2-TC");
      mibs.add(baseMibsPrefix + "SNMP-FRAMEWORK-MIB.txt");
      mibs.add(baseMibsPrefix + "NETWORK-SERVICES-MIB.txt");
      mibs.add(baseMibsPrefix + "SYSAPPL-MIB.txt");
      mibs.add(baseMibsPrefix + "WWW-MIB.txt");
      mibs.add(baseMibsPrefix + "SNMPv2-MIB.txt");
      return (String[]) mibs.toArray(new String[mibs.size()]);
   }

}
