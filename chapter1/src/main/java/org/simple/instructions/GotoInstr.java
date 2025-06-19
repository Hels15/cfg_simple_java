package org.simple.instructions;

import org.simple.bbs.BB;
import org.simple.type.Type;

import java.util.BitSet;

public class GotoInstr extends Instr {

    // `c` represents the current basic block; unused here but passed for interface consistency.
    public GotoInstr(BB c) {
      super();
      _bb = c;
    }

    @Override
    public String label() {
        return "Goto";
    }

    @Override
    StringBuilder _print1(StringBuilder sb, BitSet visited) {
        return sb.append("Goto: bb").append(_bb._succs.getFirst()._nid);
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
