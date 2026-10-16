package com.example.shareinfo.sentiment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Java port of VADER (Valence Aware Dictionary and sEntiment Reasoner), a rule-based sentiment
 * model tuned for short social media text. Faithful to vaderSentiment 3.3.2:
 * https://github.com/cjhutto/vaderSentiment (MIT License, Copyright (c) 2016 C.J. Hutto).
 *
 * Unlike a plain word count, VADER accounts for negation ("not good"), intensifiers
 * ("very good"), dampeners ("kind of good"), ALL CAPS emphasis, exclamation marks and the
 * contrastive "but" ("good, but expensive"). This class is plain Java so it can be unit tested
 * on the JVM; {@link Lexicons} builds the word list it runs on.
 */
public class SentimentAnalyzer {
    // Empirically derived constants from the VADER paper.
    private static final double B_INCR = 0.293;
    private static final double B_DECR = -0.293;
    private static final double C_INCR = 0.733;
    private static final double N_SCALAR = -0.74;

    // Unicode whitespace, like Python's str.split(). Spelled out because Android's regex engine
    // rejects the (?U) flag that would do this on the JVM.
    private static final Pattern WHITESPACE =
            Pattern.compile("[\\s\\u0085\\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000]+");
    private static final String PUNCTUATION = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";

    private static final Set<String> NEGATE = new HashSet<>(Arrays.asList(
            "aint", "arent", "cannot", "cant", "couldnt", "darent", "didnt", "doesnt",
            "ain't", "aren't", "can't", "couldn't", "daren't", "didn't", "doesn't",
            "dont", "hadnt", "hasnt", "havent", "isnt", "mightnt", "mustnt", "neither",
            "don't", "hadn't", "hasn't", "haven't", "isn't", "mightn't", "mustn't",
            "neednt", "needn't", "never", "none", "nope", "nor", "not", "nothing", "nowhere",
            "oughtnt", "shant", "shouldnt", "uhuh", "wasnt", "werent",
            "oughtn't", "shan't", "shouldn't", "uh-uh", "wasn't", "weren't",
            "without", "wont", "wouldnt", "won't", "wouldn't", "rarely", "seldom", "despite"));

    private static final Map<String, Double> BOOSTER_DICT = new HashMap<>();
    private static final Map<String, Double> SPECIAL_CASES = new HashMap<>();

    static {
        for (String word : new String[]{"absolutely", "amazingly", "awfully", "completely",
                "considerable", "considerably", "decidedly", "deeply", "effing", "enormous",
                "enormously", "entirely", "especially", "exceptional", "exceptionally", "extreme",
                "extremely", "fabulously", "flipping", "flippin", "frackin", "fracking", "fricking",
                "frickin", "frigging", "friggin", "fully", "fuckin", "fucking", "fuggin", "fugging",
                "greatly", "hella", "highly", "hugely", "incredible", "incredibly", "intensely",
                "major", "majorly", "more", "most", "particularly", "purely", "quite", "really",
                "remarkably", "so", "substantially", "thoroughly", "total", "totally", "tremendous",
                "tremendously", "uber", "unbelievably", "unusually", "utter", "utterly", "very"}) {
            BOOSTER_DICT.put(word, B_INCR);
        }
        for (String word : new String[]{"almost", "barely", "hardly", "just enough", "kind of",
                "kinda", "kindof", "kind-of", "less", "little", "marginal", "marginally",
                "occasional", "occasionally", "partly", "scarce", "scarcely", "slight", "slightly",
                "somewhat", "sort of", "sorta", "sortof", "sort-of"}) {
            BOOSTER_DICT.put(word, B_DECR);
        }
        SPECIAL_CASES.put("the shit", 3.0);
        SPECIAL_CASES.put("the bomb", 3.0);
        SPECIAL_CASES.put("bad ass", 1.5);
        SPECIAL_CASES.put("badass", 1.5);
        SPECIAL_CASES.put("bus stop", 0.0);
        SPECIAL_CASES.put("yeah right", -2.0);
        SPECIAL_CASES.put("kiss of death", -1.5);
        SPECIAL_CASES.put("to die for", 3.0);
        SPECIAL_CASES.put("beating heart", 3.5);
    }

    private final Map<String, Double> lexicon;

    /** @param lexicon lower-case token to valence, on VADER's -4..+4 scale. */
    public SentimentAnalyzer(Map<String, Double> lexicon) {
        this.lexicon = lexicon;
    }

