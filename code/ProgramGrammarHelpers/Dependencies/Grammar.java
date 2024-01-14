package code.ProgramGrammarHelpers.Dependencies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Grammar {

    private HashMap<String, List<List<Symbol>>> productions;
    private String goalSymbol;
    private final String[] NO_SPACE_BEFORE;
    private final String[] NO_SPACE_AFTER;
    private final Set<String> whitespaceChars;

    public Grammar() {
        productions = new HashMap<>();

        // There could be another way to decide what characters behave specially about
        // spaces. Here is just a likely default set.
        NO_SPACE_BEFORE = new String[] { ",", ".", "}", ")" };
        NO_SPACE_AFTER = new String[] { "{", ".", "(" };
        whitespaceChars = new HashSet<String>();
        initWhitespaceChars();
    }

    private void initWhitespaceChars() {
        String[] whitespaceStrings = { "\n", "\t", " " };

        for (String s : whitespaceStrings) {
            whitespaceChars.add(s);
        }
    }

    public void add(String nonTerminal, List<Symbol> expansion) {
        if (!productions.containsKey(nonTerminal)) {
            productions.put(nonTerminal, getNewExpansionsList());
        }
        productions.get(nonTerminal).add(getClone(expansion));

    }

    private List<Symbol> getClone(List<Symbol> production) {
        ArrayList<Symbol> copy = new ArrayList<>();
        for (Symbol symbol : production) {
            copy.add(symbol.clone());
        }
        return copy;
    }

    private List<List<Symbol>> getNewExpansionsList() {
        return new ArrayList<>();
    }

    public void setGoalSymbol(String goalSymbol) {
        this.goalSymbol = goalSymbol;
    }

    public Symbol getNewGoalSymbol() {
        // There is no implementation for handling goal nodes which are terminal.
        return new Symbol(goalSymbol, SymbolType.NON_TERMINAL);
    }

    public List<List<Symbol>> getProductions(Symbol symbol) {
        return productions.get(symbol.getValue());
    }

    public String[] getNoSpaceBefores() {
        return NO_SPACE_BEFORE;
    }

    public String[] getNoSpaceAfters() {
        return NO_SPACE_AFTER;
    }

    public List<String> tokenize(String program) {
        /**
         * Skip opening whitespace
         * Deopsit beginCursor at beginning of current token.
         * Advance the cursor until you reach whitespace or a delimiting token (we can
         * define that as )
         * That's a token.
         * If you reached a delimiting token, capture until you reach the last char of
         * the only possible token it could be.
         * That's a token.
         * If there's whitespace, skip it.
         * 
         * Skip whitespace
         * Mark beginning, advance until you've created the largest possible token.
         * Mark end.
         * Repeat.
         * 
         * OR
         * 
         * Condense all whitepsace to just a single space
         * Trim edges of whitespace
         * At the boundary between every letter and non-letter, non-space character, put
         * a space.
         * Split by spaces, that's the tokens.
         * Can check with production tree generation to see if it's a legit program.
         * 
         */
        //TODO: simplify and document.

        String preparedProgram = prepareStringForTokenization(program);

        ArrayList<String> tokens = new ArrayList<>();

        java.util.Scanner tokenSplitter = new java.util.Scanner(preparedProgram);

        while(tokenSplitter.hasNext()) {
            tokens.add(tokenSplitter.next());
        }

        tokenSplitter.close();

        return tokens;
    }

    private String prepareStringForTokenization(String program) {
        if (program.length() == 0) return "";

        StringBuilder preparedString = new StringBuilder();

        int pos = 0;

        // Skip initial whitespace
        while (isWhitespace(program.charAt(pos))) {
            pos++;
            if (pos == program.length()) return "";
        }

        while (pos < program.length()) {
            if (isWhitespace(program.charAt(pos))) {
                if (! isWhitespace(program.charAt(pos - 1))) {
                    preparedString.append(' ');
                }
            } else if (atBorder(program, pos)) {
                preparedString.append(' ');
                preparedString.append(program.charAt(pos));
            } else {
                preparedString.append(program.charAt(pos));
            }
            pos++;
        }

        return preparedString.toString();
    }

    private boolean atBorder(String program, int pos) {
        if (pos == 0) return false;
        return atBorder(program.charAt(pos - 1), program.charAt(pos));
    }

     public boolean atBorder(char a, char b) {
        // if (a == ' ' && isDelimitingToken(b)) return false;
        // if (b == ' ' && isDelimitingToken(a)) return false;
        return isDelimitingToken(a) && ! isDelimitingToken(b) || isDelimitingToken(b) && ! isDelimitingToken(a);
    }

    private boolean isDelimitingToken(char c) {
        return ! (c <= 'z' && c >= 'a' || c <= 'Z' && c >= 'A' || whitespaceChars.contains(c + "") || c >= '0' && c <= '9');
    }

    public boolean isWhitespace(char c) {
        return whitespaceChars.contains(c + "");
    }

}
