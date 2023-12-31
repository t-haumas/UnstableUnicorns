package code.ProgramGrammarHelpers.Dependencies;

import java.util.ArrayList;
import java.util.Arrays;
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

    public List<Symbol> getText(int desiredLength) {
        List<Symbol> text = new LinkedList<>();
        for (int i = 0; i < programSymbols.size() && text.size() <= desiredLength; i++) {
            if (programSymbols.get(i).getType() == SymbolType.TERMINAL) {
                text.add(programSymbols.get(i).clone());
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

    private void removeUnwantedSpaces() {
        for (int i = 1; i < programSymbols.size(); i++) {
            if (Arrays.asList(grammar.getNoSpaceBefores()).contains(programSymbols.get(i).getValue().charAt(0) + "")) {
                if (programSymbols.get(i - 1).getValue().charAt(programSymbols.get(i - 1).getValue().length() - 1) == ' ') {
                    programSymbols.get(i - 1).setText(programSymbols.get(i - 1).getValue().substring(0, programSymbols.get(i - 1).getValue().length() - 1));
                }
            }
        }
    }

    private List<Symbol> getProgramCopy(List<Symbol> production) {
        ArrayList<Symbol> copy = new ArrayList<>();
        for (Symbol symbol : production) {
            copy.add(symbol.clone());
        }
        return copy;
    }

    private List<Symbol> getProgramCopyAndAddSpaces(List<Symbol> production) {
        ArrayList<Symbol> copy = new ArrayList<>();
        for (int i = 0; i < production.size(); i++) {
            Symbol symbol = production.get(i);
            Symbol newSymbol = symbol.clone();

            if (production.get(i).getType() == SymbolType.TERMINAL) {
                String newText = symbol.getValue();
                if (!Arrays.asList(grammar.getNoSpaceAfters()).contains(symbol.getValue())) {
                    newText += " ";
                }
                newSymbol.setText(newText);
            }

            copy.add(newSymbol);
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