    public SentimentScore polarityScores(String text) {
        String[] words = tokenize(text);
        String[] lower = new String[words.length];
        for (int i = 0; i < words.length; i++) {
            lower[i] = words[i].toLowerCase(Locale.ROOT);
        }
        boolean isCapDiff = allCapDifferential(words);

        double[] sentiments = new double[words.length];
        for (int i = 0; i < words.length; i++) {
            // Boosters and "kind of" modify their neighbours rather than carrying sentiment.
            if (BOOSTER_DICT.containsKey(lower[i])) {
                continue;
            }
            if (i < words.length - 1 && lower[i].equals("kind") && lower[i + 1].equals("of")) {
                continue;
            }
            sentiments[i] = sentimentValence(words, lower, i, isCapDiff);
        }
        butCheck(lower, sentiments);
        return scoreValence(sentiments, text);
    }

    private double sentimentValence(String[] words, String[] lower, int i, boolean isCapDiff) {
        Double base = lexicon.get(lower[i]);
        if (base == null) {
            return 0;
        }
        double valence = base;

        // "no" next to a lexicon word negates that word instead of carrying its own valence.
        if (lower[i].equals("no") && i != words.length - 1 && lexicon.containsKey(lower[i + 1])) {
            valence = 0.0;
        }
        if ((i > 0 && lower[i - 1].equals("no"))
                || (i > 1 && lower[i - 2].equals("no"))
                || (i > 2 && lower[i - 3].equals("no") && (lower[i - 1].equals("or") || lower[i - 1].equals("nor")))) {
            valence = base * N_SCALAR;
        }

        if (isUpper(words[i]) && isCapDiff) {
            valence += valence > 0 ? C_INCR : -C_INCR;
        }

        for (int startI = 0; startI < 3; startI++) {
            // Dampen modifiers the further they are from the word they modify.
            if (i > startI && !lexicon.containsKey(lower[i - (startI + 1)])) {
                double s = scalarIncDec(words[i - (startI + 1)], valence, isCapDiff);
                if (startI == 1 && s != 0) {
                    s *= 0.95;
                }
                if (startI == 2 && s != 0) {
                    s *= 0.9;
                }
                valence += s;
                valence = negationCheck(valence, lower, startI, i);
                if (startI == 2) {
                    valence = specialIdiomsCheck(valence, lower, i);
                }
            }
        }
        return leastCheck(valence, lower, i);
    }

    private static double scalarIncDec(String word, double valence, boolean isCapDiff) {
        Double booster = BOOSTER_DICT.get(word.toLowerCase(Locale.ROOT));
        if (booster == null) {
            return 0;
        }
        double scalar = valence < 0 ? -booster : booster;
        if (isUpper(word) && isCapDiff) {
            scalar += valence > 0 ? C_INCR : -C_INCR;
        }
        return scalar;
    }

    private static double negationCheck(double valence, String[] lower, int startI, int i) {
        if (startI == 0) {
            if (negated(lower[i - 1])) {
                valence *= N_SCALAR;
            }
        } else if (startI == 1) {
            if (lower[i - 2].equals("never") && (lower[i - 1].equals("so") || lower[i - 1].equals("this"))) {
                valence *= 1.25;
            } else if (lower[i - 2].equals("without") && lower[i - 1].equals("doubt")) {
                return valence;
            } else if (negated(lower[i - 2])) {
                valence *= N_SCALAR;
            }
        } else {
            // Operator precedence mirrors the reference implementation exactly.
            if ((lower[i - 3].equals("never") && (lower[i - 2].equals("so") || lower[i - 2].equals("this")))
                    || (lower[i - 1].equals("so") || lower[i - 1].equals("this"))) {
                valence *= 1.25;
            } else if (lower[i - 3].equals("without") && (lower[i - 2].equals("doubt") || lower[i - 1].equals("doubt"))) {
                return valence;
            } else if (negated(lower[i - 3])) {
                valence *= N_SCALAR;
            }
        }
        return valence;
    }

    private static double specialIdiomsCheck(double valence, String[] lower, int i) {
        String oneZero = lower[i - 1] + " " + lower[i];
        String twoOneZero = lower[i - 2] + " " + lower[i - 1] + " " + lower[i];
        String twoOne = lower[i - 2] + " " + lower[i - 1];
        String threeTwoOne = lower[i - 3] + " " + lower[i - 2] + " " + lower[i - 1];
        String threeTwo = lower[i - 3] + " " + lower[i - 2];

        for (String sequence : new String[]{oneZero, twoOneZero, twoOne, threeTwoOne, threeTwo}) {
            Double special = SPECIAL_CASES.get(sequence);
            if (special != null) {
                valence = special;
                break;
            }
        }
        if (lower.length - 1 > i) {
            Double special = SPECIAL_CASES.get(lower[i] + " " + lower[i + 1]);
            if (special != null) {
                valence = special;
            }
        }
        if (lower.length - 1 > i + 1) {
            Double special = SPECIAL_CASES.get(lower[i] + " " + lower[i + 1] + " " + lower[i + 2]);
            if (special != null) {
                valence = special;
            }
        }
        // Booster bi-grams such as "sort of" or "kind of".
        for (String nGram : new String[]{threeTwoOne, threeTwo, twoOne}) {
            Double booster = BOOSTER_DICT.get(nGram);
            if (booster != null) {
                valence += booster;
            }
        }
        return valence;
    }

