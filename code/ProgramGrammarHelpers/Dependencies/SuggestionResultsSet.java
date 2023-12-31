package code.ProgramGrammarHelpers.Dependencies;

import java.util.HashSet;
import java.util.Set;

public class SuggestionResultsSet {

    private Set<String> stringSet;
    private boolean complete;

    public SuggestionResultsSet() {
        stringSet = new HashSet<>();
        complete = false;
    }

    public Set<String> getStringSet() {
        return new HashSet<>(stringSet);
    }

    public void setComplete(boolean b) {
        complete = b;
    }

    public void add(String s) {
        stringSet.add(s);
    }

    public int size() {
        return stringSet.size();
    }

    public String getElementIfOnlyElement() {
        return stringSet.iterator().next();
    }

    public boolean isComplete() {
        return complete;
    }

}
