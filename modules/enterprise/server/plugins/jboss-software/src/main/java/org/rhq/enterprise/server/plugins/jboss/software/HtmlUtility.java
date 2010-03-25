package org.rhq.enterprise.server.plugins.jboss.software;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Ian Springer
 */
public class HtmlUtility {
    private static final Map<String, Character> ENTITY_TO_CHAR_MAP = new HashMap<String, Character>();

    static {
        ENTITY_TO_CHAR_MAP.put("&lt;", '<');
        ENTITY_TO_CHAR_MAP.put("&gt;", '>');
        ENTITY_TO_CHAR_MAP.put("&amp;", '&');
        ENTITY_TO_CHAR_MAP.put("&quot;", '"');
        ENTITY_TO_CHAR_MAP.put("&agrave;", 'à');
        ENTITY_TO_CHAR_MAP.put("&Agrave;", 'À');
        ENTITY_TO_CHAR_MAP.put("&acirc;", 'â');
        ENTITY_TO_CHAR_MAP.put("&auml;", 'ä');
        ENTITY_TO_CHAR_MAP.put("&Auml;", 'Ä');
        ENTITY_TO_CHAR_MAP.put("&Acirc;", 'Â');
        ENTITY_TO_CHAR_MAP.put("&aring;", 'å');
        ENTITY_TO_CHAR_MAP.put("&Aring;", 'Å');
        ENTITY_TO_CHAR_MAP.put("&aelig;", 'æ');
        ENTITY_TO_CHAR_MAP.put("&AElig;", 'Æ');
        ENTITY_TO_CHAR_MAP.put("&ccedil;", 'ç');
        ENTITY_TO_CHAR_MAP.put("&Ccedil;", 'Ç');
        ENTITY_TO_CHAR_MAP.put("&eacute;", 'é');
        ENTITY_TO_CHAR_MAP.put("&Eacute;", 'É');
        ENTITY_TO_CHAR_MAP.put("&egrave;", 'è');
        ENTITY_TO_CHAR_MAP.put("&Egrave;", 'È');
        ENTITY_TO_CHAR_MAP.put("&ecirc;", 'ê');
        ENTITY_TO_CHAR_MAP.put("&Ecirc;", 'Ê');
        ENTITY_TO_CHAR_MAP.put("&euml;", 'ë');
        ENTITY_TO_CHAR_MAP.put("&Euml;", 'Ë');
        ENTITY_TO_CHAR_MAP.put("&iuml;", 'ï');
        ENTITY_TO_CHAR_MAP.put("&Iuml;", 'Ï');
        ENTITY_TO_CHAR_MAP.put("&ocirc;", 'ô');
        ENTITY_TO_CHAR_MAP.put("&Ocirc;", 'Ô');
        ENTITY_TO_CHAR_MAP.put("&ouml;", 'ö');
        ENTITY_TO_CHAR_MAP.put("&Ouml;", 'Ö');
        ENTITY_TO_CHAR_MAP.put("&oslash;", 'ø');
        ENTITY_TO_CHAR_MAP.put("&Oslash;", 'Ø');
        ENTITY_TO_CHAR_MAP.put("&szlig;", 'ß');
        ENTITY_TO_CHAR_MAP.put("&ugrave;", 'ù');
        ENTITY_TO_CHAR_MAP.put("&Ugrave;", 'Ù');
        ENTITY_TO_CHAR_MAP.put("&ucirc;", 'û');
        ENTITY_TO_CHAR_MAP.put("&Ucirc;", 'Û');
        ENTITY_TO_CHAR_MAP.put("&uuml;", 'ü');
        ENTITY_TO_CHAR_MAP.put("&Uuml;", 'Ü');
        ENTITY_TO_CHAR_MAP.put("&nbsp;", ' ');
        ENTITY_TO_CHAR_MAP.put("&reg;", '\u00a9');
        ENTITY_TO_CHAR_MAP.put("&copy;", '\u00ae');
        ENTITY_TO_CHAR_MAP.put("&euro;", '\u20a0');
    }

    public static String unescapeHTML(String string) {
        if (string == null) {
            return null;
        }
        if (string.length() < 4) {
            // This is an optimization, since we know that no entities are less than 4 characters in length
            // (&lt; and &gt; are the shortest ones).
            return string;
        }

        StringBuilder unescapedString = new StringBuilder((int) (string.length() * 1.1));
        int index = 0;
        while (index < string.length()) {
            int ampersandIndex = string.indexOf("&", index);
            if (ampersandIndex >= 0 && ampersandIndex < (string.length() - 3)) {
                int semicolonIndex = string.indexOf(";", ampersandIndex + 1);
                if (semicolonIndex >= 0) {
                    // Found an entity.

                    // Append the substring from the index up to but not including the ampersand.
                    unescapedString.append(string.substring(index, ampersandIndex));

                    String entity = string.substring(ampersandIndex, semicolonIndex + 1);
                    // See if the entity is one we grok.
                    Character c = ENTITY_TO_CHAR_MAP.get(entity);
                    if (c != null) {
                        // If we were able to map the entity to a character, append the character.
                        unescapedString.append(c);
                    } else {
                        // Otherwise, append the unknown entity as is.
                        unescapedString.append(entity);
                    }

                    index = semicolonIndex + 1;
                } else {
                    // No semicolon found - not an entity. Append the substring from the index up to and including the
                    // ampersand.
                    unescapedString.append(string.substring(index, ampersandIndex + 1));
                    index = ampersandIndex + 1;
                }
            } else {
                // No more entities in the string - append the rest of the original string.
                unescapedString.append(string.substring(index));
                index = string.length();                
            }
        }

        return unescapedString.toString();
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java " + HtmlUtility.class.getName() + " html");
            System.exit(1);
        }
        String html = args[0];
        String unescapedHtml = unescapeHTML(html);
        System.out.println(unescapedHtml);
    }
}
