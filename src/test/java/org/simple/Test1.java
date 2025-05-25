package org.simple;

import org.junit.Test;
import org.simple.bbs.BB;
import org.simple.bbs.EntryBB;

import static org.junit.Assert.assertEquals;

public class Test1 {
        @Test
        public void testSimpleProgram() {
        Parser parser = new Parser("return ");
        BB ret = parser.parse();
        // check entry block
        assertEquals(1, ret._succs.size());
        assertEquals(0, ret._preds.size());
        // main block
        assertEquals(1, ret._succs.getFirst()._preds.size());
        assertEquals(1, ret._succs.getFirst()._succs.size());

        // exit block
        assertEquals(1, ret._succs.getFirst()._succs.getFirst()._preds.size());
        assertEquals(0, ret._succs.getFirst()._succs.getFirst()._succs.size());

        // get Entry block
        // print graph
        GraphDot graphDot = new GraphDot();
        System.out.println(graphDot.generateDotOutput((EntryBB) ret));
        }
}
