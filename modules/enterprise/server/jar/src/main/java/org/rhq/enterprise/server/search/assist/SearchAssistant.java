package org.rhq.enterprise.server.search.assist;

import java.util.List;

import org.rhq.core.domain.search.SearchSubsystem;

public interface SearchAssistant {

    public SearchSubsystem getSearchSubsystem();

    public List<String> getSimpleContexts();

    public List<String> getParameterizedContexts();

    public List<String> getParameters(String context, String filter);

    public List<String> getValues(String context, String param, String filter);

}
