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

    @Override public boolean debug() {
        return true;
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
    boolean eq(Instr i) {
       GotoInstr gotoi = (GotoInstr)i;
       // compare bbs based on ID
       return gotoi._bb._succs.getFirst()._nid == _bb._succs.getFirst()._nid;
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
