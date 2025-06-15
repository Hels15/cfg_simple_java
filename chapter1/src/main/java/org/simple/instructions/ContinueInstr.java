package org.simple.instructions;

import org.simple.bbs.BB;
import org.simple.type.Type;

import java.util.BitSet;

public class ContinueInstr extends Instr{
    public ContinueInstr(BB c) {
        super();
    }

    @Override
    public String label() {
        return "Continue";
    }

    @Override
    StringBuilder _print1(StringBuilder sb, BitSet visited) {
        return sb.append(label());
    }

    @Override
    public Type compute() {
        return Type.BOTTOM;
    }

    @Override
    public Instr idealize() {
        return null;
    }
}