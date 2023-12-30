package code;

import static org.junit.Assert.*;
import org.junit.Test;

import code.Parser.CardParser;

public class ParserTester {

    @Test
    public void testGetOutput() {
        assertTrue(new CardParser().getOutput().equals("Hello"));
    }

    @Test
    public void testGetOuputFail() {
        assertTrue(! new CardParser().getOutput().equals("Hi"));
    }

    @Test
    public void testGetOutputPass() {
        assertEquals(new CardParser().getOutput(), "Hello");
    }
    

}
