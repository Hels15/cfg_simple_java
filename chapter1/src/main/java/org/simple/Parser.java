package org.simple;

import org.simple.bbs.BB;
import org.simple.bbs.EntryBB;
import org.simple.bbs.ExitBB;
import org.simple.instructions.*;
import org.simple.type.Type;
import org.simple.type.TypeInteger;

import java.awt.color.ICC_ColorSpace;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;


public class Parser {
    private final Lexer _lexer;

    public static EntryBB _entry;
    public static BB _cBB; // represents the current basic block where instructions are inserted
    public static ExitBB _exit;
    public static ScopeInstr _scope;
    public static PassManager _pass;
    private final HashSet<String> KEYWORDS = new HashSet<>(){{
        add("else");
        add("false");
        add("if");
        add("int");
        add("return");
        add("true");
        add("while");
        add("break");
        add("continue");
    }};

    public ArrayList<ReturnInstr> _returns;

    ScopeInstr _continueScope;
    ScopeInstr _breakScope;
    boolean _in_loop;

    static class LoopContext {
        BB header;
        BB shared_exit;
        BB real_exit;

        LoopContext(BB header, BB shared_exit, BB real_exit) {
            this.header = header;
            this.shared_exit = shared_exit;
            this.real_exit = real_exit;
        }
    }

    Stack<LoopContext> _loopStack;

    public Parser(String source) {
        _lexer = new Lexer(source);
        _scope = new ScopeInstr();

        _entry = new EntryBB();
        _entry._label = "entry";
        _entry._kind = BB.BBKind.ENTRY;

        _cBB   = new BB("cbb"); // current basic block is the entry block

        _exit  = new ExitBB();
        _exit._label = "exit";
        _entry._kind = BB.BBKind.EXIT;

        _returns  = new ArrayList<>();
        _continueScope = _breakScope = null;
        _loopStack = new Stack<>();
        _in_loop = false;
        _pass = new PassManager();
    }
    public Instr parse() {return parse(false);}
    public Instr parse(boolean show, TypeInteger arg) {

        _scope.push();
        _entry.addSuccessor(_cBB);
        // For now no jumping instructions

        _scope.define(ScopeInstr.ARG0, new ConstantInstr(arg, ScopeInstr.ARG0, _cBB).peephole());

        var ret = parseBlock();
        // add it end the end when the graph is complete
        _scope.pop();

        if(!_lexer.isEOF()) throw error("Syntax error, unexpected " + _lexer.getAnyNextToken());

        MultiReturnInstr instra = new MultiReturnInstr(_cBB);

        // As it turns out, these passes are needed to get the sound IR
        // they are not optional

         _pass.bb_dead_main(_entry);
         _pass.bb_combine_main(_entry);

        System.out.print("Dce Passes: " + _pass.dce_pass + "\n");
        System.out.print("Combine Passes: " + _pass.dce_pass + "\n");
        // collect now returns from new graph
//        _pass.collect_returns(_entry, _returns);

        for(ReturnInstr r : _returns) {
            instra.addDef(r);
        }
        if(show) showGraph();
        return instra.peephole();
    }
    public Instr parse(boolean show) {
        return parse(show, TypeInteger.BOT);
    }

    private String requireId() {
        String id = _lexer.matchId();
        if(id != null && !KEYWORDS.contains(id)) return id;
        throw error("Expected an identifier, found '"+id+"'");
    }
    private Instr parseStatement() {
        if (matchx("return")) return parseReturn();
        else if(matchx("int")) return parseDecl();
        else if(matchx("{")) return require(parseBlock(), "}");
        else if(matchx("if")) return parseIf();
        else if(matchx("while")) return parseWhile();
        else if(matchx("break")) return parseBreak();
        else if(matchx("continue")) return parseContinue();
        else if(matchx("#showGraph")) return require(showGraph(), ";");
        else if(matchx(";")) return null;
        else return parseExpressionStatement();
    }

