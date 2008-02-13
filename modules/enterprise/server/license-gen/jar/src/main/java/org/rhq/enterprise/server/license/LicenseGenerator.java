package org.rhq.enterprise.server.license;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.rhq.core.clientapi.util.StringUtil;
import org.rhq.core.util.xmlparser.XmlAttrException;

public final class LicenseGenerator
{

   public static final String EVAL_NAME = "Evaluation Customer";
   public static final String EVAL_EMAIL = "johndoe@example.com";
   public static final String EVAL_PHONE = "";

   public static final String LICENSE_VERSION = "2.0";

   public static final String LICENSE_HEADER
         = "\n<!--"
         + "\n    This license should be uploaded to your RHQ Server in the"
         + "\n    Manage License section of the Administration area. If you have"
         + "\n    not already installed a license, you will be directed to this"
         + "\n    page on your first login to the application."
         + "\n    "
         + "\n    You must login as an administrator to update your license file."
         + "\n-->\n";

   // max 3 platforms in an eval
   public static final int EVAL_MAXPLATFORMS = 20;

   // level 0 support in an eval
   public static final int EVAL_SUPPORT_LEVEL = License.FEATURE_BASIC;

   public static final SimpleDateFormat DFORMAT
         = new SimpleDateFormat("MMM-dd-yyyy HH:mm");

   public LicenseGenerator()
   {
   }

   /**
    * Can be used to generate a license file.
    * <p/>
    * Usage: java this-class "<name>" <email> "phone" <expiration> <platform-limit> "<server-ip-list>"
    * "<plugin-name-list>"
    * <p/>
    * Where: "<name>" is the licensee's name <email> is the licensee's email "<phone>" is the licensee's phone
    * <expiration> is in yyyy-MM-dd format, or '*' for no expiration <platform-limit> is an integer "<server-ip-list>"
    * is a whitespace-separated list of ip addresses "<plugin-name-list>" is a whitespace-separated list of plugin
    * names "<support-level" is 1 for no monitoring support or 3 for monitoring support -- OR -- if arg[0] is "EVAL", a
    * license file suitable for evaluation purposes will be generated.
    */
   public static void main(String[] args)
   {

      LicenseGenerator g = new LicenseGenerator();

      if (args[0].equals("EVAL"))
      {
         try
         {
            g.generateEval(System.out);
         }
         catch (IOException e)
         {
            System.err.println("IOException: " + e);
            e.printStackTrace();
         }
         return;
      }

      int i = 0;
      String name = args[i++];
      String email = args[i++];
      String phone = args[i++];
      String expString = args[i++];
      String aLimitString = args[i++];
      List serverIps = StringUtil.explode(args[i++], " \t\n");
      List plugins = StringUtil.explode(args[i++], " \t\n");
      String supportString = args[i++];

      try
      {
         g.generate(name, email, phone,
               expString, aLimitString, serverIps, plugins,
               supportString, System.out);

      }
      catch (IOException e)
      {
         System.err.println("IOException: " + e);
         e.printStackTrace();
      }
   }

   public void generateEval(OutputStream out) throws IOException
   {


      Calendar expCal = Calendar.getInstance();
      expCal.setTime(new Date());
      expCal.set(Calendar.MONTH, Calendar.NOVEMBER);
      expCal.set(Calendar.DAY_OF_MONTH, 1);
      expCal.set(Calendar.ZONE_OFFSET, ExpirationTag.PST_OFFSET);
      expCal.set(Calendar.DST_OFFSET, ExpirationTag.DST_OFFSET);
      String expiration = ExpirationTag.DFORMAT.format(expCal.getTime());

      // String expiration = LRES.get(LRES.EXPIRATION_NEVER);

      // no server ip or plugin restrictions
      List serverIps = new ArrayList();
      serverIps.add(LRES.get(LRES.IP_ANY));
      List plugins = new ArrayList();
      plugins.add(LRES.get(LRES.PLUGIN_ANY));

      generate(EVAL_NAME,
            EVAL_EMAIL,
            EVAL_PHONE,
            expiration,
            String.valueOf(EVAL_MAXPLATFORMS),
            serverIps,
            plugins,
            String.valueOf(License.FEATURE_ALL),
            out);
   }

   /**
    * From GenericLicenseGenerator, used by ant task
    */
   public void generate(Map licenseData,
                        OutputStream out) throws IOException
   {
      generate((String)licenseData.get("name"),
            (String)licenseData.get("email"),
            (String)licenseData.get("phone"),
            (String)licenseData.get("expiration"),
            (String)licenseData.get("platform-limit"),
            (List)licenseData.get("server-ips"),
            (List)licenseData.get("plugins"),
            (String)licenseData.get("support-level"),
            out);
   }

