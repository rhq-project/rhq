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
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;

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
 *     <td>imageFormat</td>
 *     <td>(<b>png</b> | jpeg)</td>
 *   </tr>
 *   <tr>
 *     <td>imageWidth</td>
 *     <td>&lt;integer <b>(700)</b>&gt;</td>
 *   </tr>
 *   <tr>
 *     <td>imageHeight</td>
 *     <td>&lt;integer <b>(350)</b>&gt;</td>
 *   </tr>
 * </table>
 */
public abstract class ImageServlet extends ParameterizedServlet {
    /**
     * Request parameter for image format.
     */
    public static final String IMAGE_FORMAT_PARAM = "imageFormat";

    /**
     * Request parameter value representing a PNG image.
     */
    public static final String IMAGE_FORMAT_PNG = "png";

    /**
     * Request parameter value representing a JPEG image.
     */
    public static final String IMAGE_FORMAT_JPEG = "jpeg";
    private static final String[] VALID_IMAGE_FORMATS = { IMAGE_FORMAT_PNG, IMAGE_FORMAT_JPEG };

    private static final String PNG_MIME_TYPE = "image/png";
    private static final String JPEG_MIME_TYPE = "image/jpeg";

    /**
     * Request parameter for image width.
     */
    public static final String IMAGE_WIDTH_PARAM = "imageWidth";

    /**
     * Default image width.
     */
    public static final int IMAGE_WIDTH_DEFAULT = 700;

    /**
     * Request parameter for image height.
     */
    public static final String IMAGE_HEIGHT_PARAM = "imageHeight";

    /**
     * Default image height.
     */
    public static final int IMAGE_HEIGHT_DEFAULT = 350;

    // member data
    private Log log = LogFactory.getLog(ImageServlet.class.getName());
    private String imageFormat;
    private int imageWidth;
    private int imageHeight;

    public ImageServlet() {
    }

    public void init() {
        if (log.isDebugEnabled()) {
            log.debug("java.awt.headless=" + System.getProperty("java.awt.headless"));
        }
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        try {
            // parse the parameters
            log.debug("Parsing parameters.");
            parseParameters(request);

            Object imgObj = createImage(request);

            // render the chart
            log.debug("Rendering image.");
            ServletOutputStream out = response.getOutputStream();

            // we _never_ want the result cached by the browser, ever
            RequestUtils.bustaCache(request, response);

            if (getImageFormat().equals(IMAGE_FORMAT_PNG)) {
                response.setContentType(PNG_MIME_TYPE);
                renderPngImage(out, imgObj);
            } else {
                response.setContentType(JPEG_MIME_TYPE);
                renderJpegImage(out, imgObj);
            }

            out.flush();
        } catch (IOException e) {
            // it's okay to ignore this one
            log.debug("Error writing image to response.", e);
        } catch (Exception e) {
            log.error("Unknown error.", e);
            throw new ServletException("Unknown error.", e);
        }
    }

    /**
     * Create the image being rendered.
     *
     * @param request the servlet request
     */
    protected abstract Object createImage(HttpServletRequest request) throws ServletException;

    /**
     * Render a PNG version of the image into the output stream.
     *
     * @param out the output stream
     */
    protected abstract void renderPngImage(ServletOutputStream out, Object imgObj) throws IOException;

    /**
     * Render a JPEG version of the image into the output stream.
     *
     * @param out the output stream
     */
    protected abstract void renderJpegImage(ServletOutputStream out, Object imgObj) throws IOException;

    /**
     * This method will be called automatically by the ImageServlet. It should handle the parsing and error-checking of
     * any specific parameters for the chart being rendered.
     *
     * @param request the HTTP request object
     */
    protected void parseParameters(HttpServletRequest request) {
        // image format
        imageFormat = parseStringParameter(request, IMAGE_FORMAT_PARAM, getDefaultImageFormat(), VALID_IMAGE_FORMATS);

        // image width
        imageWidth = parseIntParameter(request, IMAGE_WIDTH_PARAM, getDefaultImageWidth());

        // image height
        imageHeight = parseIntParameter(request, IMAGE_HEIGHT_PARAM, getDefaultImageHeight());

        _logParameters();
    }

    /**
     * Return the image format.
     *
     * @return <code>{@link IMAGE_FORMAT_PNG}</code> or <code>{@link IMAGE_FORMAT_JPEG}</code>
     */
    protected String getImageFormat() {
        return imageFormat;
    }

    /**
     * Return the image height.
     *
     * @return the height of the image
     *
     * @see    <code>{@link IMAGE_HEIGHT_DEFAULT}</code>
     */
    protected int getImageHeight() {
        return imageHeight;
    }

    /**
     * Return the image width.
     *
     * @return the width of the image
     *
     * @see    <code>{@link IMAGE_WIDTH_DEFAULT}</code>
     */
    protected int getImageWidth() {
        return imageWidth;
    }

    /**
     * Return the default <code>imageFormat</code>.
     */
    protected String getDefaultImageFormat() {
        return IMAGE_FORMAT_PNG;
    }

    /**
     * Return the default <code>imageWidth</code>.
     */
    protected int getDefaultImageWidth() {
        return IMAGE_WIDTH_DEFAULT;
    }

    /**
     * Return the default <code>imageHeight</code>.
     */
    protected int getDefaultImageHeight() {
        return IMAGE_HEIGHT_DEFAULT;
    }

    //---------------------------------------------------------------
    //-- private helpers
    //---------------------------------------------------------------
    private void _logParameters() {
        if (log.isDebugEnabled()) {
            StringBuffer sb = new StringBuffer("Parameters:");
            sb.append("\n");
            sb.append("\t");
            sb.append(IMAGE_FORMAT_PARAM);
            sb.append(": ");
            sb.append(imageFormat);
            sb.append("\n");
            sb.append("\t");
            sb.append(IMAGE_WIDTH_PARAM);
            sb.append(": ");
            sb.append(imageWidth);
            sb.append("\n");
            sb.append("\t");
            sb.append(IMAGE_HEIGHT_PARAM);
            sb.append(": ");
            sb.append(imageHeight);
            log.debug(sb.toString());
        }
    }
}