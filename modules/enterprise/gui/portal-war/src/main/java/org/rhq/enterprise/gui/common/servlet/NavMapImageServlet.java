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
package org.rhq.enterprise.gui.common.servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.enterprise.gui.image.WebImage;
import org.rhq.enterprise.gui.image.widget.ResourceTree;

/**
 * <p>This servlet returns a response that contains the binary data of an image (JPEG or PNG) that can be viewed in a
 * web browser.</p>
 *
 * <p>The navigation map servlet takes the following parameters (any applicable defaults are in <b>bold</b> and required
 * parameters are in <i>italics</i>):</p>
 *
 * <table border="1">
 *   <tr>
 *     <th>key</th>
 *     <th>value</th>
 *   </tr>
 *   <tr>
 *     <td> <i>treeVar</i></td>
 *     <td>&lt;string&gt;</td>
 *   </tr>
 * </table>
 */
public class NavMapImageServlet extends ImageServlet {
    /**
     * Request parameter for the tree variable session attribute.
     */
    public static final String TREE_VAR_PARAM = "treeVar";

    /**
     * Default image width.
     */
    public static final int IMAGE_WIDTH_DEFAULT = 800;

    // member data
    private Log log = LogFactory.getLog(NavMapImageServlet.class.getName());
    private String treeVar;

    public NavMapImageServlet() {
    }

    /**
     * Create the image being rendered.
     *
     * @param request the servlet request
     */
    protected Object createImage(HttpServletRequest request) throws ServletException {
        WebImage image = (ResourceTree) request.getSession().getAttribute(treeVar);
        request.getSession().removeAttribute(treeVar);
        return image;
    }

    /**
     * Render a PNG version of the image into the output stream.
     *
     * @param out the output stream
     */
    protected void renderPngImage(ServletOutputStream out, Object imgObj) throws IOException {
        WebImage image = (WebImage) imgObj;
        if (null != image) {
            image.writePngImage(out);
        }
    }

    /**
     * Render a JPEG version of the image into the output stream.
     *
     * @param out the output stream
     */
    protected void renderJpegImage(ServletOutputStream out, Object imgObj) throws IOException {
        WebImage image = (WebImage) imgObj;
        if (null != image) {
            image.writeJpegImage(out);
        }
    }

    /**
     * This method will be called automatically by the ChartServlet. It should handle the parsing and error-checking of
     * any specific parameters for the chart being rendered.
     *
     * @param request the HTTP request object
     */
    protected void parseParameters(HttpServletRequest request) {
        super.parseParameters(request);

        // chart data key
        treeVar = parseRequiredStringParameter(request, TREE_VAR_PARAM);

        _logParameters();
    }

    /**
     * Return the default <code>imageWidth</code>.
     */
    protected int getDefaultImageWidth() {
        return IMAGE_WIDTH_DEFAULT;
    }

    //---------------------------------------------------------------
    //-- private helpers
    //---------------------------------------------------------------
    private void _logParameters() {
        if (log.isDebugEnabled()) {
            StringBuffer sb = new StringBuffer("Parameters:");
            sb.append("\n");
            sb.append("\t");
            sb.append(TREE_VAR_PARAM);
            sb.append(": ");
            sb.append(treeVar);
            log.debug(sb.toString());
        }
    }
}

// EOF
