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

import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;

import org.jvnet.inflector.rule.SuffixInflectionRule;
import org.jvnet.inflector.rule.RegexReplacementRule;
import org.jvnet.inflector.rule.CategoryInflectionRule;
import org.jvnet.inflector.rule.AbstractRegexReplacementRule;
import org.jvnet.inflector.rule.IrregularMappingRule;
import static org.jvnet.inflector.rule.AbstractRegexReplacementRule.disjunction;
import static org.jvnet.inflector.rule.IrregularMappingRule.toMap;
import org.jvnet.inflector.Rule;
import org.jvnet.inflector.RuleBasedPluralizer;

/**
 * A tweaked copy of {@link org.jvnet.inflector.lang.en.NounPluralizer}. Comments labeled with "[RHQ]" describe the
 * tweaks.
 *
 * @author Ian Springer
 */
public class CustomEnglishPluralizer extends RuleBasedPluralizer {
    // TODO understand this regex better and compare to Perl!
	private static final String POSTFIX_ADJECTIVE_REGEX =
		"(" +
		"(?!major|lieutenant|brigadier|adjutant)\\S+(?=(?:-|\\s+)general)|" +
		"court(?=(?:-|\\s+)martial)" +
		")(.*)";

	private static final String[] PREPOSITIONS = {
		"about", "above", "across", "after", "among", "around", "at", "athwart", "before", "behind",
		"below", "beneath", "beside", "besides", "between", "betwixt", "beyond", "but", "by",
		"during", "except", "for", "from", "in", "into", "near", "of", "off", "on", "onto", "out", "over",
		"since", "till", "to", "under", "until", "unto", "upon", "with",
	};

	private static final Map<String, String> NOMINATIVE_PRONOUNS = toMap(new String[][]{
		// nominative			reflexive
		{ "i", "we" },			{ "myself",		"ourselves" },
		{ "you", "you" },		{ "yourself",	"yourselves" },
		{ "she", "they" },		{ "herself",	"themselves" },
		{ "he", "they" },		{ "himself",	"themselves" },
		{ "it", "they" },		{ "itself", 	"themselves" },
		{ "they", "they" },		{ "themself",	"themselves" },

		// possessive

		{ "mine", "ours" },
		{ "yours", "yours" },
		{ "hers", "theirs" },
		{ "his", "theirs" },
		{ "its", "theirs" },
		{ "theirs", "theirs" },
	});

	private static final Map<String, String> ACCUSATIVE_PRONOUNS = toMap(new String[][]{
		// accusative			reflexive
		{ "me",		"us" }, 	{ "myself",		"ourselves" },
		{ "you",	"you" },	{ "yourself",	"yourselves" },
		{ "her",	"them" },	{ "herself",	"themselves" },
		{ "him", 	"them" },	{ "himself",	"themselves" },
		{ "it",		"them" },	{ "itself",		"themselves" },
		{ "them",	"them" },	{ "themself",	"themselves" },
	});

	private static final Map<String, String> IRREGULAR_NOUNS = toMap(new String[][]{
		{ "child", "children" },
		{ "brother", "brothers" }, // irregular classical form
		{ "loaf", "loaves" },
		{ "hoof", "hoofs" }, // irregular classical form
		{ "beef", "beefs" }, // irregular classical form
		{ "money", "monies" },
		{ "mongoose", "mongooses" },
		{ "ox", "oxen" },
		{ "cow", "cows" }, // irregular classical form
		{ "soliloquy", "soliloquies" },
		{ "graffito", "graffiti" },
		{ "prima donna", "prima donnas" }, // irregular classical form
		{ "octopus", "octopuses" }, // irregular classical form
		{ "genie", "genies" }, // irregular classical form
		{ "ganglion", "ganglions" }, // irregular classical form
		{ "trilby", "trilbys" },
		{ "turf", "turfs" }, // irregular classical form
		{ "numen", "numina" },
		{ "atman", "atmas" },
		{ "occiput", "occiputs" }, // irregular classical form

		// Words ending in -s
		{ "corpus", "corpuses" }, // irregular classical form
		{ "opus", "opuses" }, // irregular classical form
		{ "genus", "genera" },
		{ "mythos", "mythoi" },
		{ "penis", "penises" }, // irregular classical form
		{ "testis", "testes" },
		{ "atlas", "atlases" }, // irregular classical form
	});

