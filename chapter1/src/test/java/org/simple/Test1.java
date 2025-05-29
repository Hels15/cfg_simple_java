package org.simple;

import org.junit.Test;
import org.simple.bbs.BB;
import org.simple.bbs.EntryBB;
import org.simple.instructions.Instr;
import org.simple.instructions.ReturnInstr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class Test1 {
        @Test
        public void testSimpleProgram() {
        Parser parser = new Parser("return 1;");
        ReturnInstr ret = (ReturnInstr)parser.parse();
        // check entry block
        assertEquals(1, Parser._entry._succs.size());
        assertEquals(0, Parser._entry._preds.size());
        // main block
        assertEquals(1,  Parser._entry._succs.getFirst()._preds.size());
        assertEquals(1,  Parser._entry._succs.getFirst()._succs.size());

        // exit block
        assertEquals(1,  Parser._entry._succs.getFirst()._succs.getFirst()._preds.size());
        assertEquals(0,  Parser._entry._succs.getFirst()._succs.getFirst()._succs.size());

        // get Entry block
        // print graph
        GraphDot graphDot = new GraphDot();
        System.out.println(graphDot.generateDotOutput(Parser._entry));
        }

        @Test
        public void testAddPeephole() {
                Instr._disablePeephole = true;
                Parser parser = new Parser("return 1+2;");
                ReturnInstr ret = (ReturnInstr)parser.parse();

                assertEquals("return 3;", ret.print());
                GraphDot graphDot = new GraphDot();
                System.out.println(graphDot.generateDotOutput(Parser._entry));
        }

        @Test
        public void testSubPeephole() {
                Instr._disablePeephole = true;
                Parser parser = new Parser("return 1-2;");
                ReturnInstr ret = (ReturnInstr)parser.parse();

                assertEquals("return -1;", ret.print());
                GraphDot graphDot = new GraphDot();
                System.out.println(graphDot.generateDotOutput(Parser._entry));
        }


        @Test
        public void testMulPeephole() {
                Instr._disablePeephole = true;
                Parser parser = new Parser("return 2*3;");
                ReturnInstr ret = (ReturnInstr)parser.parse();
                assertEquals("return 6;", ret.print());
                GraphDot graphDot = new GraphDot();
                System.out.println(graphDot.generateDotOutput(Parser._entry));
        }

        @Test
        public void testDivPeephole() {
                Instr._disablePeephole = true;
                Parser parser = new Parser("return 6/3;");
                ReturnInstr ret = (ReturnInstr)parser.parse();

                assertEquals("return 2;", ret.print());

                GraphDot graphDot = new GraphDot();
                System.out.println(graphDot.generateDotOutput(Parser._entry));
        }

        @Test
        public void testMinusPeephole() {
                Instr._disablePeephole = true;
                Parser parser = new Parser("return 6/-3;");
                ReturnInstr ret = (ReturnInstr)parser.parse();
                assertEquals("return -2;", ret.print());
                GraphDot graphDot = new GraphDot();
                System.out.println(graphDot.generateDotOutput(Parser._entry));
        }

        @Test
        public void testExample() {
                Instr._disablePeephole = true;
                Parser parser = new Parser("return 1+2*3+-5;");
                ReturnInstr ret = (ReturnInstr)parser.parse();

                assertEquals("return 2;", ret.print());

                GraphDot graphDot = new GraphDot();
                System.out.println(graphDot.generateDotOutput(Parser._entry));
        }
        @Test
        public void testVarDecl() {
                Parser parser = new Parser("int a = 1; return a;");
                ReturnInstr ret = (ReturnInstr) parser.parse(true);
                assertEquals("return 1;", ret.print());
        }

        @Test
        public void testVarAdd() {
                Parser parser = new Parser("int a=1; int b=2; return a+b;");
                ReturnInstr ret = (ReturnInstr)parser.parse(true);
                assertEquals("return 3;", ret.print());
        }

        @Test
        public void testVarScope() {
                Parser parser = new Parser("int a=1; int b=2; int c=0; { int b=3; c=a+b; } return c;");
                ReturnInstr ret = (ReturnInstr)parser.parse();
                assertEquals("return 4;", ret.print());
        }

        @Test
        public void testVarScopeNoPeephole() {
                Parser parser = new Parser("int a=1; int b=2; int c=0; { int b=3; c=a+b; #showGraph; } return c; #showGraph;");
                Instr._disablePeephole = true;
                ReturnInstr ret = (ReturnInstr)parser.parse(true);
                Instr._disablePeephole = false;
                assertEquals("return (1+3);", ret.print());
        }

        @Test
        public void testVarDist() {
                Parser parser = new Parser("int x0=1; int y0=2; int x1=3; int y1=4; return (x0-x1)*(x0-x1) + (y0-y1)*(y0-y1); #showGraph;");
                ReturnInstr ret = (ReturnInstr)parser.parse(true);
                assertEquals("return 8;", ret.print());
        }

        @Test
        public void testSelfAssign() {
                try {
                        new Parser("int a=a; return a;").parse();
                        fail();
                } catch( RuntimeException e ) {
                        assertEquals("Undefined name 'a'",e.getMessage());
                }
        }


        @Test
        public void testRedeclareVar() {
                try {
                        new Parser("int a=1; int b=2; int c=0; int b=3; c=a+b;").parse();
                        fail();
                } catch( RuntimeException e ) {
                        assertEquals("Redefining name 'b'",e.getMessage());
                }
        }

        @Test
        public void testBad1() {
                try {
                        new Parser("int a=1; int b=2; int c=0; { int b=3; c=a+b;").parse();
                        fail();
                } catch( RuntimeException e ) {
                        assertEquals("Syntax error, expected }: ",e.getMessage());
                }
        }

}

