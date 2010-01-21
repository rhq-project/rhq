package org.rhq.core.template;

import java.util.Map;

public class TemplateEngine {
	public final Map<String, String> tokens;

	public TemplateEngine(Map<String, String> tokens) {
		super();
		this.tokens = tokens;
	}

	public String replaceTokens(String input){
		
		for (String  token : tokens.keySet()) {
			//Since . is a special regex character, 
			//replace it with \. before building the larger regex
			String regex = "<%\\s*" + token.replaceAll("\\.", "\\\\.") + "\\s*%>";
			input = input.replaceAll(regex, tokens.get(token) );
		}
		
		return input;
	}
}