	private static final String[] CATEGORY_UNINFLECTED_NOUNS = {
		// Fish and herd animals
		".*fish", "tuna", "salmon", "mackerel", "trout",
		"bream", "sea[- ]bass", "carp", "cod", "flounder", "whiting",

		".*deer", ".*sheep",

		// Nationals ending in -ese
        "Portuguese", "Amoyese", "Borghese", "Congoese", "Faroese",
		"Foochowese", "Genevese", "Genoese", "Gilbertese", "Hottentotese",
		"Kiplingese", "Kongoese", "Lucchese", "Maltese", "Nankingese",
		"Niasese", "Pekingese", "Piedmontese", "Pistoiese", "Sarawakese",
		"Shavese", "Vermontese", "Wenchowese", "Yengeese",
		".*[nrlm]ese",

		// Diseases
		".*pox",

		// Other oddities
		"graffiti", "djinn",

		// Words ending in -s

		// Pairs or groups subsumed to a singular
	    "breeches", "britches", "clippers", "gallows", "hijinks",
		"headquarters", "pliers", "scissors", "testes", "herpes",
		"pincers", "shears", "proceedings", "trousers",

		// Unassimilated Latin 4th declension
		"cantus", "coitus", "nexus",

		// Recent imports
		"contretemps", "corps", "debris",
		".*ois", "siemens",

		// Diseases
		".*measles", "mumps",

		// Others
		"diabetes", "jackanapes", "series", "species", "rabies",
		"chassis", "innings", "news", "mews",
	};


	private static final String[] CATEGORY_MAN_MANS_RULE = {
		"human",
		"Alabaman", "Bahaman", "Burman", "German",
		"Hiroshiman", "Liman", "Nakayaman", "Oklahoman",
		"Panaman", "Selman", "Sonaman", "Tacoman", "Yakiman",
		"Yokohaman", "Yuman",
	};

	private static final String[] CATEGORY_EX_ICES_RULE = {
		"codex",	"murex",	"silex",
	};

	private static final String[] CATEGORY_IX_ICES_RULE = {
		"radix",	"helix",
	};

	private static final String[] CATEGORY_UM_A_RULE = {
		"bacterium",	"agendum",	"desideratum",	"erratum",
		"stratum",	"datum",	"ovum",		"extremum",
		"candelabrum",
	};

	private static final String[] CATEGORY_US_I_RULE = {
		"alumnus",	"alveolus",	"bacillus",	"bronchus",
		"locus",	"nucleus",	"stimulus",	"meniscus",
	};

	private static final String[] CATEGORY_ON_A_RULE = {
		"criterion",	"perihelion",	"aphelion",
		"phenomenon",	"prolegomenon",	"noumenon",
		"organon",	"asyndeton",	"hyperbaton",
	};

	private static final String[] CATEGORY_A_AE_RULE = {
		"alumna", "alga", "vertebra", "persona"
	};

	private static final String[] CATEGORY_O_OS_RULE = {
		"albino",	"archipelago",	"armadillo",
		"commando",	"crescendo",	"fiasco",
		"ditto",	"dynamo",	"embryo",
		"ghetto",	"guano",	"inferno",
		"jumbo",	"lumbago",	"magneto",
		"manifesto",	"medico",	"octavo",
		"photo",	"pro",		"quarto",
		"canto",	"lingo",	"generalissimo",
		"stylo",	"rhino",	"casino",
		"auto",     "macro",    "zero",

		"solo",		"soprano",	"basso",	"alto",
		"contralto",	"tempo",	"piano",	"virtuoso",

	};

	private static final String[] CATEGORY_SINGULAR_S_RULE = {
		".*ss",
        "acropolis", "aegis", "alias", "asbestos", "bathos", "bias",
        "bronchitis", "bursitis", "caddis", "cannabis",
        "canvas", "chaos", "cosmos", "dais", "digitalis",
        "epidermis", "ethos", "eyas", "gas", "glottis",
        "hubris", "ibis", "lens", "mantis", "marquis", "metropolis",
        "pathos", "pelvis", "polis", "rhinoceros",
        "sassafras", "trellis", ".*us", "[A-Z].*es",

    	"ephemeris", "iris", "clitoris",
    	"chrysalis", "epididymis",

    	// Inflammations
    	".*itis",
	};

	// References to Steps are to those in Conway's paper

