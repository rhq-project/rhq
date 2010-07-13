/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.helpers.bundleGen;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.text.MessageFormat;import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.util.stream.StreamUtil;

/**
 * Main class of a (simple) bundle creator
 * @author Heiko W. Rupp
 */
public class BundleGen {

   private final Log log = LogFactory.getLog(BundleGen.class);

   public static void main(String[] args) throws Exception {
      BundleGen bg = new BundleGen();
      bg.run();
   }


   private void run() {

      ResourceBundle resourceBundle = ResourceBundle.getBundle("bundleGen");
      Props props = new Props();
      BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

      // Ask the user
      XmlQuestionsReader xqr = new XmlQuestionsReader("bundleQuestions");
      try {
         xqr.readQuestions(br,props);
      } catch (Exception e) {
         log.error(e.getMessage());
         return;
      }

      String bundleTargetDirectory = "/tmp"; // TODO ask user?

      String tmpDirPath = System.getProperty("java.io.tmpdir");
      log.debug("Java Tmp dir is " + tmpDirPath);

      String genPath = tmpDirPath + "/" + "bundleGen";
      File tmpDir = new File(genPath); // TODO path separator
      if (!tmpDir.exists() && !tmpDir.mkdir()) {
         log.error(resourceBundle.getString("no.tmp.dir"));
         System.exit(1);
      }

      createFile(props,"deployMain","deploy.xml", genPath);

      try {
         File outFile = new File(genPath,"generatedBundle.zip");
         FileOutputStream fos = new FileOutputStream(outFile);
         ZipOutputStream zos = new ZipOutputStream(fos);

         copyToOutputStream(zos,genPath,"deploy.xml");
         copyToOutputStream(zos,props.getContentDir(),props.getBundleFile());
         zos.flush();
         zos.close();

         File targetFile = new File(bundleTargetDirectory + "/" + "generatedBundle.zip");
         boolean success = outFile.renameTo(targetFile);
         if (success)
            log.info(MessageFormat.format(resourceBundle.getString("bundle.ready"), targetFile.getAbsolutePath()));
         else {
            log.debug("Could not rename file to ["+ targetFile.getAbsolutePath()+"]");
            log.info(MessageFormat.format(resourceBundle.getString("bundle.ready"), outFile.getAbsolutePath()));
         }

      } catch (IOException e) {
         e.printStackTrace();  // TODO: Customise this generated block
      }

   }

   /**
    * Copy a file to the passed Zip Output Stream
    * @param zos OutputStream for a new Zip file
    * @param filedir directory of the file to copy
    * @param fileName name of the file to copy
    * @throws IOException If anything here goes wrong.
    */
   private void copyToOutputStream(ZipOutputStream zos,String filedir, String fileName) throws IOException {
      ZipEntry entry = new ZipEntry(fileName);
      zos.putNextEntry(entry);
      FileInputStream fis = new FileInputStream(new File(filedir , fileName));
      StreamUtil.copy(fis,zos,false);
      zos.flush();
      log.debug("Added ["+ fileName + "]");
      fis.close();
   }

   /**
    * Apply a freemarker template to generate a file
    * @param props The properties used to create the respective file
    * @param template The name of the template without .ftl suffix
    * @param fileName The name of the file to create
    * @param directory The name of the directory to create in
    */
   public void createFile(Props props, String template, String fileName, String directory) {

       try {
           log.info("Trying to generate " + directory + "/" + fileName);
           Configuration config = new Configuration();

           ClassTemplateLoader ctl = new ClassTemplateLoader(getClass(), "/");
           TemplateLoader[] loaders = new TemplateLoader[] { ctl };
           MultiTemplateLoader mtl = new MultiTemplateLoader(loaders);

           config.setTemplateLoader(mtl);

           Template templ = config.getTemplate(template + ".ftl");

           Writer out = new BufferedWriter(new FileWriter(new File(directory, fileName)));
           Map<String, Props> root = new HashMap<String, Props>();
           root.put("props", props);
           templ.process(root, out);
           out.flush();
           out.close();
       } catch (IOException ioe) {
           ioe.printStackTrace();
       } catch (TemplateException te) {
           te.printStackTrace();
       }
   }
}
