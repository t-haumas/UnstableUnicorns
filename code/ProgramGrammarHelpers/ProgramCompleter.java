package code.ProgramGrammarHelpers;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import code.ProgramGrammarHelpers.Dependencies.ExpandableProgram;
import code.ProgramGrammarHelpers.Dependencies.Grammar;
import code.ProgramGrammarHelpers.Dependencies.GrammarReader;
import code.ProgramGrammarHelpers.Dependencies.SuggestionResultsList;
import code.ProgramGrammarHelpers.Dependencies.SuggestionResultsSet;
import code.ProgramGrammarHelpers.Dependencies.Symbol;

/**
 * A class to be instantiated that finds next suggestions for a program being
 * written (called a draft) given a grammar, which must be provided to the
 * Program Completer.
 */
public class ProgramCompleter {

    private Grammar grammar;
    private static final String COMPLETE_PROGRAM_SIGNIFIER = "~";

    /**
     * Standard constructor for a ProgramCompleter. 
     * @param programGrammar - The Grammar from which to build programs.
     */
    public ProgramCompleter(Grammar programGrammar) {
        this.grammar = programGrammar;
    }

    /**
     * Given a draft, returns the list of possible strings that could be appended to
     * the program such that the program is still a valid program.
     * 
     * @param draft - The draft program.
     * @return A SuggestionResultsList of the next possible Strings for the draft.
     *         SuggestionResultsList also contains info about whether the draft is a
     *         complete program or not.
     */
    public SuggestionResultsList getPossibilities(String draft) {
        SuggestionResultsList possibilities = new SuggestionResultsList();

        /*  There could be many different preferences on how to determine what
            suggestions are given. I'm using dependency injection (I think) here to
            redirect this function to another one that contains the real functionality so
            behavior could be changed later if desired. */
        possibilities = getPossibilitiesWithLengthenedSingles(draft);

        possibilities.sort();
        return possibilities;
    }

    /**
     * Given a draft, returns next possibilities for the program. If any of the
     * possibilities only have one child next possibility, the child is
     * automatically appended to the parent possibility. This is done to exhaustion
     * so there will never be two lists of only one possibility in a row.
     * 
     * @param draft - The draft program being written.
     * @return A list of the next possibilities for the program, with each
     *         possibility being as long as possible.
     */
    private SuggestionResultsList getPossibilitiesWithLengthenedSingles(String draft) {

        //  First, just get the raw next possibilities for the draft.
        SuggestionResultsSet possibilitiesSet = getNextPossibilities(draft);

        //  Set up what will be returned.
        SuggestionResultsList possibilities = new SuggestionResultsList(possibilitiesSet);

        for (int i = 0; i < possibilities.size(); i++) {
            String possibility = possibilities.get(i);
            
            //  For each possibility, see how many child possibilities it has.
            int numNextPossibilities = getNextPossibilities(draft + possibility).size();
            if (numNextPossibilities == 1) {
                //  If there is exactly one child possibility, lengthen that child possibility as much as possible.
                possibilities.set(i, possibility + getPossibilitiesLengthenedIfSingle(draft + possibility).get(0));
            }
        }
        return possibilities;
    }

    /**
     * If the draft has only one next possibility, this function lengthens that
     * possibility as much as possible. Otherwise, returns the raw list of next
     * possbilities for the draft.
     * 
     * @param draft - The draft program being written.
     * @return a list of possibilities for the draft. If only one possibility is
     *         returned, that possibility is guaranteed to be as long as possible.
     */
    private SuggestionResultsList getPossibilitiesLengthenedIfSingle(String draft) {
        SuggestionResultsList possibilities;
        SuggestionResultsSet possibilitiesSet = getNextPossibilities(draft);
        if (possibilitiesSet.size() == 1) {
            String nextPossibility = "";
            while (possibilitiesSet.size() == 1) {
                nextPossibility += possibilitiesSet.getElementIfOnlyElement();
                possibilitiesSet = getNextPossibilities(draft + nextPossibility);
            }
            possibilities = new SuggestionResultsList();
            possibilities.add(nextPossibility);
            possibilities.setComplete(possibilitiesSet.isComplete());
        } else {
            possibilities = new SuggestionResultsList(possibilitiesSet);
            possibilities.sort();
        }
        return possibilities;
    }