    private Instr parseBreak() {
        checkLoopActive();
        _cBB.clear_succs();
        _cBB.addSuccessor(_loopStack.peek().shared_exit);
        // This flag will make sure that we wont
        // add the bb that the break is as a predecessor
        // for the succeeding if
        _cBB._kind = BB.BBKind.BREAK;

        BreakInstr br = new BreakInstr(_cBB);
        _cBB.addInstr(br);
        return br;
    }

    private Instr parseContinue() {
        checkLoopActive();
        _cBB.clear_succs();
        _cBB.addSuccessor(_loopStack.peek().header);
        // This flag will make sure that we wont
        // add the bb that the break is as a predecessor
        // for the succeeding if
        _cBB._kind = BB.BBKind.CONTINUE;
        ContinueInstr cont = new ContinueInstr(_cBB);
        _cBB.addInstr(cont);
        return cont;
    }

    void checkLoopActive() {
        if(!_in_loop) throw Utils.TODO("No active loop for a break or continue");
    }

    private Instr parseWhile() {
        require("(");
        _in_loop = true;
        // header(predecessor is the loop)
        BB header = new BB("header");
        header._kind = BB.BBKind.LOOP_HEADER;
        _cBB.addSuccessor(header);
        _cBB = header;

        _scope.ctrl(_cBB);

        BB shared_exit = new BB("loop_exit"); // pre-made exit here as well
        shared_exit._kind = BB.BBKind.LOOP_EXIT;

        ScopeInstr head = _scope.keep();
        _scope.ctrl(_cBB);
        _scope = _scope.dup(true);

        var pred = require(parseExpression(), ")");

        IfInstr if_instr = (IfInstr)new IfInstr(_cBB, pred).<IfInstr>keep().peephole();
        _cBB.addInstr(if_instr);

        if_instr.create_bbs(_cBB);
        if_instr.unkeep();

        _cBB = if_instr.false_bb();


        shared_exit.addSuccessor(_cBB);
        _loopStack.push(new LoopContext(header, shared_exit, _cBB));

        var exit = _scope.dup();

        _cBB = if_instr.true_bb();
        parseStatement();

        _cBB.addSuccessor(header);
        _cBB._kind = BB.BBKind.BACK_EDGE;

        // kills redundant phi(s)
        head.endLoop(_scope);

        head.unkeep().kill();

        _cBB = if_instr.false_bb();
        _scope = exit;

        return if_instr;
    }
    private Instr parseIf() {
        require("(");
        var pred = require(parseExpression(), ")");

        IfInstr if_instr = (IfInstr)new IfInstr(_cBB, pred).<IfInstr>keep().peephole();
        _cBB.addInstr(if_instr);
        if_instr.create_bbs(_cBB);

        if_instr.unkeep();

        int ndefs = _scope.nIns();
        _scope.ctrl(_cBB);
        ScopeInstr fScope = _scope.dup();

        _cBB = if_instr.true_bb(); // set current BB to true branch
        parseStatement();
        ScopeInstr tScope = _scope;

        _scope = fScope;
        // same as ctrl(ifF) in Son context.

        _cBB = if_instr.false_bb(); // set current BB to false branch

        if (matchx("else")) {
            parseStatement();
            fScope = _scope;
        }

        if( tScope.nIns() != ndefs || fScope.nIns() != ndefs )
            throw error("Cannot define a new name on one arm of an if");

        _scope = tScope;

        // create merge point
        _cBB = new BB("merge");
        if_instr.addIfSuccessor(_cBB);

        // add Phi to current BB
        tScope.mergeScopes(fScope, _cBB);

        return if_instr;
    }


    private Instr parseDecl() {
        var name = requireId();
        require("=");
        var expr = require(parseExpression(), ";");
        if(_scope.define(name, expr) == null) throw error("Redefining name '" + name + "'");
        AssignmentInstr a = new AssignmentInstr(true, name, expr);
        _cBB.addInstr(a);
        return expr;
    }

