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
package org.rhq.enterprise.gui.util;

import java.util.ArrayList;
import java.util.List;

import org.jvnet.inflector.lang.en.NounPluralizer;
import org.jvnet.inflector.rule.SuffixInflectionRule;
import org.jvnet.inflector.Rule;

/**
 * @author Ian Springer
 */
public class CustomEnglishPluralizer extends NounPluralizer {
    public CustomEnglishPluralizer() {
        super();
        List<Rule> rules = new ArrayList<Rule>();
        rules.add(new SuffixInflectionRule("-y", "-y", "-ies"));
        rules.addAll(getRules());
        setRules(rules);
    }

    @Override
    protected String postProcess(String trimmedWord, String pluralizedWord) {
        return pluralizedWord;
    }
}
