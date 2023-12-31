package code.ProgramGrammarHelpers;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.RenderingHints.Key;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;

import code.ProgramGrammarHelpers.Dependencies.ExpandableProgram;
import code.ProgramGrammarHelpers.Dependencies.Grammar;
import code.ProgramGrammarHelpers.Dependencies.Symbol;

public class ProgramCompleter {

    private Grammar grammar;
    private static final String COMPLETE_PROGRAM_SIGNIFIER = "~";

    public ProgramCompleter(Grammar cardGrammar) {
        this.grammar = cardGrammar;
    }

    public List<String> getPossibilities(String buildingString) {
        List<String> possibilities = new ArrayList<>();
        possibilities = getPossibilitiesWithLengthenedSingles(buildingString);

        Collections.sort(possibilities);

        return possibilities;
    }

    private List<String> getPossibilitiesWithLengthenedSingles(String buildingString) {
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

    private List<String> getPossibilitiesLengthenedIfSingle(String buildingString) {
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
    private List<String> getManyPossibilities(String buildingString, String extraString) {
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

    private void expandAndCheck(String draftProgramString, ExpandableProgram program, Set<String> possibilities) {
        // TODO: I could refactor this method body to use Lists of Strings instead of Lists of Symbols.
        String originalDraftString = draftProgramString;
        boolean draftEndsWithWhitespace;
        if (draftProgramString.length() == 0) {
            draftEndsWithWhitespace = false;
        } else {
            draftEndsWithWhitespace = grammar.isWhitespace(draftProgramString.charAt(draftProgramString.length() - 1));
        }
        if (program.getNumberOfUnexpandedSymbols() > 0) {
            List<ExpandableProgram> expansions = program.getNextExpansions();
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
                } 
                else {
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
    }

    private boolean requireSpaceToSeparate(Symbol s1, Symbol s2) {
        return isAlphanumeric(s1.getLastChar()) && isAlphanumeric(s2.getFirstChar());
    }

    private boolean isAlphanumeric(char c) {
        return 'a' <= c && c <= 'z' || 'A' <= c && c <= 'Z' || '0' <= c && c <= '9';
    }

    /*
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
    } */

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
        
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        GrammarReader grammarReader = new GrammarReader(args[0]);   //TODO: detect grammar files automatically and asks the user which one they want to use.
        Grammar myGrammar = grammarReader.getGrammar();
        ProgramCompleter cardBuilder = new ProgramCompleter(myGrammar);

        
        JFrame window = new JFrame("Program Completer");
        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        JTextArea programDraftArea = new JTextArea();
        programDraftArea.setHighlighter(null);

        JPopupMenu suggestionMenu = new JPopupMenu();

        InputMap draftAreaInputMap = programDraftArea.getInputMap();
        KeyStroke cmdDelKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        draftAreaInputMap.put(cmdDelKeyStroke, "Cmd+Del");
        ActionMap draftAreaActionMap = programDraftArea.getActionMap();

        draftAreaInputMap.put(KeyStroke.getKeyStroke("LEFT"), "none");
        draftAreaInputMap.put(KeyStroke.getKeyStroke("UP"), "none");

        draftAreaActionMap.put("Cmd+Del", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                int caretOffset = programDraftArea.getCaretPosition();
                int lineNumber = 0;
                try {
                    lineNumber = programDraftArea.getLineOfOffset(caretOffset);
                    int startOffset = programDraftArea.getLineStartOffset(lineNumber);
                    int endOffset = programDraftArea.getLineEndOffset(lineNumber);
                    programDraftArea.replaceRange("", startOffset, endOffset);
                } catch (BadLocationException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        programDraftArea.addCaretListener(new CaretListener() {
            public void caretUpdate(CaretEvent ce) {
                if (ce.getDot() != programDraftArea.getDocument().getLength()) {
                    programDraftArea.setCaretPosition(programDraftArea.getDocument().getLength());
                }
            }
        });
            
        programDraftArea.addKeyListener(new KeyListener() {

            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyPressed(e);
            }

            public void handleKeyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    System.out.println("Left");
                    //programDraftArea.setCaretPosition(programDraftArea.getDocument().getLength());
                } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    // Right arrow key pressed
                    System.out.println("Right arrow key pressed");
                } else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    Runnable update = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            updateGivenDelete(programDraftArea.getText(0, programDraftArea.getDocument().getLength()));
                        } catch (BadLocationException e1) {
                                e1.printStackTrace();
                            }
                        }
                    };
                    SwingUtilities.invokeLater(update);
                } else {
                    insertUpdate(e);
                }
            }

            public void insertUpdate(KeyEvent e) {
                Runnable update = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            updateGivenNewText(programDraftArea.getText(0, programDraftArea.getDocument().getLength()));
                        } catch (BadLocationException e1) {
                            e1.printStackTrace();
                        }
                    }
                };
                SwingUtilities.invokeLater(update);
            }

            private void updateGivenNewText(String text) {
                try {
                    List<String> nextPossibilities = cardBuilder.getPossibilities(text);
                    if (nextPossibilities.size() == 1) {
                        programDraftArea.getDocument().insertString(programDraftArea.getDocument().getLength(), nextPossibilities.get(0), null);
                        programDraftArea.setCaretPosition(programDraftArea.getDocument().getLength());
                    } else {
                        standardUpdateGivenText(text);
                    }
                } catch (BadLocationException e) {
                    throw new RuntimeException(e);
                }
            }

            private void updateGivenDelete(String text) {
                standardUpdateGivenText(text);
            }

            private void standardUpdateGivenText(String text) {
                List<String> nextPossibilities = cardBuilder.getPossibilities(text);
                if (nextPossibilities.size() == 0) {
                    // Program is either complete or invalid.
                    suggestionMenu.setVisible(false);
                } else {
                    suggestionMenu.removeAll();
                    for (String possibility : nextPossibilities) {
                        JMenuItem menuItem = new JMenuItem(possibility);
                        menuItem.addActionListener(new ActionListener() {

                            @Override
                            public void actionPerformed(ActionEvent e) {
                                programDraftArea.insert(possibility, programDraftArea.getCaretPosition());
                                suggestionMenu.setVisible(false);
                            }
                        });
                        suggestionMenu.add(menuItem);
                    }

                    // Show the suggestion box above the cursor
                    try {
                        Rectangle2D caretBounds = programDraftArea.modelToView2D(programDraftArea.getCaretPosition());
                        suggestionMenu.show(programDraftArea, (int) caretBounds.getX(), (int) (caretBounds.getY() + caretBounds.getHeight()));
                        programDraftArea.requestFocus();

                    } catch (BadLocationException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyReleased(KeyEvent e) {}
            
        });

        JScrollPane scrollPane = new JScrollPane(programDraftArea);   

        mainPanel.add(scrollPane);
        
        window.add(mainPanel);

        window.setVisible(true);

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

//TODO: TWO PARENTHESIS ISN'T VALID FOR SOME REASON!

//TODO: implement card functionality 
//TODO: card database explorer. Called program database. Make a way for every program to have an auto-generated name which is the same as its auto-generated number ID, which comes from what decisions were made in its creation. Could have some mod() operator to condense the name. Or use base 64 encoding, idk.
//TODO: remove extraneous areas of the card grammar.
//TODO: once the card grammar is done, make an event grammar. Maybe start making the game first.
//TODO: optional: make a program that reads and re-formats grammar files? Makes some way for you to clean it up / work with it?
}
