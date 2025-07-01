package org.simple.instructions;

import org.simple.bbs.BB;
import org.simple.type.Type;
import org.simple.type.TypeInteger;

import java.util.BitSet;

public class NotInstr extends Instr{
    public NotInstr(BB c, Instr in) {super(null, in); _bb = c; }

    @Override public String label() { return "Not"; }

    @Override
    StringBuilder _print1(StringBuilder sb, BitSet visited) {
        in(1)._print0(sb.append("(!"), visited);
        return sb.append(")");
    }

    @Override
    public Type compute() {
        if( in(1)._type instanceof TypeInteger i0 )
            return i0.isConstant() ? TypeInteger.constant(i0.value()==0 ? 1 : 0) : i0;
        return Type.BOTTOM;
    }

    @Override public boolean graphVis() {
        return false;
    }

    @Override
    public Instr idealize() { return null; }
}