    private double leastCheck(double valence, String[] lower, int i) {
        if (i > 1 && !lexicon.containsKey(lower[i - 1]) && lower[i - 1].equals("least")) {
            if (!lower[i - 2].equals("at") && !lower[i - 2].equals("very")) {
                valence *= N_SCALAR;
            }
        } else if (i > 0 && !lexicon.containsKey(lower[i - 1]) && lower[i - 1].equals("least")) {
            valence *= N_SCALAR;
        }
        return valence;
    }

    /** Words after "but" carry more weight than the words before it. */
    private static void butCheck(String[] lower, double[] sentiments) {
        int butIndex = Arrays.asList(lower).indexOf("but");
        if (butIndex < 0) {
            return;
        }
        for (int i = 0; i < sentiments.length; i++) {
            if (i < butIndex) {
                sentiments[i] *= 0.5;
            } else if (i > butIndex) {
                sentiments[i] *= 1.5;
            }
        }
    }

    private static SentimentScore scoreValence(double[] sentiments, String text) {
        if (sentiments.length == 0) {
            return SentimentScore.NEUTRAL;
        }
        double sum = 0;
        for (double s : sentiments) {
            sum += s;
        }
        double punctuationAmplifier = punctuationEmphasis(text);
        if (sum > 0) {
            sum += punctuationAmplifier;
        } else if (sum < 0) {
            sum -= punctuationAmplifier;
        }
        double compound = normalize(sum);

        double posSum = 0;
        double negSum = 0;
        int neuCount = 0;
        for (double s : sentiments) {
            // +/-1 compensates for neutral words that are counted as 1.
            if (s > 0) {
                posSum += s + 1;
            } else if (s < 0) {
                negSum += s - 1;
            } else {
                neuCount++;
            }
        }
        if (posSum > Math.abs(negSum)) {
            posSum += punctuationAmplifier;
        } else if (posSum < Math.abs(negSum)) {
            negSum -= punctuationAmplifier;
        }
        double total = posSum + Math.abs(negSum) + neuCount;
        return new SentimentScore(compound, Math.abs(posSum / total), Math.abs(negSum / total),
                Math.abs(neuCount / total));
    }

    private static double punctuationEmphasis(String text) {
        int exclamations = Math.min(count(text, '!'), 4);
        int questions = count(text, '?');
        double questionAmplifier = 0;
        if (questions > 1) {
            questionAmplifier = questions <= 3 ? questions * 0.18 : 0.96;
        }
        return exclamations * 0.292 + questionAmplifier;
    }

    /** Maps an unbounded valence sum onto -1..1. */
    static double normalize(double score) {
        double norm = score / Math.sqrt(score * score + 15);
        return Math.max(-1.0, Math.min(1.0, norm));
    }

    static String[] tokenize(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return new String[0];
        }
        String[] raw = WHITESPACE.split(trimmed);
        List<String> tokens = new ArrayList<>(raw.length);
        for (String token : raw) {
            String stripped = stripPunctuation(token);
            // Short leftovers were probably emoticons such as ":)" - keep those intact.
            tokens.add(stripped.length() <= 2 ? token : stripped);
        }
        return tokens.toArray(new String[0]);
    }

    private static String stripPunctuation(String token) {
        int start = 0;
        int end = token.length();
        while (start < end && PUNCTUATION.indexOf(token.charAt(start)) >= 0) {
            start++;
        }
        while (end > start && PUNCTUATION.indexOf(token.charAt(end - 1)) >= 0) {
            end--;
        }
        return token.substring(start, end);
    }

    private static boolean negated(String lowerWord) {
        return NEGATE.contains(lowerWord) || lowerWord.contains("n't");
    }

    /** True when some, but not all, words are ALL CAPS. */
    private static boolean allCapDifferential(String[] words) {
        int allCaps = 0;
        for (String word : words) {
            if (isUpper(word)) {
                allCaps++;
            }
        }
        int differential = words.length - allCaps;
        return differential > 0 && differential < words.length;
    }

    /** Matches Python's str.isupper(): at least one cased character and no lower case ones. */
    private static boolean isUpper(String word) {
        boolean hasCased = false;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (Character.isLowerCase(c)) {
                return false;
            }
            if (Character.isUpperCase(c) || Character.isTitleCase(c)) {
                hasCased = true;
            }
        }
        return hasCased;
    }

    private static int count(String text, char target) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == target) {
                count++;
            }
        }
        return count;
    }
}
