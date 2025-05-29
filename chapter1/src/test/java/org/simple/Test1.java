package org.simple;

import org.junit.Test;
import org.simple.bbs.BB;
import org.simple.bbs.EntryBB;
import org.simple.instructions.Instr;
import org.simple.instructions.ReturnInstr;

import static org.junit.Assert.assertEquals;

public class Test1 {
        @Test
        public void testSimpleProgram() {
        Parser parser = new Parser("return 1;");
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

        @Test
        public void testAddPeephole() {
                Instr._disablePeephole = true;
                Parser parser = new Parser("return 1+2;");
                BB ret = parser.parse();

                GraphDot graphDot = new GraphDot();
                System.out.println(graphDot.generateDotOutput((EntryBB) ret));
        }

        @Test
        public void testSubPeephole() {
                Instr._disablePeephole = true;
                Parser parser = new Parser("return 1-2;");
                BB ret = parser.parse();

                GraphDot graphDot = new GraphDot();
                System.out.println(graphDot.generateDotOutput((EntryBB) ret));
        }


        @Test
        public void testMulPeephole() {
                Instr._disablePeephole = true;
                Parser parser = new Parser("return 2*3;");
                BB ret = parser.parse();

                GraphDot graphDot = new GraphDot();
                System.out.println(graphDot.generateDotOutput((EntryBB) ret));
        }

        @Test
        public void testDivPeephole() {
                Instr._disablePeephole = true;
                Parser parser = new Parser("return 6/3;");
                BB ret = parser.parse();

                GraphDot graphDot = new GraphDot();
                System.out.println(graphDot.generateDotOutput((EntryBB) ret));
        }

        @Test
        public void testMinusPeephole() {
                Instr._disablePeephole = true;
                Parser parser = new Parser("return 6/-3;");
                BB ret = parser.parse();

                GraphDot graphDot = new GraphDot();
                System.out.println(graphDot.generateDotOutput((EntryBB) ret));
        }

        @Test
        public void testExample() {
                Instr._disablePeephole = true;
                Parser parser = new Parser("return 1+2*3+-5;");
                BB ret = parser.parse();

                GraphDot graphDot = new GraphDot();
                System.out.println(graphDot.generateDotOutput((EntryBB) ret));
        }
}