   /**
    * Generate a license file
    *
    * @param name          The licensee's name
    * @param email         The licensee's email
    * @param phone         The licensee's phone
    * @param expiration    The expiration in yyyy-MM-dd format, or "*"
    * @param platformLimit The platform limit or "*"
    * @param serverIps     a List of Strings that are server IP addresses, or this may a List of length 1, where the
    *                      element at index zero is the String "*"
    * @param plugins       a List of Strings that are plugin names, or this may a List of length 1, where the element
    *                      at index zero is the String "*"
    * @param supportLevel  TODO
    * @param out           This is where the license file will get written. NOTE THAT THE CALLER IS RESPONSIBLE FOR
    *                      OPENING AND CLOSING THIS STREAM.
    */
   public void generate(String name,
                        String email,
                        String phone,
                        String expiration,
                        String platformLimit,
                        List serverIps,
                        List plugins,
                        String supportLevel,
                        OutputStream out)
         throws IOException
   {

      License l = new License();
      LicenseTag lt = new LicenseTag(l);
      String masterKey = lt.generateKey();
      lt.setMasterKey(masterKey);
      lt.setVersion(LICENSE_VERSION);

      LicenseeTag licenseeTag = new LicenseeTag(lt, name, email, phone);
      ExpirationTag exTag = new ExpirationTag(lt);
      PlatformLimitTag plTag = new PlatformLimitTag(lt);
      SupportLevelTag slTag = new SupportLevelTag(lt);

      try
      {
         exTag.handleAttribute(0, expiration);
      }
      catch (XmlAttrException e)
      {
         throw new IOException("Error setting expiration: " + e);
      }
      try
      {
         plTag.handleAttribute(0, platformLimit);
      }
      catch (XmlAttrException e)
      {
         throw new IOException("Error setting platform-limit: " + e);
      }

      ServerIpTag[] ipTags = new ServerIpTag[serverIps.size()];
      for (int i = 0; i < ipTags.length; i++)
      {
         ipTags[i] = new ServerIpTag(lt);
         try
         {
            ipTags[i].handleAttribute(0, (String)serverIps.get(i));
            ipTags[i].handleAttribute(1, String.valueOf(i + 1));
         }
         catch (XmlAttrException e)
         {
            throw new IOException("Error setting server-ip: " + e);
         }
      }

      PluginTag[] pluginTags = new PluginTag[plugins.size()];
      for (int i = 0; i < pluginTags.length; i++)
      {
         pluginTags[i] = new PluginTag(lt);
         try
         {
            pluginTags[i].handleAttribute(0, (String)plugins.get(i));
         }
         catch (XmlAttrException e)
         {
            throw new IOException("Error setting plugin: " + e);
         }
      }

      try
      {
         slTag.handleAttribute(0, supportLevel);
      }
      catch (XmlAttrException e)
      {
         throw new IOException("Error setting support-level: " + e);
      }

      StringBuilder sb = new StringBuilder();
      sb.append("<?xml version=\"1.0\"?>\n")
            .append(LICENSE_HEADER)
            .append("\n<!-- License created: ")
            .append(DFORMAT.format(new Date())).append(" -->")
            .append("\n<").append(lt.getName()).append(" ")
            .append(LRES.get(LRES.ATTR_KEY)).append("=\"").append(lt.getMasterKey()).append("\" ")
            .append(LRES.get(LRES.ATTR_VERSION)).append("=\"").append(lt.getVersion()).append("\">\n");

      // license-owner
      sb.append("\n\t<").append(licenseeTag.getName()).append(" ")
            .append(LRES.get(LRES.ATTR_NAME))
            .append("=\"").append(licenseeTag.getLicenseeName()).append("\" ")
            .append("\n\t               ").append(LRES.get(LRES.ATTR_EMAIL))
            .append("=\"").append(licenseeTag.getLicenseeEmail()).append("\" ")
            .append("\n\t               ").append(LRES.get(LRES.ATTR_PHONE))
            .append("=\"").append(licenseeTag.getLicenseePhone()).append("\" ")
            .append("\n\t               ").append(LRES.get(LRES.ATTR_KEY))
            .append("=\"").append(licenseeTag.generateKey()).append("\"/>\n\n");

      // expiration
      sb.append("\t<").append(exTag.getName()).append(" ")
            .append(LRES.get(LRES.ATTR_DATE))
            .append("=\"").append(exTag.getOriginalExpirationString())
            .append("\" ")
            .append(LRES.get(LRES.ATTR_KEY))
            .append("=\"").append(exTag.generateKey()).append("\" ")
            .append("/>\n");

      // platform count
      sb.append("\t<").append(plTag.getName()).append(" ")
            .append(LRES.get(LRES.ATTR_COUNT))
            .append("=\"").append(plTag.getOriginalPlatformLimitString())
            .append("\" ")
            .append(LRES.get(LRES.ATTR_KEY))
            .append("=\"").append(plTag.generateKey()).append("\" ")
            .append("/>\n");

      // server ips
      for (int i = 0; i < ipTags.length; i++)
      {
         sb.append("\t<").append(ipTags[i].getName()).append(" ")
               .append(LRES.get(LRES.ATTR_COUNT))
               .append("=\"").append(ipTags[i].getCount()).append("\" ")
               .append(LRES.get(LRES.ATTR_ADDRESS))
               .append("=\"").append(ipTags[i].getAddress()).append("\" ")
               .append(LRES.get(LRES.ATTR_KEY))
               .append("=\"").append(ipTags[i].generateKey()).append("\" ")
               .append("/>\n");
      }

      // plugins
      for (int i = 0; i < pluginTags.length; i++)
      {
         sb.append("\t<").append(pluginTags[i].getName()).append(" ")
               .append(LRES.get(LRES.ATTR_NAME))
               .append("=\"").append(pluginTags[i].getPlugin()).append("\" ")
               .append(LRES.get(LRES.ATTR_KEY))
               .append("=\"").append(pluginTags[i].generateKey()).append("\" ")
               .append("/>\n");
      }

      // support level
      sb.append("\t<").append(slTag.getName()).append(" ")
            .append(LRES.get(LRES.ATTR_LEVEL))
            .append("=\"").append(slTag.getOriginalSupportLevelString())
            .append("\" ")
            .append(LRES.get(LRES.ATTR_KEY))
            .append("=\"").append(slTag.generateKey()).append("\" ")
            .append("/>\n");

      sb.append("\n</license>\n");

      // write to stream
      BufferedWriter bw;
      bw = new BufferedWriter(new OutputStreamWriter(out));
      bw.write(sb.toString());
      bw.flush();
   }
}