    private Instr parseExpressionStatement() {
        var name = requireId();
        require("=");
        var expr = require(parseExpression(), ";");
        if( _scope.update(name, expr)==null )
            throw error("Undefined name '" + name + "'");
        AssignmentInstr a = new AssignmentInstr(false, name, expr);
        _cBB.addInstr(a);
        return expr;
    }

    private Instr parseReturn() {
        var expr = require(parseExpression(), ";");
        ReturnInstr ret = (ReturnInstr)new ReturnInstr(_cBB, expr).peephole();
        _cBB.addInstr(ret);

        // Todo: Better solution  - maybe need graph to collect return after all optimisations
        // Todo: see collect return for the current attempt
        if(!Instr._disablePeephole) {
            if(!_cBB.dead()) _returns.add(ret);
        } else {
            _returns.add(ret);
        }

        _cBB.addSuccessor(_exit);

        return ret;
    }

    private Instr parseBlock() {
        _scope.push();
        Instr n = null;
        while (!peek('}') && !_lexer.isEOF()) {
            Instr n0 = parseStatement();

            if(n0 != null) {n = n0;}
        }
        _scope.pop();
        return n;
    }

    private boolean peek(char ch) {
        return _lexer.peek(ch);
    }

    private Instr showGraph() {
        System.out.println(new GraphDot().generateDotOutput(_entry, _scope));
        return null;
    }

    private Instr parseExpression() {
        return parseComparison();
    }

    private Instr parseComparison() {
        var lhs = parseAddition();

        if (match("==")) {
            return new BoolInstr.EQ(_cBB, lhs, parseComparison()).peephole();
        }
        if (match("!=")) {
            return new NotInstr(_cBB, new BoolInstr.EQ(_cBB, lhs, parseComparison()).peephole()).peephole();
        }
        if (match("<=")) {
            return new BoolInstr.LE(_cBB, lhs, parseComparison()).peephole();
        }
        if (match("<")) {
            return new BoolInstr.LT(_cBB, lhs, parseComparison()).peephole();
        }
        if (match(">=")) {
            return new BoolInstr.LE(_cBB, parseComparison(), lhs).peephole();
        }
        if (match(">")) {
            return new BoolInstr.LT(_cBB, parseComparison(), lhs).peephole();
        }

        return lhs;
    }
    private Instr parseAddition() {
        var lhs = parseMultiplication();

        if (match("+")) return new AddInstr(_cBB, lhs, parseAddition()).peephole();
        if (match("-")) return new SubInstr(_cBB, lhs, parseAddition()).peephole();
        return lhs;
    }

    private Instr parseMultiplication() {
        var lhs = parseUnary();
        if (match("*")) {
            var instr = new MulInstr(_cBB, lhs, parseMultiplication()).peephole();
            _cBB.addInstr(instr);
            return instr;
        }

        if (match("/")) {
            var instr = new DivInstr(_cBB, lhs, parseMultiplication()).peephole();
            _cBB.addInstr(instr);
            return instr;
        }

        return lhs;
    }
    private Instr parseUnary() {
        if (match("-")) {
            var instr = new MinusInstr(_cBB, parseUnary()).peephole();
            _cBB.addInstr(instr);
            return instr;
        }
        return parsePrimary();
    }

    private Instr parsePrimary() {
        if (_lexer.isNumber()) {
            var instr = parseIntegerLiteral();
            return instr;
        }

        if (match("(")) {
            return require(parseExpression(), ")");
        }

        if (matchx("true")) {
            var instr = new ConstantInstr(TypeInteger.constant(1), _cBB).peephole();
            return instr;
        }

        if (matchx("false")) {
            var instr = new ConstantInstr(TypeInteger.constant(0), _cBB).peephole();
            return instr;
        }

        String name = _lexer.matchId();
        if (name == null) throw errorSyntax("an identifier or expression");

        Instr n = _scope.lookup(name);
        if(n!= null) return n;
        throw error("Undefined name '" + name + "'");
    }

    private ConstantInstr parseIntegerLiteral() {
        return (ConstantInstr) new ConstantInstr(_lexer.parseNumber(), _cBB).peephole();
    }

