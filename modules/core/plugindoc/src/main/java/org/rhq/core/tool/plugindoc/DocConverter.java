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
package org.rhq.core.tool.plugindoc;

/**
 * @author Joseph Marques
 */
public class DocConverter {
    public static String htmlToDocBook(String htmlContent) {
        if (htmlContent == null) {
            return null;
        }

        String result = htmlContent;

        while (true) {
            int startHeaderIndex = result.indexOf("<h3>");
            if (startHeaderIndex == -1) {
                break;
            }
            int endHeaderIndex = result.indexOf("</h3>", startHeaderIndex + 4);
            int startParaIndex = result.indexOf("<p>", endHeaderIndex + 5);
            int endParaIndex = result.indexOf("</p>", startParaIndex + 3);

            String firstPart = result.substring(0, startHeaderIndex);
            String header = result.substring(startHeaderIndex + 4, endHeaderIndex);
            String middle = result.substring(endHeaderIndex + 5, startParaIndex);
            String paragraph = result.substring(startParaIndex + 3, endParaIndex);
            String lastPart = result.substring(endParaIndex + 4);

            String replacement = "<formalpara><title>" + header + "</title>" + middle + "<para>" + paragraph
                + "</para></formalpara>";
            result = firstPart + replacement + lastPart;
        }

        String[][] tagConversions = { //
        new String[] { "<code>", "<screen>" }, //
            new String[] { "</code>", "</screen>" }, //
            new String[] { "<tt>", "<filename>" }, //
            new String[] { "</tt>", "</filename>" }, //
            new String[] { "<p>", "<para>" }, //
            new String[] { "</p>", "</para>" }, //
            new String[] { "<pre>", "" }, //
            new String[] { "</pre>", "" }, //
            new String[] { "<a href=", "<ulink url=" }, //
            new String[] { "</a>", "</ulink>" }, //
            new String[] { "<div class=\"note\">", "<important><title>Important</title><para>" }, //
            new String[] { "</div>", "</para></important>" }, //
        };

        result = convert(result, tagConversions);

        return result;
    }

    public static String htmlToConfluence(String htmlContent) {
        if (htmlContent == null) {
            return null;
        }

        String result = htmlContent;

        while (true) {
            int startLinkIndex = result.indexOf("<a ");
            if (startLinkIndex == -1) {
                break;
            }
            int endLinkIndex = result.indexOf("</a>", startLinkIndex + 1);

            String firstPart = result.substring(0, startLinkIndex);
            String htmlLink = result.substring(startLinkIndex, endLinkIndex);
            String lastPart = result.substring(endLinkIndex + 4);

            int hrefIndex = htmlLink.indexOf("href=");
            int startQuoteIndex = hrefIndex + 5;
            int endQuoteIndex = htmlLink.indexOf("\"", startQuoteIndex + 1);
            int endTagIndex = htmlLink.indexOf(">", endQuoteIndex + 1);

            String url = htmlLink.substring(startQuoteIndex + 1, endQuoteIndex);
            String label = htmlLink.substring(endTagIndex + 1);

            String replacement = "[" + label + "|" + url + "]";
            result = firstPart + replacement + lastPart;
        }

        String[][] tagConversions = { //
        new String[] { "<code>", "{code}" }, //
            new String[] { "</code>", "{code}" }, //
            new String[] { "<tt>", "{{" }, //
            new String[] { "</tt>", "}}" }, //
            new String[] { "<p>", "" }, //
            new String[] { "</p>", "" }, //
            new String[] { "<pre>", "" }, //
            new String[] { "</pre>", "" }, //
            new String[] { "<div class=\"note\">", "{note}" }, //
            new String[] { "</div>", "{note}" }, //
            new String[] { "<h3>", "h3. " }, //
            new String[] { "</h3>", "" }, //
        };

        result = convert(result, tagConversions);

        return result;
    }

    private static String convert(String message, String[][] tagConversions) {
        String result = message;
        for (String[] conversion : tagConversions) {
            String from = conversion[0];
            String to = conversion[1];
            while (result.indexOf(from) != -1) {
                result = result.replaceFirst(from, to);
            }
        }

        return result;
    }

    public static void testHtmlToDocBook() {
        String html = "" //
            + "   <h3>header</h3>\n" //
            + "   \n" //
            + "   <p>some paragraph\n" //
            + "   <a href=\"url\">label</a> with <tt>property</tt> \n" //
            + "   <div class=\"note\">some urgent thing</div> </p>";

        String expectedDocBookFormat = "" //
            + "   <formalpara><title>header</title>\n" //
            + "   \n" //
            + "   <para>some paragraph\n" //
            + "   <ulink url=\"url\">label</ulink> with <filename>property</filename> \n" //
            + "   <important><title>Important</title><para>some urgent thing</para></important> </para></formalpara>";

        String actualDocBookFormat = DocConverter.htmlToDocBook(html);
        if (actualDocBookFormat.equals(expectedDocBookFormat) == false) {
            System.out.println("Expected: \"" + expectedDocBookFormat + "\"");
            System.out.println("Actual:   \"" + actualDocBookFormat + "\"");
        } else {
            System.out.println("Success");
        }
    }

    public static void testHtmlToConfluence() {
        String html = "" //
            + "   <h3>header</h3>\n" //
            + "   \n" //
            + "   <p>some paragraph\n" //
            + "   <a href=\"url\">label</a> with <tt>property</tt> \n" //
            + "   <div class=\"note\">some urgent thing</div> </p>";

        String expectedDocBookFormat = "" //
            + "   h3. header\n" //
            + "   \n" //
            + "   some paragraph\n" //
            + "   [label|url] with {{property}} \n" //
            + "   {note}some urgent thing{note} ";

        String actualDocBookFormat = DocConverter.htmlToConfluence(html);
        if (actualDocBookFormat.equals(expectedDocBookFormat) == false) {
            System.out.println("Expected: \"" + expectedDocBookFormat + "\"");
            System.out.println("Actual:   \"" + actualDocBookFormat + "\"");
        } else {
            System.out.println("Success");
        }
    }

    public static void main(String[] args) {
        testHtmlToDocBook();
        testHtmlToConfluence();
    }
}
