package org.simple;

import org.junit.Test;
import org.simple.bbs.BB;
import org.simple.bbs.EntryBB;
import org.simple.instructions.Instr;
import org.simple.instructions.MulInstr;
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
        System.out.println(graphDot.generateDotOutput(Parser._entry, parser));
        }

        @Test
        public void testAddPeephole() {
                Parser parser = new Parser("return 1+2;");
                ReturnInstr ret = (ReturnInstr)parser.parse();

                assertEquals("return 3;", ret.print());
                GraphDot graphDot = new GraphDot();
                System.out.println(graphDot.generateDotOutput(Parser._entry, parser));
        }

        @Test
        public void testSubPeephole() {
                Parser parser = new Parser("return 1-2;");
                ReturnInstr ret = (ReturnInstr)parser.parse();

                assertEquals("return -1;", ret.print());
                GraphDot graphDot = new GraphDot();
                System.out.println(graphDot.generateDotOutput(Parser._entry, parser));
        }


        @Test
        public void testMulPeephole() {
                Parser parser = new Parser("return 2*3;");
                ReturnInstr ret = (ReturnInstr)parser.parse();
                assertEquals("return 6;", ret.print());
                GraphDot graphDot = new GraphDot();
                System.out.println(graphDot.generateDotOutput(Parser._entry, parser));
        }

        @Test
        public void testDivPeephole() {
                Parser parser = new Parser("return 6/3;");
                ReturnInstr ret = (ReturnInstr)parser.parse();

                assertEquals("return 2;", ret.print());

                GraphDot graphDot = new GraphDot();
                System.out.println(graphDot.generateDotOutput(Parser._entry, parser));
        }

        @Test
        public void testMinusPeephole() {
                Parser parser = new Parser("return 6/-3;");
                ReturnInstr ret = (ReturnInstr)parser.parse();
                assertEquals("return -2;", ret.print());
                GraphDot graphDot = new GraphDot();
                System.out.println(graphDot.generateDotOutput(Parser._entry, parser));
        }

        @Test
        public void testExample() {
                Parser parser = new Parser("return 1+2*3+-5;");
                ReturnInstr ret = (ReturnInstr)parser.parse();

                assertEquals("return 2;", ret.print());

                GraphDot graphDot = new GraphDot();
                System.out.println(graphDot.generateDotOutput(Parser._entry, parser));
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
                assertEquals("return Phi(bb6,(arg+2),(arg-3));", ret.print());
        }
        // Todo: should be phi node
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
                assertEquals("return Phi(bb6,4,3);", ret.toString());
        }
        
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
                assertEquals("return ((arg*2)+Phi(bb6,2,3));", ret.toString());
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
                assertEquals("return ((Phi(bb6,(arg*2),arg)+arg)+Phi(bb6,4,5));", ret.toString());
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
                assertEquals("return ((arg+Phi(bb6,1,0))+Phi(bb9,2,0));", ret.toString());
        }

        // Same merge point
        @Test
        public void relatedPhiS() {
                Parser parser = new Parser("""
                int a = 1;
                int b = 2;
                if(arg == 1) {
                   a = 3;
                   b = 4;
                }
                #showGraph;
                return arg+a+b;
                """);
                ReturnInstr ret = (ReturnInstr)parser.parse(true, TypeInteger.BOT);
                assertEquals("return (arg+Phi(bb6,7,3));", ret.toString());
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
                assertEquals("return (arg==Phi(bb6,3,2));", ret.toString());
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

        // should just make this return: return1
        @Test
        public void testIf() {
                Parser parser = new Parser("""
                if( true ) return 2;
                return 1;
                """);
                Instr._disablePeephole = true;
                Instr._disablePasses = true;
                MultiReturnInstr ret = (MultiReturnInstr)parser.parse(true, TypeInteger.BOT);
                assertEquals("[return 2; return 1;]", ret.toString());
        }

        @Test public void testIfPeephole() {
                Parser parser = new Parser("""
                                        if( true ) return 2;
                                        return 1;
                        """);
                ReturnInstr ret = (ReturnInstr)parser.parse(true, TypeInteger.BOT);
                assertEquals("return 2;", ret.toString());
        }

        // Todo: implement it from here, rotation, dominators
        @Test public void testPeepholeRotate() {
                Parser parser = new Parser(
                        """
                                int a = 1;
                                if (arg)
                                    a = 2;
                                return (arg < a) < 3;
                                """
                );
                ReturnInstr instr = (ReturnInstr) parser.parse(true, TypeInteger.BOT);
                assertEquals("return ((arg<Phi(bb6,2,1))<3);", instr.toString());
        }

        @Test
        public void testPeepholeCFG() {
                Parser parser = new Parser(
                        """
                        int a=1;
                        if( true )
                          a=2;
                        else
                          a=3;
                        return a;
                        """);
                ReturnInstr instr = (ReturnInstr)parser.parse(true);
                assertEquals("return 2;", instr.toString());
        }

        @Test
        public void testIfIf() {
                Parser parser = new Parser(
                        """
                        int a=1;
                        if( arg!=1 )
                            a=2;
                        else
                            a=3;
                        int b=4;
                        if( a==2 )
                            b=42;
                        else
                            b=5;
                        return b;
                        """);
                ReturnInstr instr = (ReturnInstr)parser.parse(true);
                assertEquals("return Phi(bb9,42,5);", instr.toString());
        }

        @Test
        public void testIfArgIf() {
                Parser parser = new Parser(
                        """
                        int a=1;
                        if( 1==1 )
                            a=2;
                        else
                            a=3;
                        int b=4;
                        if( arg==2 )
                            b=a;
                        else
                            b=5;
                        return b;
                        """);
                ReturnInstr instr = (ReturnInstr)parser.parse(true);
                assertEquals("return Phi(bb9,2,5);", instr.toString());
        }

        // Todo: phi rotate
        @Test
        public void testMerge3With2() {
                Parser parser = new Parser(
                        """
                        int a=1;
                        if( arg==1 )
                            if( arg==2 )
                                a=2;
                            else
                                a=3;
                        else if( arg==3 )
                            a=4;
                        else
                            a=5;
                        return a;
                        """);
                ReturnInstr instr = (ReturnInstr)parser.parse(true, TypeInteger.constant(2));
                assertEquals("return 5;", instr.toString());
        }

        // Todo: phi rotate
        @Test
        public void testMerge3With1() {
                Parser parser = new Parser(
                        """
                        int a=1;
                        if( arg==1 )
                            if( arg==2 )
                                a=2;
                            else
                                a=3;
                        else if( arg==3 )
                            a=4;
                        else
                            a=5;
                        return a;
                        """);
                ReturnInstr instr = (ReturnInstr)parser.parse(true, TypeInteger.constant(1));
                assertEquals("return 3;", instr.toString());
        }

        @Test
        public void testMerge3Peephole() {
                Parser parser = new Parser(
                        """
                        int a=1;
                        if( arg==1 )
                            if( 1==2 )
                                a=2;
                            else
                                a=3;
                        else if( arg==3 )
                            a=4;
                        else
                            a=5;
                        return a;
                        """);
                ReturnInstr instr = (ReturnInstr)parser.parse(true);
                assertEquals("return Phi(bb12,3,Phi(bb11,4,5));", instr.toString());
        }

        @Test
        public void testMerge3Peephole1() {
                Parser parser = new Parser(
                        """
                        int a=1;
                        if( arg==1 )
                            if( 1==2 )
                                a=2;
                            else
                                a=3;
                        else if( arg==3 )
                            a=4;
                        else
                            a=5;
                        return a;
                        """);
                ReturnInstr instr = (ReturnInstr)parser.parse(true, TypeInteger.constant(1));
                assertEquals("return 3;", instr.toString());
        }


        @Test
        public void testMerge3Peephole3() {
                Parser parser = new Parser(
                        """
                        int a=1;
                        if( arg==1 )
                            if( 1==2 )
                                a=2;
                            else
                                a=3;
                        else if( arg==3 )
                            a=4;
                        else
                            a=5;
                        return a;
                        """);
                ReturnInstr instr = (ReturnInstr)parser.parse(true, TypeInteger.constant(3));
                assertEquals("return 4;", instr.toString());
        }

        // 0 2 and 1 2 = 41
        // Todo: fix this(dominators are missing)
        @Test
        public void testDemo1NonConst() {
                Parser parser = new Parser(
                        """
                   
                        int a = 0;
                        int b = 1;
                        if( arg ) {
                            a = 2;
                            if( arg ) { b = 2; }
                            else b = 3;
                        }
                        return a+b;
                        """);
                ReturnInstr instr = (ReturnInstr)parser.parse(true);
                assertEquals("return Phi(bb9,4,1);", instr.toString());
        }

        @Test
        public void testDemo1True() {
                Parser parser = new Parser(
                        """
                  
                        int a = 0;
                        int b = 1;
                        if( arg ) {
                            a = 2;
                            if( arg ) { b = 2; }
                            else b = 3;
                        }
                        return a+b;
                        """);
                ReturnInstr instr = (ReturnInstr)parser.parse(false, TypeInteger.constant(1));
                assertEquals("return 4;", instr.toString());
        }

        @Test
        public void testDemo1False() {
                Parser parser = new Parser(
                        """
                        int a = 0;
                        int b = 1;
                        if( arg ) {
                            a = 2;
                            if( arg ) { b = 2; }
                            else b = 3;
                        }
                        return a+b;
                        """);
                ReturnInstr instr = (ReturnInstr)parser.parse(false, TypeInteger.constant(0));
                assertEquals("return 1;", instr.toString());
        }

        @Test
        public void testDemo2NonConst() {
                Parser parser = new Parser(
                        """
                        int a = 0;
                        int b = 1;
                        int c = 0;
                        if( arg ) {
                            a = 1;
                            if( arg==2 ) { c=2; } else { c=3; }
                            if( arg ) { b = 2; }
                            else b = 3;
                        }
                        return a+b+c;
                        """);
                ReturnInstr instr = (ReturnInstr)parser.parse(false);
                assertEquals("return (Phi(bb12,Phi(bb8,2,3),0)+Phi(bb12,3,1));", instr.toString());
        }

        // Todo: phi rotate
        @Test
        public void testDemo2True() {
                Parser parser = new Parser(
                        """
                        int a = 0;
                        int b = 1;
                        int c = 0;
                        if( arg ) {
                            a = 1;
                            if( arg==2 ) { c=2; } else { c=3; }
                            if( arg ) { b = 2; }
                            else b = 3;
                        }
                        return a+b+c;
                        """);
                ReturnInstr instr = (ReturnInstr)parser.parse(false, TypeInteger.constant(1));
                assertEquals("return 6;", instr.toString());
        }

        @Test
        public void testDemo2arg2() {
                Parser parser = new Parser(                        """
                        int a = 0;
                        int b = 1;
                        int c = 0;

                        if( arg ) {
                        a = 1;
                        if( arg==2 ) { c=2; } else { c=3; }
                        if( arg ) { b = 2; }
                        else b = 3;
                        }
                        return a+b+c;
                        """);
                ReturnInstr instr = (ReturnInstr)parser.parse(false, TypeInteger.constant(2));
                assertEquals("return 5;", instr.toString());
        }

        // Maybe specific Loop as bbid instead of bb prefix
        // Loops
        @Test
        public void testExampleLoop() {
                Parser parser = new Parser(                        """
                        while(arg < 10) {
                                    arg = arg + 1;
                                }
                        return arg;
                        """);
                Instr._disablePeephole = true;
                MultiReturnInstr instr = (MultiReturnInstr)parser.parse(true);
                assertEquals("[return Phi(bb4,arg,(Phi_arg+1));]", instr.toString());
        }

        @Test
        public void testRegression() {
                Parser parser = new Parser(                        """
                        int a = 1;
                        if(arg){}else{
                            while(a < 10) {
                                a = a + 1;
                            }
                        }
                        return a;
                        """);
                ReturnInstr instr = (ReturnInstr)parser.parse(false);
                assertEquals("return Phi(bb9,1,Phi(bb6,1,(Phi_a+1)));", instr.toString());
        }

        @Test
        public void testWhileNested() {
                Parser parser = new Parser(                        """
                        int sum = 0;
                        int i = 0;
                        while(i < arg) {
                            i = i + 1;
                            int j = 0;
                            while( j < arg ) {
                                sum = sum + j;
                                j = j + 1;
                            }
                        }
                        return sum;
                        """);
                ReturnInstr instr = (ReturnInstr)parser.parse(false);
                assertEquals("return Phi(bb4,0,Phi(bb7,Phi_sum,(Phi(bb7,0,(Phi_j+1))+Phi_sum)));", instr.toString());
        }

        @Test
        public void testWhileScope() {
                Parser parser = new Parser(                        """
                int a = 1;
                int b = 2;
                while(a < 10) {
                        if (a == 2) a = 3;
                        else b = 4;
                }
                return b;
                        """);
                Instr._disablePeephole = true;
                MultiReturnInstr instr = (MultiReturnInstr) parser.parse(false);
                assertEquals("[return Phi(bb4,2,Phi(bb9,Phi_b,4));]", instr.toString());
                Instr._disablePeephole = false;
        }


        @Test
        public void testWhileNestedIfAndInc() {
                Parser parser = new Parser(                        """
                        int a = 1;
                        int b = 2;
                        while(a < 10) {
                            if (a == 2) a = 3;
                            else b = 4;
                            b = b + 1;
                            a = a + 1;
                        }
                        return b;
                        """);
                ReturnInstr instr = (ReturnInstr)parser.parse(false);
                assertEquals("return Phi(bb4,2,(Phi(bb9,Phi_b,4)+1));", instr.toString());
        }


        @Test
        public void testWhile() {
                Parser parser = new Parser(                        """
                        int a = 1;
                        while(a < 10) {
                            a = a + 1;
                            a = a + 2;
                        }
                        return a;
                        """);
                Instr._disablePeephole = true;
                MultiReturnInstr instr = (MultiReturnInstr)parser.parse(false);
                assertEquals("[return Phi(bb4,1,((Phi_a+1)+2));]", instr.toString());
                Instr._disablePeephole = false;
        }

        @Test
        public void testWhilePeep() {
                Parser parser = new Parser(                        """
                        int a = 1;
                        while(a < 10) {
                            a = a + 1;
                            a = a + 2;
                        }
                        return a;
                        """);
                ReturnInstr instr = (ReturnInstr)parser.parse(false);
                assertEquals("return Phi(bb4,1,(Phi_a+3));", instr.toString());
        }


        @Test
        public void testWhile2() {
                Parser parser = new Parser(                        """
                        int a = 1;
                        while(arg) a = 2;
                        return a;
                        """);
                Instr._disablePeephole = true;
                MultiReturnInstr instr = (MultiReturnInstr)parser.parse(false);
                assertEquals("[return Phi(bb4,1,2);]", instr.toString());
                Instr._disablePeephole = false;
        }

        @Test
        public void testWhile2Peep() {
                Parser parser = new Parser(                        """
                        int a = 1;
                        while(arg) a = 2;
                        return a;
                        """);
                ReturnInstr instr = (ReturnInstr)parser.parse(false);
                assertEquals("return Phi(bb4,1,2);", instr.toString());
        }


        @Test
        public void testWhile3() {
                Parser parser = new Parser(                        """
                        int a = 1;
                        while(a < 10) {
                            int b = a + 1;
                            a = b + 2;
                        }
                        return a;
                        """);
                Instr._disablePeephole = true;
                MultiReturnInstr instr = (MultiReturnInstr)parser.parse(false);
                assertEquals("[return Phi(bb4,1,((Phi_a+1)+2));]", instr.toString());
                Instr._disablePeephole = false;
        }


        @Test
        public void testWhile3Peep() {
                Parser parser = new Parser(                        """
                        int a = 1;
                        while(a < 10) {
                            int b = a + 1;
                            a = b + 2;
                        }
                        return a;
                        """);
                ReturnInstr instr = (ReturnInstr)parser.parse(false);
                assertEquals("return Phi(bb4,1,(Phi_a+3));", instr.toString());
        }


        @Test
        public void testWhile4() {
                Parser parser = new Parser(                        """
                        int a = 1;
                        int b = 2;
                        while(a < 10) {
                            int b = a + 1;
                            a = b + 2;
                        }
                        return a;
                        """);
                Instr._disablePeephole = true;
                MultiReturnInstr instr = (MultiReturnInstr)parser.parse(false);
                assertEquals("[return Phi(bb4,1,((Phi_a+1)+2));]", instr.toString());
                Instr._disablePeephole = false;
        }


        @Test
        public void testWhile4Peep() {
                Parser parser = new Parser(                        """
                        int a = 1;
                        int b = 2;
                        while(a < 10) {
                            int b = a + 1;
                            a = b + 2;
                        }
                        return a;
                        """);
                ReturnInstr instr = (ReturnInstr)parser.parse(false);
                assertEquals("return Phi(bb4,1,(Phi_a+3));", instr.toString());
        }

        // Add it after binary
        // Multiple inputs to a region here?
        @Test public void testLogicalOr() {
                Parser parser = new Parser(
                        """
                                int a = 2;
                                int b = 3;
                                int arg = 1;
                                if(a || b) {
                                        arg = arg + 1;
                                }
                                return arg;
                                """
                );
                ReturnInstr instr = (ReturnInstr)parser.parse();
                assertEquals("return (arg||arg);", instr.toString());
        }

        @Test public void testLogicalAnd() {
                Parser parser = new Parser(
                        """
                                int a = 2;
                                int b = 3;
                                int arg = 1;
                                if(a && b) {
                                        arg = arg + 1;
                                }
                                return arg;
                                """
                );
                ReturnInstr instr = (ReturnInstr)parser.parse();
                assertEquals("return (arg&&arg);", instr.toString());
        }

        @Test public void testEagerPhiKill() {
                Parser parser = new Parser("""
                       int a = 1;
                       int b = 1; 
                       while(a < 10) {
                            a = a + 1;
                            a = a + 2;
                        }
                        return a;
                        """);
                ReturnInstr instr = (ReturnInstr)parser.parse(false);
                assertEquals("return Phi(bb4,1,(Phi_a+3));", instr.toString());
        }

        // break - continue stuff
        @Test public void testEx6() {
                Parser parser = new Parser("""
                        while(arg < 10) {
                            arg = arg + 1;
                            if (arg == 5)
                                break;
                            if (arg == 6)
                                break;
                        }
                        return arg;
         """);
                GraphEval eval = new GraphEval(Parser._entry);
                ReturnInstr instr = (ReturnInstr)parser.parse(true);
                assertEquals("return Phi(bb4,arg,(Phi_arg+1));", instr.toString());
                assertEquals(5, eval.evaluate(1).value());
                assertEquals(10, eval.evaluate(6).value());
        }

        // Failed
        @Test public void testEx5() {
                Parser parser = new Parser("""
                        int a = 1;
                        while(arg < 10) {
                            arg = arg + 1;
                            if (arg == 5)
                                continue;
                            if (arg == 7)
                                continue;
                            a = a + 1;
                        }
                        return a;
                        """);
                GraphEval eval = new GraphEval(Parser._entry);
                ReturnInstr instr = (ReturnInstr)parser.parse(true);
                assertEquals(1, eval.evaluate(1).value());
                assertEquals("return Phi(bb4,1,(Phi_a+1));", instr.toString());
        }

        // Failed
        @Test public void testEx4() {
                Parser parser = new Parser("""
                        while(arg < 10) {
                            arg = arg + 1;
                            if (arg == 5)
                                continue;
                            if (arg == 6)
                                break;
                        }
                        return arg;
                        """);
                GraphEval eval = new GraphEval(Parser._entry);
                ReturnInstr instr = (ReturnInstr)parser.parse(true);
                assertEquals(6, eval.evaluate(1).value());
                assertEquals("return Phi(bb4,arg,(Phi_arg+1));", instr.toString());
        }


        @Test public void testEx3() {
                Parser parser = new Parser("""
                        while(arg < 10) {
                            arg = arg + 1;
                            if (arg == 6)
                                break;
                        }
                        return arg;
                        """);
                GraphEval eval = new GraphEval(Parser._entry);
                ReturnInstr instr = (ReturnInstr)parser.parse(true);
                assertEquals(6, eval.evaluate(1).value());
                assertEquals("return Phi(bb4,arg,(Phi_arg+1));", instr.toString());
        }

        // Failed
        @Test public void testEx2() {
                Parser parser = new Parser("""
                        while(arg < 10) {
                            arg = arg + 1;
                            if (arg == 5)
                                continue;
                            if (arg == 6)
                                continue;
                        }
                        return arg;
                        """);
                GraphEval eval = new GraphEval(Parser._entry);
                ReturnInstr instr = (ReturnInstr)parser.parse(true);
                assertEquals(10, eval.evaluate(1).value());
                assertEquals("return Phi(bb4,arg,(Phi_arg+1));", instr.toString());
        }

        // Failed
        @Test public void testEx1() {
                Parser parser = new Parser("""
                        while(arg < 10) {
                            arg = arg + 1;
                            if (arg == 5)
                                continue;
                        }
                        return arg;
                        """);
                GraphEval eval = new GraphEval(Parser._entry);
                ReturnInstr instr = (ReturnInstr)parser.parse(true);
                assertEquals(10, eval.evaluate(1).value());
                assertEquals("return Phi(bb4,arg,(Phi_arg+1));", instr.toString());
        }


        @Test public void testRegress1BreakCon() {
                Parser parser = new Parser("""
                        while( arg < 10 ) {
                            int a = arg+2;
                            arg = arg + 1;
                            if( a > 4 )
                                break;
                        }
                        return arg;
                        """);
                GraphEval eval = new GraphEval(Parser._entry);
                ReturnInstr instr = (ReturnInstr)parser.parse(true);
                assertEquals(4, eval.evaluate(1).value());
                assertEquals("return Phi(bb4,arg,(Phi_arg+1));", instr.toString());
        }

        @Test public void testBreakOutsideLoop() {
                try {
                        Parser parser = new Parser("""
                                if(arg <= 10) {
                                    break;
                                    arg = arg + 1;
                                }
                                return arg;
                                """);
                        ReturnInstr instr = (ReturnInstr)parser.parse(false);
                        fail();
                }
                catch( RuntimeException e ) {
                        assertEquals("No active loop for a break or continue",e.getMessage());
                }
        }

        // More GVN examples
        @Test public void testGVN1() {
                Parser parser = new Parser(
        """
                int x = arg + arg;
                if(arg < 10) {
                    return arg + arg;
                }
                else {
                    x = x + 1;
                }
                return x;
               """
                );

                MultiReturnInstr ret = (MultiReturnInstr)parser.parse(true);
                GraphEval eval = new GraphEval(Parser._entry);
                //System.out.print(IRPrinter.prettyPrint(Parser._entry));
                GraphDot graph = new GraphDot();
                System.out.print(graph.generateDotOutput(Parser._entry, parser));
                assertEquals("[return (arg*2); return (Mul+1);]", ret.toString());
                // infinite loop
                assertEquals(2, eval.evaluate(1).value());
                assertEquals(23, eval.evaluate(11).value());
        }

        @Test public void testGVN2() {
                Parser parser = new Parser(
                        """
                        return arg*arg-arg*arg;
                        """
                );
                GraphEval eval = new GraphEval(Parser._entry);
                ReturnInstr ret = (ReturnInstr)parser.parse(true);
                assertEquals("return 0;", ret.toString());
                assertEquals(0, eval.evaluate(1).value());
        }
        // WorkList algo here

        @Test public void testWorklist1() {
                Parser parser = new Parser(
                 """
                         int step = 1;
                         while (arg < 10) {
                             arg = arg + step + 1;
                         }
                         return arg;
                        """
                );
                GraphEval eval = new GraphEval(Parser._entry);
                ReturnInstr instr = (ReturnInstr)parser.parse(true).iterate();
                assertEquals("return Phi(bb4,arg,(Phi_arg+2));", instr.toString());
                assertEquals(11, eval.evaluate(1).value());
        }

        @Test public void testWorklist2() {
                Parser parser = new Parser(
                """
                int cond = 0;
                int one = 1;
                while (arg < 10) {
                    if (cond) one = 2;
                    arg = arg + one*3 + 1;
                }
                return arg;
               
                """);

                ReturnInstr instr = (ReturnInstr)parser.parse(true).iterate();

                Parser._pass.bb_dead_main(Parser._entry, parser);
                Parser._pass.bb_combine_main(Parser._entry, parser);

                GraphEval eval = new GraphEval(Parser._entry);
                System.out.print(GraphDot.generateDotOutput(Parser._entry, parser));
                assertEquals("return Phi(bb4,arg,(Phi_arg+4));", instr.toString());
                assertEquals(13, eval.evaluate(1).value());
        }

        @Test public void testWorklist3() {
                Parser parser = new Parser(
                        """
                                int v1 = 0;
                                int v2 = 0;
                                int v3 = 0;
                                int v4 = 0;
                                int v5 = 0;
                                int v6 = 0;
                                int v7 = 0;
                                int v8 = 0;
                                while (arg) {
                                    if (v1) v2 = 1;
                                    if (v2) v3 = 1;
                                    if (v3) v4 = 1;
                                    if (v4) v5 = 1;
                                    if (v5) v6 = 1;
                                    if (v6) v7 = 1;
                                    if (v7) v8 = 1;
                                    arg = arg + v8 + 1;
                                }
                                return arg;
                        )
                        """);
                GraphEval eval = new GraphEval(Parser._entry);
                ReturnInstr instr = (ReturnInstr)parser.parse(true).iterate();
                assertEquals("return Phi(Loop14,arg,(Phi_arg+1));", instr.toString());
        }

}

