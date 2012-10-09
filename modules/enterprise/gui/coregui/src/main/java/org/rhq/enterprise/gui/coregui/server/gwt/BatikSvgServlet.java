/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.server.gwt;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.transcoder.Transcoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.JPEGTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.w3c.dom.Document;


/**
 * BatikSvgServlet that takes SVG XML input as a POST and returns a rasterized image
 * (either jpg or png) of the SVG sent to it.
 * This is useful for gracefully degrading to browsers that dont support SVG such as IE8.
 */
public class BatikSvgServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public BatikSvgServlet() {
        super();
    }

    /*
    * Example svg:
    * <svg xmlns="http://www.w3.org/2000/svg" version="1.1">
    *      <circle cx="100" cy="50" r="40" stroke="black" stroke-width="2" fill="red" />
    * </svg>
    *
    * encode the svg to url format with http://www.motobit.com/util/url-encoder.asp
    *
    * http://localhost:8080/BatikService1/BatikService?image_type=png&svg=%3Csvg+xml...
    * where the ellipsis are the escaped svg
    *
    * Post request has max post size limit set to 2MB (http://tomcat.apache.org/tomcat-5.5-doc/config/http.html)
    * Fix it by changing server.xml to something like below which accepts unlimited size posts(totally dangerous):
    * <Connector connectionTimeout="20000" port="8080" protocol="HTTP/1.1" redirectPort="8443" maxPostSize="0"/>
    */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();

        String svg = request.getParameter("svg");
//        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" width=\"300px\" height=\"300px\" >\n" +
//                "        <circle cx=\"100\" cy=\"50\" r=\"40\" stroke=\"black\" stroke-width=\"2\" fill=\"red\" />\n" +
//                "</svg>";
        String imageType = request.getParameter("image_type");

        InputStream inputStream = new ByteArrayInputStream(svg.getBytes());
        OutputStream outputStream = response.getOutputStream();

        Document doc = getDocument(inputStream);

        TranscoderInput input = new TranscoderInput(doc);
        TranscoderOutput output = new TranscoderOutput(outputStream);

        try {
            Transcoder transcoder = getTranscoder(imageType, 0.7f);
            transcoder.transcode(input, output);
        } catch (TranscoderException e) {
            Log.error("Error in Batik Transcoding:\n"+svg,e);
        }

        Log.debug("Svg: "+svg);
        Log.debug("Transcoded to : "+ imageType + " in "+(System.currentTimeMillis() - startTime) +" ms.");
        response.setContentType("image/" + imageType);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
        IOException {
        doGet(request, response);
    }

    private Document getDocument(InputStream inputStream) throws IOException {
        String parser = XMLResourceDescriptor.getXMLParserClassName();
        SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);
        Document doc = f.createDocument("http://www.w3.org/2000/svg", inputStream);
        return doc;
    }

    private Transcoder getTranscoder(String transcoderType, float keyQuality) {
        Transcoder transcoder = null;
        if (transcoderType.equals("jpg")) {
            transcoder = getJPEGTranscoder(keyQuality);
        } else if (transcoderType.equals("png")) {
            transcoder = getPNGTranscoder();
        }else {
           Log.error("Image type is unknown: "+ transcoderType);
        }
        return transcoder;
    }

    private JPEGTranscoder getJPEGTranscoder(float keyQuality) {
        JPEGTranscoder jpeg = new JPEGTranscoder();
        jpeg.addTranscodingHint(JPEGTranscoder.KEY_QUALITY, new Float(keyQuality));
        return jpeg;
    }

    private PNGTranscoder getPNGTranscoder() {
        return new PNGTranscoder();
    }

}
