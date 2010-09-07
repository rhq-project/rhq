package org.rhq.enterprise.gui.coregui.client.util.effects;

import java.util.ArrayList;
import java.util.List;

public class ColoringUtility {
    public static String highlight(String data, String filter) {
        return highlight(data, filter, "yellow");
    }

    public static String highlight(String data, String filter, String color) {
        String lowerData = data.toLowerCase();
        String lowerFilter = filter.toLowerCase();

        List<Integer> matchingIndices = new ArrayList<Integer>();

        int startFrom = -1;
        while ((startFrom = lowerData.indexOf(lowerFilter, startFrom + 1)) != -1) {
            matchingIndices.add(startFrom);
        }

        String style = "background-color: " + color;
        for (int i = matchingIndices.size() - 1; i >= 0; i--) {
            Integer nextMatchingIndex = matchingIndices.get(i);
            data = decorate(data, style, nextMatchingIndex, nextMatchingIndex + filter.length());
        }

        return data;
    }

    private static String decorate(String data, String style, int startIndex, int endIndex) {
        if (startIndex == -1 || (startIndex == endIndex)) {
            return data; // no match or zero-width match
        }

        String[] words = data.split("<br/>");
        int counter = 0;
        int wordIndex = 0;
        int letterIndex = 0;

        StringBuilder results = new StringBuilder();
        while (counter < startIndex) {
            if (wordIndex == words.length) {
                break;
            }

            if (letterIndex < words[wordIndex].length()) { // more letters left in the current word?
                results.append(words[wordIndex].charAt(letterIndex)); // append the next char of the current word
                letterIndex++; // move to the next char in the current word
                counter++; // only move counter forward when we've added non-BR chars to the results
            } else { // next word
                results.append("<br/>"); // put the break point back in between words
                letterIndex = 0; // point to the first char
                wordIndex++; // of the next word
            }
        }

        // we're at start index, wrap all words and word fragments in the specified style up to endIndex
        results.append("<span style=\"" + style + "\">"); // seed action
        while (counter < endIndex) {
            if (wordIndex == words.length) {
                break;
            }

            if (letterIndex < words[wordIndex].length()) { // more letters left in the current word?
                results.append(words[wordIndex].charAt(letterIndex)); // append the next char of the current word
                letterIndex++; // move to the next char in the current word
                counter++; // only move counter forward when we've added non-BR chars to the results
            } else { // next word
                results.append("</span>"); // close the previous word, we don't highlight breaks
                results.append("<br/>"); // put the break point back in between words
                letterIndex = 0; // point to the first char
                wordIndex++; // of the next word
                results.append("<span style=\"" + style + "\">"); // prepare for next word
            }
        }
        results.append("</span>"); // end last dangling span

        // append the rest of the current word fragment, if any
        if (wordIndex != words.length) {
            results.append(words[wordIndex].substring(letterIndex));
        }

        // append the rest of the words
        while (++wordIndex < words.length) {
            results.append("<br/>"); // put the break point back in between words
            results.append(words[wordIndex]);
        }

        return results.toString();
    }
}
