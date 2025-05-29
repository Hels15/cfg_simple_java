package org.simple;

import org.simple.bbs.BB;
import org.simple.bbs.EntryBB;
import org.simple.bbs.ExitBB;
import org.simple.instructions.*;
import org.simple.type.Type;
import org.simple.type.TypeInteger;


public class Parser {
    private final Lexer _lexer;

    public Parser(String source) {
        _lexer = new Lexer(source);
    }
    public BB parse() {
        EntryBB entry = new EntryBB();

        BB mainBB = new BB();
        var ret = parseStatement();

        entry.addSuccessor(mainBB);
        mainBB.addInstr(ret);

        ExitBB exitBB = new ExitBB();
        mainBB.addSuccessor(exitBB);

        return entry;
    }

    private Instr parseStatement() {
        if (matchx("return")) return parseReturn();
        throw errorSyntax("a statement");
    }

    private ReturnInstr parseReturn() {
        // Todo: parse down expression here
        var expr = require(parseExpression(), ";");
        return new ReturnInstr(expr);
    }
    private Instr parseExpression() {
        return parseAddition();
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
        throw errorSyntax("integer literal");
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


   }


    }

