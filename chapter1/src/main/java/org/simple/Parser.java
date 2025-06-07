package org.simple;

import org.simple.bbs.BB;
import org.simple.bbs.EntryBB;
import org.simple.bbs.ExitBB;
import org.simple.instructions.*;
import org.simple.type.Type;
import org.simple.type.TypeInteger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


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
    }};

    public ArrayList<ReturnInstr> _returns;
    public Parser(String source) {
        _lexer = new Lexer(source);
        _scope = new ScopeInstr();
        _entry = new EntryBB();
        _cBB   = new BB(); // current basic block is the entry block
        _exit  = new ExitBB();
        _returns  = new ArrayList<>();
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

        // Run pass manager to clean up dead bbs
        if(!Instr._disablePasses) {
             _pass.bb_dead(_entry);
            _pass.combine(_entry);
        }
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
        else if(matchx("#showGraph")) return require(showGraph(), ";");
        else if(matchx(";")) return null;
        else return parseExpressionStatement();
    }

    private Instr parseIf() {
        require("(");
        var pred = require(parseExpression(), ")");

        IfInstr if_instr = (IfInstr)new IfInstr(_cBB, pred).<IfInstr>keep().peephole();
        _cBB.addInstr(if_instr);
        if_instr.create_bbs(_cBB);

        int ndefs = _scope.nIns();
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
        _cBB = new BB();
        if_instr.true_bb().addSuccessor(_cBB);
        if_instr.false_bb().addSuccessor(_cBB);

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
            if(_cBB._type != Type.XCONTROL) _returns.add(ret);
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
            var instr = new BoolInstr.EQ(_cBB, lhs, parseComparison()).peephole();
            return instr;
        }
        if (match("!=")) {
            var instr = new NotInstr(_cBB, new BoolInstr.EQ(_cBB, lhs, parseComparison()).peephole()).peephole();
            return instr;
        }
        if (match("<=")) {
            var instr = new BoolInstr.LE(_cBB, lhs, parseComparison()).peephole();
            return instr;
        }
        if (match("<")) {
            var instr = new BoolInstr.LT(_cBB, lhs, parseComparison()).peephole();
            return instr;
        }
        if (match(">=")) {
            var instr = new BoolInstr.LE(_cBB, parseComparison(), lhs).peephole();
            return instr;
        }
        if (match(">")) {
            var instr = new BoolInstr.LT(_cBB, parseComparison(), lhs).peephole();
            return instr;
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

