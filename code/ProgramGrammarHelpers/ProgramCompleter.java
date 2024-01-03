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
     * Use this if you want it to give you look-ahead predictions when there's only one option.
     * AKA using this method will prevent the predictions from ever providing only one option.
     * 
     * @param buildingString
     * @param extraString
     * @return
     */
    private List<String> getManyPossibilities(String buildingString, String extraString) {
        SuggestionResultsSet possibilitiesResults = getNextPossibilities(buildingString);
        Set<String> possibilities = possibilitiesResults.getStringSet();
        if (possibilities.size() == 1) {
            String littleString = possibilities.iterator().next();
            String nextString = buildingString + littleString;
            if (littleString.length() != 0) {
                return getManyPossibilities(nextString, extraString + littleString);
            }
        }
        List<String> result = new ArrayList<>();
        result.addAll(possibilities);
        for (int i = 0; i < result.size(); i++) {
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

    private SuggestionResultsSet getNextPossibilities(String buildingString) {
        SuggestionResultsSet possibilities = new SuggestionResultsSet();
        ArrayList<Symbol> possibleProgram = new ArrayList<>();
        possibleProgram.add(grammar.getNewGoalSymbol());
        ExpandableProgram currentProgram = new ExpandableProgram(possibleProgram, grammar);
        expandAndCheck(buildingString, currentProgram, possibilities);
        return possibilities;
    }

    private void expandAndCheck(String draftProgramString, ExpandableProgram program, SuggestionResultsSet possibilities) {
        // TODO: I could refactor this method body to use Lists of Strings instead of Lists of Symbols.
        String originalDraftString = draftProgramString;
        boolean draftEndsWithWhitespace;
        if (draftProgramString.length() == 0) {
            draftEndsWithWhitespace = false;
        } else {
            draftEndsWithWhitespace = grammar.isWhitespace(draftProgramString.charAt(draftProgramString.length() - 1));
        }
        List<ExpandableProgram> expansions = program.getNextExpansions();
        if (expansions.size() == 0) {
            possibilities.setComplete(true);
            return;
        }
        for (int x = 0; x < expansions.size(); x++) {
            ExpandableProgram nextProgram = expansions.get(x);
            List<Symbol> draftTokens = grammar.tokenize(draftProgramString);
            List<Symbol> nextProgramTokens = nextProgram.getText(draftTokens.size());
            boolean possiblyNeedToContinueToExpand;
            if (draftTokens.size() == 0) {
                possiblyNeedToContinueToExpand = nextProgramTokens.size() == 0;
            } else {
                possiblyNeedToContinueToExpand = nextProgramTokens.size() < draftTokens.size() || nextProgramTokens.size() == draftTokens.size() && draftTokens.get(draftTokens.size() - 1).equals(nextProgramTokens.get(draftTokens.size() - 1));
            }
            
            if (possiblyNeedToContinueToExpand) {
                if (startsWith(draftTokens, nextProgramTokens)) {
                    expandAndCheck(originalDraftString, nextProgram, possibilities);
                }
            } else {
                if (startsWith(nextProgramTokens, draftTokens)) {
                    //  This nextProgram is valid.
                    if (nextProgramTokens.size() == draftTokens.size()) {
                        /* The draftProgram's last token is not complete.
                            * The nextProgram completes it.                     */
                        if (! draftEndsWithWhitespace) {
                            //  In the draft, the incomplete token is not followed by whitespace. This is good, because otherwise the whitespace would signify that the token is complete.
                            possibilities.add(nextProgramTokens.get(draftTokens.size() - 1).getValue().substring(draftTokens.get(draftTokens.size() - 1).getValue().length()));
                        }
                    } else {
                        //  Maybe completed the current token, but definitely also added another whole one.

                        //  Special case
                        if (draftTokens.size() == 0) {
                            while (nextProgramTokens.size() > 1) {
                                nextProgramTokens.remove(nextProgramTokens.size() - 1);
                            }
                            //possibilities.add(join(nextProgramTokens, " "));    //TODO: is this join necessary?
                            possibilities.add(nextProgramTokens.get(0).getValue());
                            continue;
                        }
                        if (nextProgramTokens.get(draftTokens.size() - 1).equals(draftTokens.get(draftTokens.size() - 1))) {
                            // Did not complete the current token. Only added another whole one.
                            while (nextProgramTokens.size() > draftTokens.size() + 1) {
                                nextProgramTokens.remove(nextProgramTokens.size() - 1);
                            }

                            if (draftEndsWithWhitespace) {
                                // Do not add a space before the recommendation.
                                possibilities.add(nextProgramTokens.get(nextProgramTokens.size() - 1).getValue());
                            } else {
                                if (requireSpaceToSeparate(nextProgramTokens.get(nextProgramTokens.size() - 2), nextProgramTokens.get(nextProgramTokens.size() - 1))) {
                                    // add recommendation with space only.
                                    possibilities.add(" " + nextProgramTokens.get(nextProgramTokens.size() - 1).getValue());
                                } else {
                                    // add recommendation with space and without.
                                    possibilities.add(" " + nextProgramTokens.get(nextProgramTokens.size() - 1).getValue());
                                    possibilities.add(nextProgramTokens.get(nextProgramTokens.size() - 1).getValue());
                                }
                            }
                        } else {
                            // Completed the current token, and added another.
                            if (!draftEndsWithWhitespace) {
                                // The draft doesn't end with whitespace, which is good, because that would mean
                                // the last token is supposedly complete.
                                        // while (nextProgramTokens.size() > draftTokens.size()) {
                                        //       // Get rid of the added tokens so we only have the one that needs to be
                                        //      // completed.
                                        //        // TODO: skip this step, just ignore it. Act as if the sizes of the lists are
                                        //        // the same.
                                        //     nextProgramTokens.remove(nextProgramTokens.size() - 1);
                                        // }
                                possibilities.add(nextProgramTokens.get(draftTokens.size() - 1).getValue().substring(draftTokens.get(draftTokens.size() - 1).getValue().length()));
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean requireSpaceToSeparate(Symbol s1, Symbol s2) {
        return isAlphanumeric(s1.getLastChar()) && isAlphanumeric(s2.getFirstChar());
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
    private boolean startsWith(List<Symbol> longerList, List<Symbol> prefixList) {

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
        
        GrammarReader grammarReader = new GrammarReader(args[0]);   //TODO: detect grammar files automatically and asks the user which one they want to use.
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