    private boolean matchx(String syntax) { return _lexer.matchx(syntax); }

    // Require an exact match
    private void require(String syntax) { require(null, syntax); }
    private Instr require(Instr n, String syntax) {
        if (match(syntax)) return n;
        throw errorSyntax(syntax);
    }

    private boolean match (String syntax) { return _lexer.match (syntax); }

    RuntimeException errorSyntax(String syntax) {
        return error("Syntax error, expected " + syntax + ": " + _lexer.getAnyNextToken());
    }

    static RuntimeException error(String errorMessage) {
        return new RuntimeException(errorMessage);
    }

    ////////////////////////////////////
    // Lexer components

    private static class Lexer {

        // Input buffer; an array of text bytes read from a file or a string
        private final byte[] _input;
        // Tracks current position in input buffer
        private int _position = 0;

        /**
         * Record the source text for lexing
         */
        public Lexer(String source) {
            this(source.getBytes());
        }

        /**
         * Direct from disk file source
         */
        public Lexer(byte[] buf) {
            _input = buf;
        }

        // True if at EOF
        private boolean isEOF() {
            return _position >= _input.length;
        }

        private char nextChar() {
            char ch = peek();
            _position++;
            return ch;
        }

        // True if a white space
        private boolean isWhiteSpace() {
            return peek() <= ' '; // Includes all the use space, tab, newline, CR
        }

        /**
         * Return the next non-white-space character
         */
        private void skipWhiteSpace() {
            while (isWhiteSpace()) _position++;
        }


        // Return true, if we find "syntax" after skipping white space; also
        // then advance the cursor past syntax.
        // Return false otherwise, and do not advance the cursor.
        boolean match(String syntax) {
            skipWhiteSpace();
            int len = syntax.length();
            if (_position + len > _input.length) return false;
            for (int i = 0; i < len; i++)
                if ((char) _input[_position + i] != syntax.charAt(i))
                    return false;
            _position += len;
            return true;
        }

        String matchId() {
            skipWhiteSpace();
            return isIdStart(peek()) ? parseId() : null;
        }
        boolean matchx(String syntax) {
            if( !match(syntax) ) return false;
            if( !isIdLetter(peek()) ) return true;
            _position -= syntax.length();
            return false;
        }

        // Used for errors
        String getAnyNextToken() {
            if (isEOF()) return "";
            if (isIdStart(peek())) return parseId();
            if (isNumber(peek())) return parseNumberString();
            if (isPunctuation(peek())) return parsePunctuation();
            return String.valueOf(peek());
        }

        boolean isNumber() {return isNumber(peek());}
        boolean isNumber(char ch) {return Character.isDigit(ch);}

        private Type parseNumber() {
            String snum = parseNumberString();
            if (snum.length() > 1 && snum.charAt(0) == '0')
                throw error("Syntax error: integer values cannot start with '0'");
            return TypeInteger.constant(Long.parseLong(snum));
        }

        private String parseNumberString() {
            int start = _position;
            while (isNumber(nextChar())) ;
            return new String(_input, start, --_position - start);
        }

        // First letter of an identifier
        private boolean isIdStart(char ch) {
            return Character.isAlphabetic(ch) || ch == '_';
        }

        // All characters of an identifier, e.g. "_x123"
        private boolean isIdLetter(char ch) {
            return Character.isLetterOrDigit(ch) || ch == '_';
        }

        private String parseId() {
            int start = _position;
            while (isIdLetter(nextChar())) ;
            return new String(_input, start, --_position - start);
        }

        private boolean isPunctuation(char ch) {
            return "=;[]<>(){}+-/*!".indexOf(ch) != -1;
        }

        private String parsePunctuation() {
            int start = _position;
            return new String(_input, start, 1);
        }
        // Peek next character, or report EOF
        private char peek() {
            return isEOF() ? Character.MAX_VALUE   // Special value that causes parsing to terminate
                    : (char) _input[_position];
        }

        private boolean peek(char ch) {
            skipWhiteSpace();
            return peek() == ch;
        }

   }


    }

