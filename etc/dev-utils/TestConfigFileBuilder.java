package org.rhq.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Generates config files for JBossAS and Apache that can be used to get 1000's of services into inventory, for testing
 * purposes.
 *
 * @author Ian Springer
 */
public class TestConfigFileBuilder
{

   private static final int DEFAULT_NUMBER_OF_SERVICES = 1000;

   public static void main(String[] args) throws IOException
   {
      int numberOfServices = args.length == 1 ? Integer.parseInt(args[0]) : DEFAULT_NUMBER_OF_SERVICES;
      buildDestinationsServiceXml(numberOfServices);
      buildVHostsConf(numberOfServices);
   }

   private static void buildDestinationsServiceXml(int numberOfTopics)
         throws FileNotFoundException
   {
      File outFile = new File(System.getProperty("java.io.tmpdir"), "test-destinations-service.xml");
      System.out.println("Writing file " + outFile + " ...");
      PrintStream out = new PrintStream(new FileOutputStream(outFile));
      out.println("<?xml version='1.0'?>\n");
      out.println("<server>\n");
      for (int i = 1; i <= numberOfTopics; i++)
      {
         out.println("  <mbean code='org.jboss.mq.server.jmx.Topic' name='jboss.mq.destination:service=Topic,name=jonTestTopic" + i + "'>");
         out.println("    <depends optional-attribute-name='DestinationManager'>jboss.mq:service=DestinationManager</depends>");
         out.println("  </mbean>\n");
      }
      out.println("</server>");
   }

   private static void buildVHostsConf(int numberOfVHosts) throws FileNotFoundException
   {
      File outFile = new File(System.getProperty("java.io.tmpdir"), "test-vhosts.conf");
      System.out.println("Writing file " + outFile + " ...");
      PrintStream out = new PrintStream(new FileOutputStream(outFile));
      out.println("NameVirtualHost *:80\n");
      for (int i = 1; i <= numberOfVHosts; i++)
      {
         out.println("<VirtualHost *:80>");
         out.println("  ServerName jon-dummy-host" + i + ".example.com:80");
         out.println("</VirtualHost>\n");
      }
   }

}