	private final List<Rule> rules = Arrays.asList(new Rule[] {

		// Blank word
		new RegexReplacementRule("^(\\s)$", "$1"),

		// Nouns that do not inflect in the plural (such as "fish") [Step 2]
		new CategoryInflectionRule(CATEGORY_UNINFLECTED_NOUNS, "-", "-"),

		// Compounds [Step 12]
		new AbstractRegexReplacementRule("(?i)^(?:" + POSTFIX_ADJECTIVE_REGEX + ")$") {
			@Override public String replace(Matcher m) {
				return pluralize(m.group(1)) + m.group(2);
			}
		},

		new AbstractRegexReplacementRule(
			"(?i)(.*?)((?:-|\\s+)(?:" + disjunction(PREPOSITIONS) + "|d[eu])(?:-|\\s+))a(?:-|\\s+)(.*)") {

			@Override public String replace(Matcher m) {
				return pluralize(m.group(1)) +
					m.group(2) +
					pluralize(m.group(3));
			}
		},

		new AbstractRegexReplacementRule(
			"(?i)(.*?)((-|\\s+)(" + disjunction(PREPOSITIONS) + "|d[eu])((-|\\s+)(.*))?)") {

			@Override public String replace(Matcher m) {
				return pluralize(m.group(1)) + m.group(2);
			}
		},

		// Pronouns [Step 3]
		new IrregularMappingRule(NOMINATIVE_PRONOUNS, "(?i)" + disjunction(NOMINATIVE_PRONOUNS.keySet())),
		new IrregularMappingRule(ACCUSATIVE_PRONOUNS, "(?i)" + disjunction(ACCUSATIVE_PRONOUNS.keySet())),
		new IrregularMappingRule(ACCUSATIVE_PRONOUNS,
				"(?i)(" + disjunction(PREPOSITIONS) + "\\s)" +
				"(" + disjunction(ACCUSATIVE_PRONOUNS.keySet()) + ")") {
			@Override public String replace(Matcher m) {
				return m.group(1) + mappings.get(m.group(2).toLowerCase());
			}
		},

		// Standard irregular plurals (such as "children") [Step 4]
		new IrregularMappingRule(IRREGULAR_NOUNS, "(?i)(.*)\\b" + disjunction(IRREGULAR_NOUNS.keySet()) + "$"),
		new CategoryInflectionRule(CATEGORY_MAN_MANS_RULE, "-man", "-mans"),
		new RegexReplacementRule("(?i)(\\S*)(person)$", "$1people"),

		// Families of irregular plurals for common suffixes (such as "-men") [Step 5]
		new SuffixInflectionRule("-man", "-man", "-men"),
		new SuffixInflectionRule("-[lm]ouse", "-ouse", "-ice"),
		new SuffixInflectionRule("-tooth", "-tooth", "-teeth"),
		new SuffixInflectionRule("-goose", "-goose", "-geese"),
		new SuffixInflectionRule("-foot", "-foot", "-feet"),

		// Assimilated irregular plurals [Step 6]
		new SuffixInflectionRule("-ceps", "-", "-"),
		new SuffixInflectionRule("-zoon", "-zoon", "-zoa"),
		new SuffixInflectionRule("-[csx]is", "-is", "-es"),
		new CategoryInflectionRule(CATEGORY_EX_ICES_RULE, "-ex", "-ices"),
		new CategoryInflectionRule(CATEGORY_IX_ICES_RULE, "-ix", "-ices"),
		new CategoryInflectionRule(CATEGORY_UM_A_RULE, "-um", "-a"),
		new CategoryInflectionRule(CATEGORY_US_I_RULE, "-us", "-i"),
		new CategoryInflectionRule(CATEGORY_ON_A_RULE, "-on", "-a"),
		new CategoryInflectionRule(CATEGORY_A_AE_RULE, "-a", "-ae"),

		// Classical irregular plurals [Step 7]
		// Classical plurals have not been implemented

		// Nouns ending in sibilants (such as "churches") [Step 8]
		new CategoryInflectionRule(CATEGORY_SINGULAR_S_RULE, "-s", "-ses"),
		new RegexReplacementRule("^([A-Z].*s)$", "$1es"),
		new SuffixInflectionRule("-[cs]h", "-h", "-hes"),
		new SuffixInflectionRule("-x", "-x", "-xes"),
		new SuffixInflectionRule("-z", "-z", "-zes"),

		// Nouns ending with "-f" or "-fe" take "-ves" in the plural (such as "halves") [Step 9]
		new SuffixInflectionRule("-[aeo]lf", "-f", "-ves"),
		new SuffixInflectionRule("-[^d]eaf", "-f", "-ves"),
		new SuffixInflectionRule("-arf", "-f", "-ves"),
		new SuffixInflectionRule("-[nlw]ife", "-fe", "-ves"),

		// Nouns ending with "-y" [Step 10]
		new SuffixInflectionRule("-[aeiou]y", "-y", "-ys"),
		// NOTE: [RHQ] Comment out the below rule, because it will pluralize capitalized words ending in "y"
        //       (e.g. ConnectionFactory) by appending "s" (e.g. ConnectionFactorys) rather than "ies". (ips, 04/24/08)
        //new RegexReplacementRule("^([A-Z].*y)$", "$1s"),
		new SuffixInflectionRule("-y", "-y", "-ies"),

		// Nouns ending with "-o" [Step 11]
		new CategoryInflectionRule(CATEGORY_O_OS_RULE, "-o", "-os"),
		new SuffixInflectionRule("-[aeiou]o", "-o", "-os"),
		new SuffixInflectionRule("-o", "-o", "-oes"),

		// Default rule: add "s" [Step 13]
		new SuffixInflectionRule("-", "-s"),

	});

    public CustomEnglishPluralizer() {
        setRules(this.rules);
        setLocale(Locale.ENGLISH);
    }

    @Override
    protected String postProcess(String trimmedWord, String pluralizedWord) {
        // NOTE: [RHQ] Don't call super.postprocess(), since it will pluralize acronyms (e.g. CPU) as all caps (e.g.
        //       CPUS). (ips, 04/24/08)
        return pluralizedWord;
    }
}
