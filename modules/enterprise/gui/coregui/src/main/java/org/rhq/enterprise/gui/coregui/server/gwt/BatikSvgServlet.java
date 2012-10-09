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
import org.apache.batik.transcoder.image.ImageTranscoder;
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
    private static final float IMAGE_QUALITY = 0.8f; // 0 - 99 only used for JPEG images

    public BatikSvgServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();

        String svg = request.getParameter("svg");
//        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" width=\"300px\" height=\"300px\" >\n" +
//                "        <circle cx=\"100\" cy=\"50\" r=\"40\" stroke=\"black\" stroke-width=\"2\" fill=\"red\" />\n" +
//                "</svg>";
        String imageType = request.getParameter("image_type");

        if(imageType == null){
           imageType = "png";
        }

        if(svg == null || svg.isEmpty()){
           throw new IllegalArgumentException("svg parameter not set") ;
        }

        InputStream inputStream = new ByteArrayInputStream(svg.getBytes());
        OutputStream outputStream = response.getOutputStream();

        Document doc = getDocument(inputStream);

        TranscoderInput input = new TranscoderInput(doc);
        TranscoderOutput output = new TranscoderOutput(outputStream);

        try {
            Transcoder transcoder = getTranscoder(imageType, IMAGE_QUALITY);
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
           throw new UnsupportedOperationException("Unknown Image type: "+transcoderType);
        }
        // add any external stylesheets that are needed for rendering
        transcoder.addTranscodingHint(ImageTranscoder.KEY_USER_STYLESHEET_URI, "http://nvd3.com/src/nv.d3.css");
        return transcoder;
    }

    private JPEGTranscoder getJPEGTranscoder(float imageQuality) {
        JPEGTranscoder jpeg = new JPEGTranscoder();
        jpeg.addTranscodingHint(JPEGTranscoder.KEY_QUALITY, new Float(imageQuality));
        return jpeg;
    }

    private PNGTranscoder getPNGTranscoder() {
        return new PNGTranscoder();
    }

}
