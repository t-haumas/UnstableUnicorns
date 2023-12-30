package code.Parser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

/**
 * 
 * Some notes on proper grammar files:
 * - Every nonterminal must have no whitespace in its name.
 * 
 */
public class GrammarReader {

    private final String FILE_PREFIX = "/files/Grammars/";
    private Grammar grammar;

    public GrammarReader(String grammarFileName) {
        InputStream grammarFile = GrammarReader.class.getResourceAsStream(FILE_PREFIX + grammarFileName);
        java.util.Scanner fileScanner;

        fileScanner = new java.util.Scanner(grammarFile);
        String currentLine;
        Scanner lineScanner;
        String currentProductionBeginning = null;
        grammar = new Grammar();
        ArrayList<Symbol> currentExpansion = new ArrayList<>();
        while (fileScanner.hasNextLine()) {
            currentLine = fileScanner.nextLine();
            lineScanner = new Scanner(currentLine);
            while (lineScanner.hasNext()) {
                String currentToken = lineScanner.next();
                if (currentToken.charAt(currentToken.length() - 1) == ':') {
                    // Beginning of a production
                    currentToken = currentToken.substring(0, currentToken.length() - 1);
                    if (currentProductionBeginning == null) {
                        grammar.setGoalSymbol(currentToken);
                    }
                    currentProductionBeginning = currentToken;
                } else {
                    if (currentToken.charAt(0) == '\"') {
                        while (!(currentToken.charAt(currentToken.length() - 1) == '\"')) {
                            currentToken += " " + lineScanner.next();
                        }
                        currentExpansion.addAll(splitAndMakeSymbols(currentToken.substring(1, currentToken.length() - 1), " "));
                    } else {
                        currentExpansion.add(new Symbol(currentToken, SymbolType.NON_TERMINAL));
                    }
                }
            }
            if (currentExpansion.size() != 0) {
                grammar.add(currentProductionBeginning, currentExpansion);
                currentExpansion.removeAll(currentExpansion);
            }
        }

        fileScanner.close();
    }

    /**
     * As of now, this function ALWAYS makes a list of TERMINAL symbols!
     * @param s
     * @param delimiter
     * @return
     */
    private List<? extends Symbol> splitAndMakeSymbols(String s, String delimiter) {
        LinkedList<Symbol> tokens = new LinkedList<>();
        
        Scanner splitter = new Scanner(s);
        splitter.useDelimiter(delimiter);

        while(splitter.hasNext()) {
            tokens.add(new Symbol(splitter.next(), SymbolType.TERMINAL));
        }

        splitter.close();
        return tokens;
    }

    public Grammar getGrammar() {
        return grammar;
    }

}
