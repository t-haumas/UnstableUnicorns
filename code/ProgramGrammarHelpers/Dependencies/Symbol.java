package code.ProgramGrammarHelpers.Dependencies;

public class Symbol {

    private String name;
    private SymbolType type;

    public Symbol(String name, SymbolType type) {
        this.name = name;
        this.type = type;
    }

    public String getValue() {
        return name;
    }

    public SymbolType getType() {
        return type;
    }

    public Symbol clone() {
        return new Symbol(name, type);
    }

    public void setText(String newText) {
        name = newText;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        Symbol s = (Symbol) o;
        return this.name.equals(s.name) && this.type == s.type;
    }

    public char getLastChar() {
        return name.charAt(name.length() - 1);
    }

    public boolean startsWith(Symbol s) {
        return name.startsWith(s.name);
    }

    public char getFirstChar() {
        return name.charAt(0);
    }

}