    /**
     * Returns a List of possibilites for the next String in the draft. However, the
     * list will try not to have a length of 1. This function will continue down the
     * tree of possibilities until there is more than one option.
     * This method would have to be refactored a bit to be used by the
     * ProgramCompleter.
     * Use this if you want it to give you look-ahead predictions when there's only
     * one option. AKA using this method will prevent the predictions from ever
     * providing only
     * one option.
     * I'm not sure if this will be useful but I made it nonetheless, maybe it will
     * be used in the future.
     * 
     * @param draft       - The draft program being written.
     * @param extraString - I think this is used internally for recursion to build
     *                    up the possibilities that will be returned. I believe the
     *                    correct functionality is to pass in the empty string in
     *                    the initial call.
     * @return a list of possibilities for the draft. This function tries to make
     *         that list have more than 1 element. It extends possibilities until
     *         there are multiple options.
     */
    @SuppressWarnings(value = { "unused" })
    private List<String> getManyPossibilities(String draft, String extraString) {

        //  First, just get the normal possibilities set.
        SuggestionResultsSet possibilitiesResults = getNextPossibilities(draft);
        Set<String> possibilities = possibilitiesResults.getStringSet();

        if (possibilities.size() == 1) {
            //  If there's only one possibility, get it and store it in the variable littleString.
            String littleString = possibilities.iterator().next();

            if (littleString.length() != 0) {
                //  The next draft to build off of is the current draft + littleString.
                String nextString = draft + littleString;

                //  Get many possibilities for the next draft.
                return getManyPossibilities(nextString, extraString + littleString);
            }
        }

        //  If we get to this point in the function, then there was more than one possibility.
        List<String> result = new ArrayList<>();
        result.addAll(possibilities);
        for (int i = 0; i < result.size(); i++) {

            // If this is the intial call, extraString will be empty and the following will
            // not change anything.
            // If this is a recursive call, extraString is how we stored everything else
            // that was added to the draft before the current possibilities were generated.
            // So, we must add extraString as a prefix to all the possibilities that have
            // been generated.
            result.set(i, extraString + result.get(i));
            if (result.get(i).length() == 0) {
                if (result.size() == 1) {
                    result.set(i, COMPLETE_PROGRAM_SIGNIFIER);
                    // This is not the best way to signify a complete program, because a program could be complete or also possibly incomplete, if the grammar has that ambiguity.
                    // But so far, I'm just using a grammar without ambiguity by requiring putting a fullstop at the end of a program.
                    // So that probably works. But the signifier also appears if some errors occur in the grammar, like an undefined non-terminable.
                } else {
                    result.remove(i);
                }
            }
        }
        return result;
    }

    /**
     * Gets a Set of possibilities of next strings for a given draft. This function
     * gets the standard set of next possibilities. Nothing fancy, just what is
     * everything that could come next.
     * 
     * @param draft - The current draft program.
     * @return A SuggestionResultsSet, which is a set containing all the possible
     *         next Strings for the draft. Also contains info about whether or not
     *         the draft is a complete program.
     */
    private SuggestionResultsSet getNextPossibilities(String draft) {
        SuggestionResultsSet possibilities = new SuggestionResultsSet();

        //  Create a seed program to expand to search for a valid program that starts with the draft.
        ArrayList<Symbol> possibleProgram = new ArrayList<>();
        possibleProgram.add(grammar.getNewGoalSymbol());
        ExpandableProgram currentProgram = new ExpandableProgram(possibleProgram, grammar);

        // Search for all valid programs that start with the draft, and store the next
        // possibilities in the SuggestionResultsSet.
        expandAndCheck(draft, currentProgram, possibilities);
        return possibilities;
    }

