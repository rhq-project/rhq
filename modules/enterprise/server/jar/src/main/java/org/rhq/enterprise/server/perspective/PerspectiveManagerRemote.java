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
package org.rhq.enterprise.server.perspective;

import java.util.Map;

import javax.ejb.Remote;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.server.system.ServerVersion;

@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebService(targetNamespace = ServerVersion.namespace)
@Remote
public interface PerspectiveManagerRemote {

    /**
     * Get the CoreUI context root. This can be used to assemble a url not otherwise obtainable via the API. 
     * This should be used with care as hardcoded paths may break in future releases of the core UI.
     *
     * @param subject
     * @param makeExplicit If true ensure "protocol://host:port" prefix. Set true for remotely deployed perspectives. 
     * @param makeSecure  If true use the secure protocol and port. Ignored if makeExplicit=false or not supported.  
     * @return the Core GUI root url in the format "protocol://host:port/"
     */
    @WebMethod
    String getRootUrl( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "makeExplicit") boolean makeExplicit, //
        @WebParam(name = "makeSecure") boolean makeSecure);

    /**
     * This method does not ensure the specified subject can actually access the requested url.
     * @param subject
     * @param menuItemName The name of the menuItem extension point
     * @param makeExplicit If true ensure "protocol://host:port" prefix. Set true for remotely deployed perspectives. 
     * @param makeSecure  If true use the secure protocol and port. Ignored if makeExplicit=false or not supported.  
     * @return The url for specified extension point. May return null if the extension does not specify a url 
     * @throws IllegalArgumentException if the extension point does not exist. 
     */
    @WebMethod
    String getMenuItemUrl( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "menuItemName") String menuItemName, //
        @WebParam(name = "makeExplicit") boolean makeExplicit, //
        @WebParam(name = "makeSecure") boolean makeSecure);

    /**
     * This method does not ensure the specified subject can actually access the requested url. 
     * @param subject
     * @param tabName The name of the resource tab extension point
     * @param resourceId The resource id to be incorporated into the url. This method does not check the validity 
     * of the resourceId. 
     * @param makeExplicit If true ensure "protocol://host:port" prefix. Set true for remotely deployed perspectives. 
     * @param makeSecure  If true use the secure protocol and port. Ignored if makeExplicit=false or not supported.  
     * @return The url for specified extension point. May return null if the extension does not specify a url 
     * @throws IllegalArgumentException if the extension point does not exist.
     */
    @WebMethod
    String getResourceTabUrl( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "tabName") String tabName, //
        @WebParam(name = "resourceId") int resourceId, //
        @WebParam(name = "makeExplicit") boolean makeExplicit, //
        @WebParam(name = "makeSecure") boolean makeSecure);

    /**
     * This method does not ensure the specified subject can actually access the requested url.
     *  
     * @param subject
     * @param target The target of the navigation link. for example, a role. 
     * @param targetId The id of the specified target. for example, a roleId
     * @param makeExplicit If true ensure "protocol://host:port" prefix. Set true for remotely deployed perspectives. 
     * @param makeSecure  If true use the secure protocol and port. Ignored if makeExplicit=false or not supported.  
     * @return The url for specified target.   
     */
    @WebMethod
    String getTargetUrl( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "target") PerspectiveTarget target, //
        @WebParam(name = "targetId") int targetId, //
        @WebParam(name = "makeExplicit") boolean makeExplicit, //
        @WebParam(name = "makeSecure") boolean makeSecure);

    /**
     * When requesting the same target url for several targets this is a more efficient call than calling
     * getTargetUrl() repeatedly. For example, if generating links to a list of resources.
     *  
     * This method does not ensure the specified subject can actually access the requested urls.
     *  
     * @param subject
     * @param target The target of the navigation link. for example, a role. 
     * @param targetId The id of the specified target. for example, a roleId
     * @param makeExplicit If true ensure "protocol://host:port" prefix. Set true for remotely deployed perspectives. 
     * @param makeSecure  If true use the secure protocol and port. Ignored if makeExplicit=false or not supported.  
     * @return A Map of targetId to url mappings.   
     */
    @WebMethod
    Map<Integer, String> getTargetUrls( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "target") PerspectiveTarget target, //
        @WebParam(name = "targetIds") int[] targetIds, //
        @WebParam(name = "makeExplicit") boolean makeExplicit, //
        @WebParam(name = "makeSecure") boolean makeSecure);

    /**
     * This method does not ensure the specified subject can actually access the requested url.
     *  
     * @param subject
     * @param resourceId The resource id of the specified target. for example, the resource on which an alert is exists
     * @param target The target of the navigation link. for example, an alert. 
     * @param targetId The id of the specified target. for example, an alertId
     * @param makeExplicit If true ensure "protocol://host:port" prefix. Set true for remotely deployed perspectives. 
     * @param makeSecure  If true use the secure protocol and port. Ignored if makeExplicit=false or not supported.  
     * @return The url for specified target.   
     */
    @WebMethod
    String getResourceTargetUrl( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId, //        
        @WebParam(name = "target") PerspectiveTarget target, //
        @WebParam(name = "targetId") int targetId, //
        @WebParam(name = "makeExplicit") boolean makeExplicit, //
        @WebParam(name = "makeSecure") boolean makeSecure);

    /**
     * When requesting the same target url for several resource targets this is a more efficient call than calling
     * getResourceTargetUrl() repeatedly. For example, if generating links to a list of a resource's alerts.
     *  
     * Same This method does not ensure the specified subject can actually access the requested urls. 
     * 
     * @param subject
     * @param resourceId The resource id of the specified target. for example, the resource on which an alert is exists
     * @param target The target of the navigation link. for example, an alert. 
     * @param targetId The id of the specified target. for example, an alertId
     * @param makeExplicit If true ensure "protocol://host:port" prefix. Set true for remotely deployed perspectives. 
     * @param makeSecure  If true use the secure protocol and port. Ignored if makeExplicit=false or not supported.  
     * @return A Map of targetId to url mappings.   
     */
    @WebMethod
    Map<Integer, String> getResourceTargetUrls( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId, //        
        @WebParam(name = "target") PerspectiveTarget target, //
        @WebParam(name = "targetIds") int[] targetIds, //
        @WebParam(name = "makeExplicit") boolean makeExplicit, //
        @WebParam(name = "makeSecure") boolean makeSecure);

    /**
     * This method does not ensure the specified subject can actually access the requested url. 
     * @param subject
     * @param resourceTypeId The resourceType id of the specified target. for example, the type for an alert template
     * @param target The target of the navigation link. for example, an alert template
     * @param targetId The id of the specified target. for example, an alert template definition Id
     * @param makeExplicit If true ensure "protocol://host:port" prefix. Set true for remotely deployed perspectives. 
     * @param makeSecure  If true use the secure protocol and port. Ignored if makeExplicit=false or not supported.  
     * @return The url for specified target.   
     */
    @WebMethod
    String getTemplateTargetUrl( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId, //        
        @WebParam(name = "target") PerspectiveTarget target, //
        @WebParam(name = "targetId") int targetId, //
        @WebParam(name = "makeExplicit") boolean makeExplicit, //
        @WebParam(name = "makeSecure") boolean makeSecure);

}