package org.simple.instructions;

import org.simple.bbs.BB;
import org.simple.type.Type;
import org.simple.type.TypeInteger;

import java.util.BitSet;

public class DivInstr extends Instr{
    public DivInstr(BB c, Instr lhs, Instr rhs) {super(lhs, rhs); _bb = c;}
    public String label() {
        return "Div";
    }
    @Override public Type compute() {
        if (in(0)._type instanceof TypeInteger i0 &&
                in(1)._type instanceof TypeInteger i1) {
            if (i0.isConstant() && i1.isConstant())
                return i1.value() == 0
                        ? TypeInteger.ZERO
                        : TypeInteger.constant(i0.value()/i1.value());
            return i0.meet(i1);
        }
        return Type.BOTTOM;
    }

    @Override public boolean graphVis() {
        return false;
    }

    @Override
    StringBuilder _print1(StringBuilder sb, BitSet visited) {
        in(0)._print0(sb.append("("), visited);
        in(1)._print0(sb.append("/"), visited);
        return sb.append(")");
    }

    @Override public Instr idealize() {
        if(in(1)._type.isConstant() && in(1)._type instanceof TypeInteger i && i.value() == 1) return in(0);
        return null;
    }
    @Override Instr copy(BB c, Instr lhs, Instr rhs) {return new AddInstr(c, lhs, rhs);}
}
