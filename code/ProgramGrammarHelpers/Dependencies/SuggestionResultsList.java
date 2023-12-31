package code.ProgramGrammarHelpers.Dependencies;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SuggestionResultsList {

    private List<String> stringList;
    private boolean complete;

    public SuggestionResultsList() {
        stringList = new ArrayList<>();
        complete = false;
    }

    public SuggestionResultsList(SuggestionResultsSet resultsSet) {
        stringList = new ArrayList<>();
        stringList.addAll(resultsSet.getStringSet());
        complete = resultsSet.isComplete();
    }

    public List<String> getStringList() {
        return new ArrayList<>(stringList);
    }

    public boolean isComplete() {
        return complete;
    }

    public void sort() {
        Collections.sort(stringList);
    }

    public int size() {
        return stringList.size();
    }

    public String get(int i) {
        return stringList.get(i);
    }

    public void set(int i, String s) {
        stringList.set(i, s);
    }

    public void add(String s) {
        stringList.add(s);
    }

    public void setComplete(boolean c) {
        complete = c;
    }
}
