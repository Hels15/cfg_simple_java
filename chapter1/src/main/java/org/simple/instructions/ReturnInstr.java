package org.simple.instructions;


import org.simple.bbs.BB;
import org.simple.type.Type;

import java.util.BitSet;

public class ReturnInstr extends Instr {
    public ReturnInstr(BB c, Instr data) {super(data); _bb = c;}
    @Override
    public String label() {
        return "Return";
    }

    @Override
    public Type compute() {
        return Type.BOTTOM;
    }

    public Instr expr() { return in(0); }

    @Override
    StringBuilder _print1(StringBuilder sb, BitSet visited) {
        return expr()._print0(sb.append("return "), visited).append(";");
    }

    @Override
    public Instr idealize() { return null; }
}
