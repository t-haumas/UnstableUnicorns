package code.Parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class ProgramBuilder {  //TODO: refactor this to ProgramBuilder.

    private Grammar grammar;
    private static final String COMPLETE_PROGRAM_SIGNIFIER = "~";

    public ProgramBuilder(Grammar cardGrammar) {
        this.grammar = cardGrammar;
    }

    public List<String> getPossibilities(String buildingString) {   //TODO:  At borders, create option to include whitespace, or not. Could do that by detecting borders in the result list! within the result and on the border between buildingString and result.
        List<String> possibilities = new ArrayList<>();
        possibilities = getPossibilitiesWithLengthenedSingles(buildingString);

        // if (buildingString.length() > 0) {
        //     int size = possibilities.size();
        //     for (int i = 0; i < size; i++) {
        //         if (grammar.atBorder(buildingString.charAt(buildingString.length() - 1), possibilities.get(i).charAt(0))) {
        //             possibilities.add(" " + possibilities.get(i));
        //         }
        //     }
        // }

        // if (buildingString.length() > 0) {
        //     if (grammar.isWhitespace(buildingString.charAt(buildingString.length() - 1))) {
        //         for (int i = 0; i < possibilities.size(); i++) {
        //             if (possibilities.get(i).charAt(0) == ' ') {
        //                 possibilities.set(i, possibilities.get(i).substring(1));
        //                 System.out.println("wow");
        //             }
        //         }
        //     }
        // }

        Collections.sort(possibilities);

        return possibilities;
    }

    public List<String> getPossibilitiesWithLengthenedSingles(String buildingString) {
        List<String> possibilities = new ArrayList<>();
        Set<String> possibilitiesSet = getNextPossibilities(buildingString);
        possibilities.addAll(possibilitiesSet);
        for (int i = 0; i < possibilities.size(); i++) {
            String possibility = possibilities.get(i);
            int numNextPossibilities = getNextPossibilities(buildingString + possibility).size();
            if (numNextPossibilities == 1) {
                possibilities.set(i, possibility + getPossibilitiesLengthenedIfSingle(buildingString + possibility).get(0));
            }
        }
        return possibilities;
    }

    public List<String> getPossibilitiesLengthenedIfSingle(String buildingString) {
        List<String> possibilities = new ArrayList<>();
        Set<String> possibilitiesSet = getNextPossibilities(buildingString);
        if (possibilitiesSet.size() == 1) {
            String nextPossibility = "";
            while (possibilitiesSet.size() == 1) {
                nextPossibility += possibilitiesSet.iterator().next();
                possibilitiesSet = getNextPossibilities(buildingString + nextPossibility);
            }
            possibilities.add(nextPossibility);
        } else {
            possibilities.addAll(possibilitiesSet);
            Collections.sort(possibilities);
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
    public List<String> getManyPossibilities(String buildingString, String extraString) {
        Set<String> possibilities = getNextPossibilities(buildingString);
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

    private Set<String> getNextPossibilities(String buildingString) {
        Set<String> possibilities = new HashSet<>();
        ArrayList<Symbol> possibleProgram = new ArrayList<>();
        possibleProgram.add(grammar.getNewGoalSymbol());
        ExpandableProgram currentProgram = new ExpandableProgram(possibleProgram, grammar);
        expandAndCheck(buildingString, currentProgram, possibilities);
        return possibilities;
    }

    private void expandAndCheck(String buildingString, ExpandableProgram program, Set<String> possibilities) {
        String originalBuildingString = buildingString;
        boolean buildingStringEndsWithWhitespace;
        if (buildingString.length() == 0) {
            buildingStringEndsWithWhitespace = false;
        } else {
            buildingStringEndsWithWhitespace = grammar.isWhitespace(buildingString.charAt(buildingString.length() - 1));
        }
        if (program.getNumberOfUnexpandedSymbols() > 0) {
            List<ExpandableProgram> expansions = program.getNextExpansions();
            for (int x = 0; x < expansions.size(); x++) {
                ExpandableProgram nextProgram = expansions.get(x);
                List<Symbol> buildingStringTokenized = grammar.tokenize(buildingString);
                List<Symbol> nextProgramStringTokenized = nextProgram.getText(buildingStringTokenized.size());
                String nextProgramString = join(nextProgramStringTokenized, " ");
                buildingString = join(buildingStringTokenized, " ");
                if (nextProgramString.length() <= buildingString.length()) {
                    if (buildingString.startsWith(nextProgramString)) {
                        expandAndCheck(originalBuildingString, nextProgram, possibilities);
                    }
                } 
                if (nextProgramString.length() > buildingString.length()) {
                    if (nextProgramString.startsWith(buildingString)) {
                        if (nextProgramStringTokenized.size() == buildingStringTokenized.size()) {
                            //  Only completed the current token.
                            if (! buildingStringEndsWithWhitespace) {
                                possibilities.add(nextProgramString.substring(buildingString.length()));
                            }
                        } else {
                            //  Maybe completed the current token, but definitely also added another whole one.

                            //  Special case
                            if (buildingStringTokenized.size() == 0) {
                                while (nextProgramStringTokenized.size() > 1) {
                                    nextProgramStringTokenized.remove(nextProgramStringTokenized.size() - 1);
                                }
                                possibilities.add(join(nextProgramStringTokenized, " "));
                                continue;
                            }
                            try {
                            if (nextProgramStringTokenized.get(buildingStringTokenized.size() - 1).equals(buildingStringTokenized.get(buildingStringTokenized.size() - 1))) {
                                // Did not complete the current token. Only added another whole one.
                                while (nextProgramStringTokenized.size() > buildingStringTokenized.size() + 1) {
                                    nextProgramStringTokenized.remove(nextProgramStringTokenized.size() - 1);
                                }
                                possibilities.add(join(nextProgramStringTokenized, " ").substring(buildingString.length()));
                            } else {
                                // Completed the current token.
                                if (! buildingStringEndsWithWhitespace) {
                                    while (nextProgramStringTokenized.size() > buildingStringTokenized.size()) {
                                        nextProgramStringTokenized.remove(nextProgramStringTokenized.size() - 1);
                                    }
                                    possibilities.add(join(nextProgramStringTokenized, " ").substring(buildingString.length()));
                                }
                            }
                        } catch (Exception e) {
                            System.out.println(nextProgramStringTokenized);
                            System.out.println(buildingStringTokenized);
                            System.out.println("---");
                            return;
                        }
                            
                        }
                    }
                }
            }
        }
    }

    private <T> String join(List<T> list, String delimiter) {
        
        if (list.size() == 0) return "";

        StringBuilder joined = new StringBuilder();

        for (int i = 0; i < list.size() - 1; i++) {
            String current = list.get(i).toString();
            String next = list.get(i + 1).toString();
            joined.append(current);
            if (! grammar.atBorder(current.charAt(current.length() - 1), next.charAt(0))) {
                joined.append(delimiter);
            }
        }

        joined.append(list.get(list.size() - 1).toString());

        //  "a b "
        //  2, 3

        //  joined.delete(joined.length() - delimiter.length(), joined.length());


        return joined.toString();
    }

    private <T> boolean startsWith(List<T> list1, List<T> list2) {

        // System.out.println("ab".startsWith("a"));       TRUE
        // System.out.println("ab".startsWith("ab"));      TRUE
        // System.out.println("ab".startsWith("abc"));     FALSE

        // size(1) must be >= size(2).

        if (list1.size() < list2.size()) return false;

        for (int i = 0; i < list2.size(); i++) {
            if (! list1.get(i).equals(list2.get(i))) return false;
        }

        return true;
    }

    public static void main(String[] args) {
        GrammarReader grammarReader = new GrammarReader(args[0]);   //TODO: detect grammar files automatically and asks the user which one they want to use.
        Grammar myGrammar = grammarReader.getGrammar();

        ProgramBuilder cardBuilder = new ProgramBuilder(myGrammar);

        String currentString = "";
        Scanner inputGetter = new Scanner(System.in);

        while (! currentString.equals("exit")) {
            List<String> nextPossibilities = cardBuilder.getPossibilities(currentString);
            for (String s : nextPossibilities) {
                System.out.println(s.replaceAll(" ", "-"));
            }
            System.out.print("-> ");
            currentString = inputGetter.nextLine();
            System.err.println(myGrammar.tokenize(currentString));
            System.err.println("|"+currentString + "|");

        }

        inputGetter.close();
    }
// Long-term TODO for unicorns
//TODO: implement card functionality 
//TODO: card database explorer. Called program database. Make a way for every program to have an auto-generated name which is the same as its auto-generated number ID, which comes from what decisions were made in its creation. Could have some mod() operator to condense the name. Or use base 64 encoding, idk.
//TODO: remove extraneous areas of the card grammar.
//TODO: once the card grammar is done, make an event grammar. Maybe start making the game first.
//TODO: optional: make a program that reads and re-formats grammar files? Makes some way for you to clean it up / work with it?
}
