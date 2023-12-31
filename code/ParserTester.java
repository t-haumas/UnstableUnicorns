package code;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import code.ProgramGrammarHelpers.Dependencies.Grammar;
import code.ProgramGrammarHelpers.Dependencies.GrammarReader;
import code.ProgramGrammarHelpers.Dependencies.ProgramCompleter;

public class ParserTester {

    private ProgramCompleter cardBuilder;

    @Before
    public void setup() {
        GrammarReader grammarReader = new GrammarReader("cardGrammar.txt");
        Grammar myGrammar = grammarReader.getGrammar();
        cardBuilder = new ProgramCompleter(myGrammar);
    }

    @Test
    public void testFromRecommendations() {
        System.out.println(cardBuilder.getPossibilities("f"));
        assertTrue(cardBuilder.getPossibilities("f").get(0).equals("rom"));
        assertTrue(cardBuilder.getPossibilities("f").size() == 1);
    }    

}
