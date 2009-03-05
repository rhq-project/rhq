/*
 * JBoss, a division of Red Hat.
 * Copyright 2007, Red Hat Middleware, LLC. All rights reserved.
 */

package org.rhq.enterprise.client;

import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.Configuration;

import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBContext;

public class JaxbTester
{

   public static void main(String[] args) throws JAXBException
   {
      Configuration c = new Configuration();
      c.put(new PropertySimple("hello","world"));

      JAXBContext context = JAXBContext.newInstance(Configuration.class, Property.class);
      context.createMarshaller().marshal(c,System.out);
   }
}
