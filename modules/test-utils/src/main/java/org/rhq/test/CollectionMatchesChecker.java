package org.rhq.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CollectionMatchesChecker<T> {

    private Collection<T> expected;

    private Collection<T> actual;

    public void setExpected(Collection<T> expected) {
        this.expected = expected;
    }

    public void setActual(Collection<T> actual) {
        this.actual = actual;
    }

    public MatchResult execute() {
        boolean isMatch = true;
        StringBuilder details = new StringBuilder();

        if (expected.size() != actual.size()) {
            isMatch = false;
            details.append("Expected " + expected.size() + " elements but found " + actual.size() + " elements\n");
        }

        List<T> elementsThatShouldBePresent = findMissingElements(expected, actual);
        if (!elementsThatShouldBePresent.isEmpty()) {
            isMatch = false;
            details.append("Expected to find the following elements:\n\t" +
                toString(elementsThatShouldBePresent) + "\n\n");
        }

        List<T> elementsThatShouldNotBePresent = findMissingElements(actual, expected);
        if (!elementsThatShouldNotBePresent.isEmpty()) {
            isMatch = false;
            details.append("Did not expect to find the following elements:\n\t" +
                toString(elementsThatShouldNotBePresent) + "\n\n");
        }

        if (!isMatch) {
            details.append("expected: " + expected + "\n").append("actual: " + actual + "\n\n");
        }

        return new MatchResult(isMatch, details.toString());
    }

    private List<T> findMissingElements(Collection<T> elementsToSearchFor, Collection<T> elementsToSearch) {
        List<T> missingElements = new ArrayList<T>();
        for (T element : elementsToSearchFor) {
            if (!containsMatch(elementsToSearch, element)) {
                missingElements.add(element);
            }
        }
        return missingElements;
    }

    private boolean containsMatch(Collection<T> elementsToSearch, T elementToSearchFor) {
        for (T actual : elementsToSearch) {
            PropertyMatcher<T> matcher = new PropertyMatcher<T>();
            matcher.setExpected(elementToSearchFor);
            matcher.setActual(actual);
            MatchResult result = matcher.execute();

            if (result.isMatch()) {
                return true;
            }
        }
        return false;
    }

    private String toString(List<T> list) {
        StringBuilder buffer = new StringBuilder("[");
        for (T element : list) {
            buffer.append(element.toString() + ", ");
        }
        buffer.delete(buffer.length() - 2, buffer.length());
        buffer.append("]");

        return buffer.toString();
    }

}
