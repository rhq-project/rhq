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
package org.rhq.enterprise.gui.legacy.util;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Utilities class that provides convenience methods for operating on the servlet context.
 */
@Deprecated
public class ContextUtils {
    //   /**
    //    * Return the cached <code>ServiceLocator</code>, loading it if necessary.
    //    *
    //    * @param context the <code>ServletContext</code>
    //    */
    //   public static ServiceLocator getServiceLocator(ServletContext context)
    //         throws ServiceLocatorException
    //   {
    //      ServiceLocator sl =
    //            (ServiceLocator)context.getAttribute(Constants.SERVICE_LOCATOR_CTX_ATTR);
    //      if (sl == null)
    //      {
    //         Map attrs = getMapOfContextAttributes(context);
    //
    //         sl = new ServiceLocator(attrs);
    //         context.setAttribute(Constants.SERVICE_LOCATOR_CTX_ATTR, sl);
    //      }
    //
    //      return sl;
    //   }

    /**
     * Used by ActionProcessLocator for getting attributes out of the ServletContext
     */
    public static Map getMapOfContextAttributes(ServletContext context) {
        HashMap attrs = new HashMap();
        Enumeration names = context.getAttributeNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            attrs.put(name, context.getAttribute(name));
        }

        return attrs;
    }

    //   /**
    //    * Consult the cached <code>ServiceLocator</code> for an instance of <code>AppdefBoss</code>.
    //    *
    //    * @param context the <code>ServletContext</code>
    //    */
    //   public static AppdefBoss getAppdefBoss(ServletContext context)
    //         throws ServiceLocatorException
    //   {
    //
    //      return getServiceLocator(context).getAppdefBoss();
    //   }
    //
    //   /**
    //    * Consult the cached <code>ServiceLocator</code> for an instance of <code>AppdefBoss</code>.
    //    *
    //    * @param context the <code>ServletContext</code>
    //    */
    //   public static AIBoss getAIBoss(ServletContext context)
    //         throws ServiceLocatorException
    //   {
    //
    //      return getServiceLocator(context).getAIBoss();
    //   }
    //
    //   public static MeasurementScheduleManagerLocal getMeasurmentManager(ServletContext context)
    //   {
    //      return getServiceLocator(context).getMeasurementManager();
    //   }
    //
    //   /**
    //    * Consult the cached <code>ServiceLocator</code> for an instance of <code>ControlBoss</code>.
    //    *
    //    * @param context the <code>ServletContext</code>
    //    */
    //   public static OperationManagerLocal getControlBoss(ServletContext context)
    //         throws ServiceLocatorException
    //   {
    //
    //      return LookupUtil.getOperationManager();
    //   }
    //
    //   /**
    //    * Consult the cached <code>ServiceLocator</code> for an instance of <code>ConfigBoss</code>.
    //    *
    //    * @param context the <code>ServletContext</code>
    //    */
    //   public static ConfigBoss getConfigBoss(ServletContext context)
    //         throws ServiceLocatorException
    //   {
    //      return getServiceLocator(context).getConfigBoss();
    //   }
    //
    //   /**
    //    * Consult the cached <code>ServiceLocator</code> for an instance of <code>AuthBoss</code>.
    //    *
    //    * @param context the <code>ServletContext</code>
    //    */
    //   public static SubjectManagerLocal getAuthBoss(ServletContext context)
    //         throws ServiceLocatorException
    //   {
    //
    //      return getServiceLocator(context).getAuthBoss();
    //   }
    //
    //   /**
    //    * Consult the cached <code>ServiceLocator</code> for an instance of <code>AuthzBoss</code>.
    //    *
    //    * @param context the <code>ServletContext</code>
    //    */
    //   public static AuthzBoss getAuthzBoss(ServletContext context)
    //         throws ServiceLocatorException
    //   {
    //
    //      return getServiceLocator(context).getAuthzBoss();
    //   }
    //
    //   /**
    //    * Consult the cached <code>ServiceLocator</code> for an instance of <code>EventsBoss</code>.
    //    *
    //    * @param context the <code>ServletContext</code>
    //    */
    //   public static EventsBoss getEventsBoss(ServletContext context)
    //         throws ServiceLocatorException
    //   {
    //      return getServiceLocator(context).getEventsBoss();
    //   }
    //
    //
    //   /**
    //    * Consult the cached <code>ServiceLocator</code> for an instance of <code>MeasurementBoss</code>.
    //    *
    //    * @param context the <code>ServletContext</code>
    //    */
    //   public static MeasurementBoss getMeasurementBoss(ServletContext context)
    //         throws ServiceLocatorException
    //   {
    //
    //      return getServiceLocator(context).getMeasurementBoss();
    //   }
    //
    //   /**
    //    * Consult the cached <code>ServiceLocator</code> for an instance of <code>ProductBoss</code>.
    //    *
    //    * @param context the <code>ServletContext</code>
    //    */
    //   public static ProductBoss getProductBoss(ServletContext context)
    //         throws ServiceLocatorException
    //   {
    //
    //      return getServiceLocator(context).getProductBoss();
    //   }
    //
    //   /**
    //    * Consult the cached <code>ServiceLocator</code> for an instance of <code>RtBoss</code>.
    //    *
    //    * @param context the <code>ServletContext</code>
    //    */
    //   public static RtBoss getRtBoss(ServletContext context)
    //         throws ServiceLocatorException
    //   {
    //      return getServiceLocator(context).getRtBoss();
    //   }
    //
    //   /**
    //    * Return the cached <code>List</code> of <code>ResourceTypeValue</code> objects, loading them from the bizapp if
    //    * necessary.
    //    *
    //    * @param context   the <code>ServletContext</code>
    //    * @param sessionId the bizapp session id for the web user
    //    */
    //   public static List getResourceTypes(ServletContext context,
    //                                       Integer sessionId)
    //         throws ServiceLocatorException, NamingException, FinderException,
    //         CreateException, PermissionException, SessionTimeoutException,
    //         SessionNotFoundException, RemoteException
    //   {
    //      List types = (List)context.getAttribute(Constants.RESTYPES_CTX_ATTR);
    //
    //      if (types == null)
    //      {
    //         AuthzBoss boss = getAuthzBoss(context);
    //         types = boss.getAllResourceTypes(sessionId);
    //         types = BizappUtils.filterTypes(types);
    //         context.setAttribute(Constants.RESTYPES_CTX_ATTR, types);
    //      }
    //
    //      return types;
    //   }

    /**
     * Load the specified properties file and return the properties.
     *
     * @param  context  the <code>ServletContext</code>
     * @param  filename the fully qualifed name of the properties file
     *
     * @throws Exception if a problem occurs while loading the file
     */
    public static Properties loadProperties(ServletContext context, String filename) throws Exception {
        Properties props = new Properties();
        InputStream is = context.getResourceAsStream(filename);
        if (is != null) {
            props.load(is);
            is.close();
        }

        return props;
    }

    public static void saveProperties(ServletContext context, String filename, Properties props) throws Exception {
        filename = context.getRealPath(filename);

        FileOutputStream out = new FileOutputStream(filename);
        props.store(out, null);
    }

    public static boolean usingLDAPAuthentication(ServletContext context) throws Exception {
        String provider = (String) context.getAttribute(Constants.JAAS_PROVIDER_CTX_ATTR);
        if (provider == null) {
            provider = LookupUtil.getSystemManager().getSystemConfiguration(
                LookupUtil.getSubjectManager().getOverlord()).getProperty(RHQConstants.JAASProvider);
            context.setAttribute(Constants.JAAS_PROVIDER_CTX_ATTR, provider);
        }

        return (provider != null) && provider.equals(RHQConstants.LDAPJAASProvider);
    }
}