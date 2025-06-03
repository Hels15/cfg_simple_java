package org.simple;

import org.junit.Test;
import org.simple.bbs.BB;
import org.simple.bbs.EntryBB;
import org.simple.instructions.Instr;
import org.simple.instructions.MultiReturnInstr;
import org.simple.instructions.ReturnInstr;
import org.simple.type.TypeInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class Test1 {
        @Test
        public void testSimpleProgram() {
        Parser parser = new Parser("return 1;");
        ReturnInstr ret = (ReturnInstr)parser.parse();
        assertEquals("return 1;", ret.print());
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
        System.out.println(graphDot.generateDotOutput(Parser._entry, Parser._scope));
        }

        @Test
        public void testAddPeephole() {
                Parser parser = new Parser("return 1+2;");
                ReturnInstr ret = (ReturnInstr)parser.parse();

                assertEquals("return 3;", ret.print());
                GraphDot graphDot = new GraphDot();
                System.out.println(graphDot.generateDotOutput(Parser._entry, Parser._scope));
        }

        @Test
        public void testSubPeephole() {
                Parser parser = new Parser("return 1-2;");
                ReturnInstr ret = (ReturnInstr)parser.parse();

                assertEquals("return -1;", ret.print());
                GraphDot graphDot = new GraphDot();
                System.out.println(graphDot.generateDotOutput(Parser._entry, Parser._scope));
        }


        @Test
        public void testMulPeephole() {
                Parser parser = new Parser("return 2*3;");
                ReturnInstr ret = (ReturnInstr)parser.parse();
                assertEquals("return 6;", ret.print());
                GraphDot graphDot = new GraphDot();
                System.out.println(graphDot.generateDotOutput(Parser._entry, Parser._scope));
        }

        @Test
        public void testDivPeephole() {
                Parser parser = new Parser("return 6/3;");
                ReturnInstr ret = (ReturnInstr)parser.parse();

                assertEquals("return 2;", ret.print());

                GraphDot graphDot = new GraphDot();
                System.out.println(graphDot.generateDotOutput(Parser._entry, Parser._scope));
        }

        @Test
        public void testMinusPeephole() {
                Parser parser = new Parser("return 6/-3;");
                ReturnInstr ret = (ReturnInstr)parser.parse();
                assertEquals("return -2;", ret.print());
                GraphDot graphDot = new GraphDot();
                System.out.println(graphDot.generateDotOutput(Parser._entry, Parser._scope));
        }

        @Test
        public void testExample() {
                Parser parser = new Parser("return 1+2*3+-5;");
                ReturnInstr ret = (ReturnInstr)parser.parse();

                assertEquals("return 2;", ret.print());

                GraphDot graphDot = new GraphDot();
                System.out.println(graphDot.generateDotOutput(Parser._entry, Parser._scope));
        }
        @Test
        public void testVarDecl() {
                Parser parser = new Parser("int a = 1; return a; #showGraph;");
                ReturnInstr ret = (ReturnInstr) parser.parse(false);
                assertEquals("return 1;", ret.print());
        }

        @Test
        public void testVarAdd() {
                Parser parser = new Parser("int a=1; int b=2; return a+b; #showGraph;");
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

        @Test
        public void testPeephole() {
                Parser parser = new Parser("return 1+arg+2; #showGraph;");
                ReturnInstr ret = (ReturnInstr)parser.parse();
                assertEquals("return (arg+3);", ret.print());
        }

        @Test
        public void testPeephole2() {
                Parser parser = new Parser("return (1+arg)+2;");
                ReturnInstr ret = (ReturnInstr)parser.parse();
                assertEquals("return (arg+3);", ret.print());
        }

        @Test
        public void testAdd0() {
                Parser parser = new Parser("return 0+arg; #showGraph;");
                ReturnInstr ret = (ReturnInstr)parser.parse();
                assertEquals("return arg;", ret.print());
        }

        @Test
        public void testAddAddMul() {
                Parser parser = new Parser("return arg+0+arg;");
                ReturnInstr ret = (ReturnInstr)parser.parse();
                assertEquals("return (arg*2);", ret.print());
        }

        @Test
        public void testPeephole3() {
                Parser parser = new Parser("return 1+arg+2+arg+3; #showGraph;");
                ReturnInstr ret = (ReturnInstr)parser.parse();
                assertEquals("return ((arg*2)+6);", ret.print());
        }


        @Test
        public void testMul1() {
                Parser parser = new Parser("return 1*arg;");
                ReturnInstr ret = (ReturnInstr)parser.parse();
                assertEquals("return arg;", ret.print());
        }

        @Test
        public void testConstantArg() {
                Parser parser = new Parser("return arg; #showGraph;");
                ReturnInstr ret = (ReturnInstr)parser.parse(false, TypeInteger.constant(2));
                assertEquals("return 2;", ret.print());
        }

        @Test
        public void testCompEq() {
                Parser parser = new Parser("return 3==3; #showGraph;");
                ReturnInstr ret = (ReturnInstr)parser.parse();
                assertEquals("return 1;", ret.print());
        }


        @Test
        public void testCompEq2() {
                Parser parser = new Parser("return 3==4; #showGraph;");
                ReturnInstr ret = (ReturnInstr)parser.parse();
                assertEquals("return 0;", ret.print());
        }

        @Test
        public void testCompNEq() {
                Parser parser = new Parser("return 3!=3; #showGraph;");
                ReturnInstr ret = (ReturnInstr)parser.parse();
                assertEquals("return 0;", ret.print());
        }

        @Test
        public void testCompNEq2() {
                Parser parser = new Parser("return 3!=4; #showGraph;");
                ReturnInstr ret = (ReturnInstr)parser.parse();
                assertEquals("return 1;", ret.print());
        }

        @Test
        public void testBug1() {
                Parser parser = new Parser("int a=arg+1; int b=a; b=1; return a+2; #showGraph;");
                ReturnInstr ret = (ReturnInstr)parser.parse();
                assertEquals("return (arg+3);", ret.print());
        }

        @Test
        public void testBug2() {
                Parser parser = new Parser("int a=arg+1; a=a; return a; #showGraph;");
                ReturnInstr ret = (ReturnInstr)parser.parse();
                assertEquals("return (arg+1);", ret.print());
        }

        @Test
        public void testBug3() {
                try {
                        new Parser("inta=1; return a;").parse();
                        fail();
                } catch( RuntimeException e ) {
                        assertEquals("Undefined name 'inta'",e.getMessage());
                }
        }

        @Test
        public void testBug4() {
                Parser parser = new Parser("return -arg;");
                ReturnInstr ret = (ReturnInstr)parser.parse();
                assertEquals("return (-arg);", ret.print());
        }

        @Test
        public void testBug5() {
                Parser parser = new Parser("return arg--2;");
                ReturnInstr ret = (ReturnInstr)parser.parse();
                assertEquals("return (arg--2);", ret.print());
        }
        @Test
        public void testIfStmt() {
                Parser parser = new Parser(
                """
                int a = 1;
                if (arg == 1)
                    a = arg+2;
                else {
                    a = arg-3;
                    #showGraph;
                }
                #showGraph;
                return a;
                """
                );
                ReturnInstr ret = (ReturnInstr)parser.parse();
                assertEquals("return Phi((arg+2),(arg-3));", ret.print());
        }
        @Test
        public void testTest() {
                Parser parser = new Parser(
                        """
                        int c = 3;
                        int b = 2;
                        if (arg == 1) {
                            b = 3;
                            c = 4;
                        }
                        return c;""");
                ReturnInstr ret = (ReturnInstr)parser.parse(true, TypeInteger.BOT);
                assertEquals("return Phi(4,3);", ret.toString());
        }

        // Todo: come back to this
        @Test
        public void testReturn2() {
                Parser parser = new Parser(
                        """
                        if( arg==1 )
                            return 3;
                        else
                           return 4;
                       #showGraph;""");
                MultiReturnInstr ret = (MultiReturnInstr) parser.parse(true, TypeInteger.BOT);
                assertEquals("[return 3; return 4;]", ret.toString());
        }

        @Test
        public void testIfMergeB() {
                Parser parser = new Parser(
                        """
                        int a=arg+1;
                        int b=0;
                        if( arg==1 )
                            b=a;
                        else
                            b=a+1;
                        return a+b;""");
                ReturnInstr ret = (ReturnInstr)parser.parse(true, TypeInteger.BOT);
                assertEquals("return ((arg*2)+Phi(2,3));", ret.toString());
        }


        @Test
        public void testIfMerge2() {
                Parser parser = new Parser(
                        """
                        int a=arg+1;
                        int b=arg+2;
                        if( arg==1 )
                            b=b+a;
                        else
                            a=b+1;
                        return a+b;""");
                ReturnInstr ret = (ReturnInstr)parser.parse(true, TypeInteger.BOT);
                assertEquals("return ((Phi((arg*2),arg)+arg)+Phi(4,5));", ret.toString());
        }

        @Test
        public void testIfMerge4() {
                Parser parser = new Parser(
                        """
                int a=0;
                int b=0;
                if( arg )
                    a=1;
                if( arg==0 )
                    b=2;
                return arg+a+b;
                #showGraph;""");
                ReturnInstr ret = (ReturnInstr)parser.parse(true, TypeInteger.BOT);
                assertEquals("return ((arg+Phi(1,0))+Phi(2,0));", ret.toString());
        }

        @Test
        public void testIfMerge5 (){
                Parser parser = new Parser(
                        """
                int a=arg==2;
                if( arg==1 )
                {
                    a=arg==3;
                }
                return a;""");
                ReturnInstr ret = (ReturnInstr)parser.parse(true, TypeInteger.BOT);
                assertEquals("return (arg==Phi(3,2));", ret.toString());
        }

        @Test
        public void testTrue (){
                Parser parser = new Parser(
                        """
                return true;
                """);
                ReturnInstr ret = (ReturnInstr)parser.parse(true, TypeInteger.BOT);
                assertEquals("return 1;", ret.toString());
        }

        @Test
        public void testHalfDef() {
                try {
                        new Parser("if( arg==1 ) int b=2; return b;").parse();
                        fail();
                } catch( RuntimeException e ) {
                        assertEquals("Cannot define a new name on one arm of an if",e.getMessage());
                }
        }

        @Test
        public void testHalfDef2() {
                try {
                        new Parser("if( arg==1 ) { int b=2; } else { int b=3; } return b;").parse();
                        fail();
                } catch( RuntimeException e ) {
                        assertEquals("Undefined name 'b'",e.getMessage());
                }
        }

        @Test
        public void testRegress1() {
                try {
                        new Parser("if(arg==2) int a=1; else int b=2; return a;").parse();
                        fail();
                } catch( RuntimeException e ) {
                        assertEquals("Cannot define a new name on one arm of an if",e.getMessage());
                }
        }


        @Test
        public void testBadNum() {
                try {
                        new Parser("return 1-;").parse();
                        fail();
                } catch( RuntimeException e ) {
                        assertEquals("Syntax error, expected an identifier or expression: ;",e.getMessage());
                }
        }

        @Test
        public void testKeyword1() {
                try {
                        new Parser("int true=0; return true;").parse();
                        fail();
                } catch( RuntimeException e ) {
                        assertEquals("Expected an identifier, found 'true'",e.getMessage());
                }
        }

        @Test
        public void testKeyword2() {
                try {
                        new Parser("int else=arg; if(else) else=2; else else=1; return else;").parse();
                        fail();
                } catch( RuntimeException e ) {
                        assertEquals("Expected an identifier, found 'else'",e.getMessage());
                }
        }

        @Test
        public void testKeyword3() {
                try {
                        new Parser("int a=1; ififif(arg)inta=2;return a;").parse();
                        fail();
                } catch( RuntimeException e ) {
                        assertEquals("Syntax error, expected =: (",e.getMessage());
                }
        }
}