    /**
     * Used to find next possibilities for a given draft program.
     * The caller must provide the draft, a seed program to expand, and a reference
     * to a SuggestionResultsSet to store the results.
     * The method is designed this way because it is called recursively.
     * 
     * The seed program used in the initial call (passed in as the currentProgram
     * formal parameter) should be an ExpandableProgram that is just a program
     * comprised of the goal node of a grammar (non-terminal).
     * 
     * When the initial call returns, possibilities will contain the next
     * possibilities for the draft.
     * 
     * @param draft          - The draft program being written.
     * @param currentProgram - The current ExpandableProgram that is being expanded
     *                       to search for possible programs given the grammar that
     *                       match the draft.
     * @param possibilities  - A reference to a SuggestionResultsSet where the
     *                       results (next possibilities) will be stored.
     */
    private void expandAndCheck(String draft, ExpandableProgram currentProgram, SuggestionResultsSet possibilities) {
        //String originalDraftString = draft;   // Might need to pass this in to the recursive call, idk.
        boolean draftEndsWithWhitespace;
        if (draft.length() == 0) {
            draftEndsWithWhitespace = false;
        } else {
            draftEndsWithWhitespace = grammar.isWhitespace(draft.charAt(draft.length() - 1));
        }
        List<ExpandableProgram> expansions = currentProgram.getNextExpansions();
        if (expansions.size() == 0) {
            possibilities.setComplete(true);
            return;
        }
        for (int x = 0; x < expansions.size(); x++) {
            ExpandableProgram nextProgram = expansions.get(x);
            List<String> draftTokens = grammar.tokenize(draft);
            List<String> nextProgramTokens = nextProgram.getText(draftTokens.size());
            boolean possiblyNeedToContinueToExpand;
            if (draftTokens.size() == 0) {
                possiblyNeedToContinueToExpand = nextProgramTokens.size() == 0;
            } else {
                possiblyNeedToContinueToExpand = nextProgramTokens.size() < draftTokens.size() || nextProgramTokens.size() == draftTokens.size() && draftTokens.get(draftTokens.size() - 1).equals(nextProgramTokens.get(draftTokens.size() - 1));
            }
            
            if (possiblyNeedToContinueToExpand) {
                if (startsWith(draftTokens, nextProgramTokens)) {
                    expandAndCheck(draft, nextProgram, possibilities);
                }
            } else {
                if (startsWith(nextProgramTokens, draftTokens)) {
                    //  This nextProgram is valid.
                    if (nextProgramTokens.size() == draftTokens.size()) {
                        /* The draftProgram's last token is not complete.
                            * The nextProgram completes it.                     */
                        if (! draftEndsWithWhitespace) {
                            //  In the draft, the incomplete token is not followed by whitespace. This is good, because otherwise the whitespace would signify that the token is complete.
                            possibilities.add(nextProgramTokens.get(draftTokens.size() - 1).substring(draftTokens.get(draftTokens.size() - 1).length()));
                        }
                    } else {
                        //  Maybe completed the current token, but definitely also added another whole one.

                        //  Special case
                        if (draftTokens.size() == 0) {
                            while (nextProgramTokens.size() > 1) {
                                nextProgramTokens.remove(nextProgramTokens.size() - 1);
                            }
                            possibilities.add(nextProgramTokens.get(0));
                            continue;
                        }
                        if (nextProgramTokens.get(draftTokens.size() - 1).equals(draftTokens.get(draftTokens.size() - 1))) {
                            // Did not complete the current token. Only added another whole one.
                            while (nextProgramTokens.size() > draftTokens.size() + 1) {
                                nextProgramTokens.remove(nextProgramTokens.size() - 1);
                            }

                            if (draftEndsWithWhitespace) {
                                // Do not add a space before the recommendation.
                                possibilities.add(nextProgramTokens.get(nextProgramTokens.size() - 1));
                            } else {
                                if (requireSpaceToSeparate(nextProgramTokens.get(nextProgramTokens.size() - 2), nextProgramTokens.get(nextProgramTokens.size() - 1))) {
                                    // add recommendation with space only.
                                    possibilities.add(" " + nextProgramTokens.get(nextProgramTokens.size() - 1));
                                } else {
                                    // add recommendation with space and without.
                                    possibilities.add(" " + nextProgramTokens.get(nextProgramTokens.size() - 1));
                                    possibilities.add(nextProgramTokens.get(nextProgramTokens.size() - 1));
                                }
                            }
                        } else {
                            // Completed the current token, and added another.
                            if (!draftEndsWithWhitespace) {
                                // The draft doesn't end with whitespace, which is good, because that would mean
                                // the last token is supposedly complete.

                                //TODO: What is going on here? Break it down.
                                possibilities.add(nextProgramTokens.get(draftTokens.size() - 1).substring(draftTokens.get(draftTokens.size() - 1).length()));
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean requireSpaceToSeparate(String s1, String s2) {
        return isAlphanumeric(s1.charAt(s1.length() - 1)) && isAlphanumeric(s2.charAt(0));
    }

    private boolean isAlphanumeric(char c) {
        return 'a' <= c && c <= 'z' || 'A' <= c && c <= 'Z' || '0' <= c && c <= '9';
    }

    /**
     * Tests if the first list starts with the second list.
     * 
     * @param longerList
     * @param prefixList
     * @return true or false.
     */
    private boolean startsWith(List<String> longerList, List<String> prefixList) {

        // System.out.println("ab".startsWith("a"));        TRUE
        // System.out.println("ab".startsWith("ab"));       TRUE
        // System.out.println("ab".startsWith("abc"));      FALSE       
        // System.out.println("ab".startsWith(""));         TRUE
        // System.out.println("".startsWith(""));           TRUE
        // System.out.println("".startsWith("ab"));         FALSE

        // size(1) must be >= size(2).

        if (longerList.size() < prefixList.size()) return false;

        if (prefixList.size() == 0) return true;

        for (int i = 0; i < prefixList.size() - 1; i++) {
            if (! longerList.get(i).equals(prefixList.get(i))) return false;
        }

        if (! longerList.get(prefixList.size() - 1).startsWith(prefixList.get(prefixList.size() - 1))) return false;

        return true;
    }

    public static void main(String[] args) {
        
        GrammarReader grammarReader = new GrammarReader(args[0]);
        Grammar myGrammar = grammarReader.getGrammar();
        ProgramCompleter cardBuilder = new ProgramCompleter(myGrammar);

        ProgramCompleterGUI gui = new ProgramCompleterGUI(cardBuilder);

        gui.start();

        //  A command-line method to interact with the ProgramCompleter.


        // if (false) {
        //     String currentString = "";
        //     Scanner inputGetter = new Scanner(System.in);
        //     while (!currentString.equals("exit")) {
        //         List<String> nextPossibilities = cardBuilder.getPossibilities(currentString).getStringList();
        //         for (String s : nextPossibilities) {
        //             System.out.println(s.replaceAll(" ", "-"));
        //         }
        //         System.out.print("-> ");
        //         currentString = inputGetter.nextLine();
        //         System.err.println(myGrammar.tokenize(currentString));
        //         System.err.println("|" + currentString + "|");
        //     }
        //     inputGetter.close();
        // }
    }
}
