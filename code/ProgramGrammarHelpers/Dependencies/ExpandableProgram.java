package code.ProgramGrammarHelpers.Dependencies;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ExpandableProgram {

    private ArrayList<Symbol> programSymbols;
    private Grammar grammar;

    public ExpandableProgram(List<Symbol> program, Grammar grammar) {
        programSymbols = new ArrayList<>();
        for (Symbol symbol : program) {
            programSymbols.add(symbol.clone());
        }
        this.grammar = grammar;
    }

    public List<String> getText(int desiredLength) {
        List<String> text = new LinkedList<>();
        for (int i = 0; i < programSymbols.size() && text.size() <= desiredLength; i++) {
            if (programSymbols.get(i).getType() == SymbolType.TERMINAL) {
                text.add(programSymbols.get(i).getValue());
            } else {
                break;
            }
        }
        return text;
    }

    public List<ExpandableProgram> getNextExpansions() {
        List<ExpandableProgram> nextPrograms = new ArrayList<>();

        if (programSymbols.size() == 0) {
            System.err.println("Oh no");
        }
        
        int indexOfFirstNonTerminal = -1;
        for (int i = 0; i < programSymbols.size(); i++) {
            if (programSymbols.get(i).getType() == SymbolType.NON_TERMINAL) {
                indexOfFirstNonTerminal = i;
                break;
            }
        }

       

        if (indexOfFirstNonTerminal == -1) { 
            return nextPrograms;
        }

        List<List<Symbol>> productions = grammar.getProductions(programSymbols.get(indexOfFirstNonTerminal));
        if (productions == null) {
            System.err.println("Grammar contains undefined non-terminable symbol: " + programSymbols.get(indexOfFirstNonTerminal).getValue());
            return nextPrograms;
        }
        for (List<Symbol> production : productions) {
            List<Symbol> programCopy = getProgramCopy(programSymbols);
            programCopy.remove(indexOfFirstNonTerminal);
            programCopy.addAll(indexOfFirstNonTerminal, getProgramCopy(production));//getProgramCopyAndAddSpaces(production));
            
            ExpandableProgram nextProgram = new ExpandableProgram(programCopy, grammar);

            nextPrograms.add(nextProgram);

        }

         

        return nextPrograms;
    }

    private List<Symbol> getProgramCopy(List<Symbol> production) {
        ArrayList<Symbol> copy = new ArrayList<>();
        for (Symbol symbol : production) {
            copy.add(symbol.clone());
        }
        return copy;
    }

    public int getNumberOfUnexpandedSymbols() {
        return programSymbols.size();
    }

    public String getFirstSymbol() {
        return programSymbols.get(0).getValue();
    }

    public boolean isCompleteProgram() {
        for (Symbol programSymbol : programSymbols) {
            if (programSymbol.getType() != SymbolType.TERMINAL) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        String output = "";
        for (int i = 0; i < programSymbols.size(); i++) {
            if (programSymbols.get(i).getType() == SymbolType.TERMINAL) {
                output += programSymbols.get(i).getValue();
            } else {
                output += "~" + programSymbols.get(i).getValue() + "~";
            }
            output += " ";
        }
        return output.substring(0, output.length() - 1);
    }


}
