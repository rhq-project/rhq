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
package org.rhq.helpers.bundleGen;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Locale;
import java.lang.reflect.Method;
import java.util.ResourceBundle;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Reads questions from an XML file and shows them to the user.
 * The reader first tries to load a locale based version of the
 * questions (file.&lt;locale&gt;.xml) if this fails, it falls back
 * to using a default version (file.xml)
 *
 * @author Heiko W. Rupp
 */
public class XmlQuestionsReader {

   private final Log log = LogFactory.getLog(XmlQuestionsReader.class);
   String baseName;
   ResourceBundle resourceBundle = ResourceBundle.getBundle("bundleGen");

   /**
    * Create a new XmlQuestionReader
    * @param fileBaseName the basename of a file - sans .xml suffix!
    */
   public XmlQuestionsReader(String fileBaseName) {
      baseName = fileBaseName;
   }

   /**
    * Present the questions to the user and read answers from the provided <i>reader</i>.
    * @param reader Buffered Reader to read from. Usually an InputStreamReader(System.in)
    * @param props Props object to fill the answers into.
    * @throws FileNotFoundException if no question file is found
    * @throws Exception On various other occasions
    */
   public void readQuestions(BufferedReader reader, Props props) throws Exception {

      try {
         DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
         DocumentBuilder builder = factory.newDocumentBuilder();
         Class<? extends XmlQuestionsReader> clazz = getClass();
         String lang = Locale.getDefault().getLanguage();
         InputStream input = clazz.getResourceAsStream("/"+baseName +".xml" );
         if (input == null)  {
            throw new FileNotFoundException("Input file [" + baseName + "] not found");
         }

         Document doc = builder.parse(input);
         Element root = doc.getDocumentElement(); // <questions>
         NodeList questionNodes = root.getChildNodes();//  "question"

         for (int i = 0; i < questionNodes.getLength(); i++ ) {
            Node node = questionNodes.item(i);
            if (node instanceof Element) {
               Element question = (Element)node;
               String prop = question.getAttribute("prop");
               String type = question.getAttribute("type");
               if (type.equals(""))
                  type="string";
               String aIf = question.getAttribute("if"); // TODO implement

               if (!isValidType(type)) {
                  System.err.println("Type " + type + " is invalid in " + question );
                  return;
               }
               Element text = getElementForLang(question,"text",lang);
               NodeList prefillNodes = question.getElementsByTagName("prefill");
               String prefill = null;
               if (prefillNodes.getLength()>0) {
                  prefill = prefillNodes.item(0).getTextContent();
               }
               Element helpNode = getElementForLang(question,"help",lang);
               String help = null;
               if (helpNode!=null) {
                  help = helpNode.getTextContent();
               }

               boolean isBool = false;
               System.out.print(text.getTextContent());
               if ("bool".equals(type)) {
                  isBool = true;
                  System.out.print(resourceBundle.getString("yes.no"));
               }
               if (prefill!=null) {
                  System.out.print("[" + prefill + "]");
               }

               String answer;
               boolean helpRequested = false;
               do {
                  helpRequested = false;
                  System.out.print(": ");
                  answer = reader.readLine();
                  if (answer.startsWith("?")) {
                     helpRequested = true;
                     if (help!=null) {
                        System.out.println(help);
                     }
                     else
                        System.out.println(resourceBundle.getString("no.help.available"));
                  }

               } while (helpRequested);

               String setterName = "set" + caps(prop);
               Method setter;
               if (isBool)
                  setter = Props.class.getMethod(setterName, Boolean.TYPE);
               else
                  setter = Props.class.getMethod(setterName, String.class);

               if (isBool) {
                  String yesString = resourceBundle.getString("yes.key");
                  if (answer.toLowerCase(Locale.getDefault()).startsWith(yesString)) {
                     setter.invoke(props, true);
                  }
               }
               else if ("string".equals(type)) {
                  // If only return pressed and we have a prefill, use this.
                  if (prefill!= null && answer.length()==0) {
                     setter.invoke(props,prefill);
                  }
                  else {
                     if (!answer.startsWith("\n") && !answer.startsWith("\r") && !(answer.length() == 0))
                        setter.invoke(props,answer);
                  }
               }
               else {
                  // TODO
               }
            }




         }
      }
      catch (Exception e) {
         throw e;
      }

   }

   /**
    * Search for an element <i>tagWanted</i> within parent. If multiple
    * elements are present, use the one with the matching <i>lang</i>.
    * If no matching element is present, use one without a <i>lang</i>
    * attribute
    * @param parent containing element
    * @param tagWanted the tag to search for
    * @param lang the desired language version
    * @return an element or null if not found
    */
   Element getElementForLang(Element parent, String tagWanted, String lang) {
      NodeList elements = parent.getElementsByTagName(tagWanted);
      if (elements==null || elements.getLength()==0)
         return null;

      Element noLang = (Element) elements.item(0);

      for (int i = 0; i < elements.getLength(); i++) {
         Element ele = (Element) elements.item(i);
         String attr = ele.getAttribute("lang");
         if (attr==null || attr.isEmpty()) {
            noLang = ele;
         }
         else if (attr.equals(lang)) {
            return ele;
         }
      }
      return noLang;
   }
   /**
    * Returns true if the passed type is a valid data type of the properties (not for a single
    * property).
    * @param type A type as string
    * @return true if its valid, false otherwise
    */
   private boolean isValidType(String type) {

      if ("bool".equals(type) || "string".equals(type) || "resourceCategory".equals(type))
         return true;

      return false;
   }

   static String caps(String in) {
      if (in == null)
         return null;

      return in.substring(0, 1).toUpperCase(Locale.getDefault()) + in.substring(1);
   }
}
