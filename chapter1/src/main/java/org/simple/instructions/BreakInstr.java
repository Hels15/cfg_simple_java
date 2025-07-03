package org.simple.instructions;

import org.simple.bbs.BB;
import org.simple.type.Type;

import java.util.BitSet;

public class BreakInstr extends Instr {

    // `c` represents the current basic block; unused here but passed for interface consistency.
    public BreakInstr(BB c) {
        super();
        _bb = c;
    }

    @Override public boolean debug() {
        return true;
    }

    @Override
    public String label() {
        return "Break";
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