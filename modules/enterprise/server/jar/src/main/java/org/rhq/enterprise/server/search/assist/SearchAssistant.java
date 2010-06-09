package org.rhq.enterprise.server.search.assist;

import java.util.List;

import org.rhq.core.domain.search.SearchSubsystem;

public interface SearchAssistant {

    SearchSubsystem getSearchSubsystem();

    String getPrimarySimpleContext();

    List<String> getSimpleContexts();

    List<String> getParameterizedContexts();

    boolean isNumericalContext(String context);

    boolean isEnumContext(String context);

    List<String> getParameters(String context, String filter);

    List<String> getValues(String context, String param, String filter);
}
