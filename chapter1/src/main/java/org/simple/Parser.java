package org.simple;

import org.simple.bbs.BB;
import org.simple.bbs.EntryBB;
import org.simple.bbs.ExitBB;
import org.simple.instructions.*;
import org.simple.type.Type;
import org.simple.type.TypeInteger;

import java.util.HashSet;


public class Parser {
    private final Lexer _lexer;

    public static EntryBB _entry;
    public static BB _cBB; // represents the current basic block where instructions are inserted

    public static ExitBB _exit;
    public static ScopeInstr _scope;

    private final HashSet<String> KEYWORDS = new HashSet<>(){{
        add("int");
        add("return");
    }};

    public Parser(String source) {
        _lexer = new Lexer(source);
        _scope = new ScopeInstr();
        _entry = new EntryBB();
        _cBB   = new BB(); // current basic block is the entry block
        _exit  = new ExitBB();
    }
    public Instr parse() {return parse(false);}
    public Instr parse(boolean show, TypeInteger arg) {

        _scope.push();
        _entry.addSuccessor(_cBB);
        // For now no jumping instructions

        _scope.define(ScopeInstr.ARG0, new ConstantInstr(arg, ScopeInstr.ARG0).peephole());

        var ret = parseBlock();
        // add it end the end when the graph is complete
        _cBB.addSuccessor(_exit);
        _scope.pop();

        if(!_lexer.isEOF()) throw error("Syntax error, unexpected " + _lexer.getAnyNextToken());
        if(show) showGraph();

        return ret;
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

        IfInstr if_instr = (IfInstr)new IfInstr(pred).<IfInstr>keep().peephole();
        _cBB.addInstr(if_instr);

        // true
        BB trueBB = new BB();
        _cBB.addSuccessor(trueBB);
        // false
        BB falseBB = new BB();
        _cBB.addSuccessor(falseBB);

        int ndefs = _scope.nIns();
        ScopeInstr fScope = _scope.dup();

        _cBB = trueBB; // set current BB to true branch
        parseStatement();
        ScopeInstr tScope = _scope;

        _scope = fScope;
        showGraph();
        // same as ctrl(ifF) in Son context.

        _cBB = falseBB; // set current BB to false branch

        if (matchx("else")) {
            parseStatement();
            fScope = _scope;
        }

        if( tScope.nIns() != ndefs || fScope.nIns() != ndefs )
            throw error("Cannot define a new name on one arm of an if");

        _scope = tScope;

        // create merge point
        _cBB = new BB();
        trueBB.addSuccessor(_cBB);
        falseBB.addSuccessor(_cBB);

        tScope.mergeScopes(fScope);

        // add Phi to current BB from symbol table
        for(int i = 0; i < _scope.nIns(); i++) {
            if(_scope.in(i) != null && _scope.in(i) instanceof PhiInstr) {
                _cBB.addInstr(_scope.in(i));
            }
        }

        return if_instr;
    }


    private Instr parseDecl() {
        var name = requireId();
        require("=");
        var expr = require(parseExpression(), ";");
        if(_scope.define(name, expr) == null) throw error("Redefining name '" + name + "'");
        return expr;
    }

    private Instr parseExpressionStatement() {
        var name = requireId();
        require("=");
        var expr = require(parseExpression(), ";");
        if( _scope.update(name, expr)==null )
            throw error("Undefined name '" + name + "'");
        return expr;
    }
    private Instr parseReturn() {
        var expr = require(parseExpression(), ";");
        return new ReturnInstr(expr).peephole();
    }

    private Instr parseBlock() {
        _scope.push();
        Instr n = null;
        while(!peek('}') && !_lexer.isEOF()) {
            Instr n0 = parseStatement();
            if(n0 instanceof ReturnInstr) {
                System.out.print("Here");
            }
            if(n0 instanceof ConstantInstr) {
                // already visualised in Scope
                continue;
            }

            if(n0 != null) {_cBB.addInstr(n0); n = n0;}
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
        if (match("==")) return new BoolInstr.EQ(lhs, parseComparison()).peephole();
        if (match("!=")) return new NotInstr(new BoolInstr.EQ(lhs, parseComparison()).peephole()).peephole();
        if (match("<=")) return new BoolInstr.LE(lhs, parseComparison()).peephole();
        if (match("<" )) return new BoolInstr.LT(lhs, parseComparison()).peephole();
        if (match(">=")) return new BoolInstr.LE(parseComparison(), lhs).peephole();
        if (match(">" )) return new BoolInstr.LT(parseComparison(), lhs).peephole();
        return lhs;
    }
    private Instr parseAddition() {
        var lhs = parseMultiplication();

        if (match("+")) return new AddInstr(lhs, parseAddition()).peephole();
        if (match("-")) return new SubInstr(lhs, parseAddition()).peephole();
        return lhs;
    }

    private Instr parseMultiplication() {
        var lhs = parseUnary();
        if (match("*")) return new MulInstr(lhs, parseMultiplication()).peephole();
        if (match("/")) return new DivInstr(lhs, parseMultiplication()).peephole();
        return lhs;
    }
    private Instr parseUnary() {
        if (match("-")) return new MinusInstr(parseUnary()).peephole();
        return parsePrimary();
    }

    private Instr parsePrimary() {
        if( _lexer.isNumber() ) return parseIntegerLiteral();
        if( match("(") ) return require(parseExpression(), ")");

        if(matchx("true")) return new ConstantInstr(TypeInteger.constant(1)).peephole();
        if(matchx("false")) return new ConstantInstr(TypeInteger.constant(0)).peephole();

        String name = _lexer.matchId();
        if(name == null) throw errorSyntax("an identifier or expression");
        Instr n = _scope.lookup(name);
        if(n!= null) return n;
        throw error("Undefined name '" + name + "'");
    }

    private ConstantInstr parseIntegerLiteral() {
        return (ConstantInstr) new ConstantInstr(_lexer.parseNumber()).peephole();
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

