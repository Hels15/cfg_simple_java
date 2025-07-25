package org.simple.instructions;

import org.simple.bbs.BB;
import org.simple.type.Type;
import org.simple.type.TypeInteger;

import java.util.BitSet;

public class MulInstr extends Instr {
    public String label() {return "Mul";}
    public MulInstr(BB c, Instr lhs, Instr rhs) {super(lhs, rhs); _bb = c;}

    @Override public Type compute() {
        if (in(0)._type instanceof TypeInteger i0 &&
                in(1)._type instanceof TypeInteger i1) {
            if (i0.isConstant() && i1.isConstant())
                return TypeInteger.constant(i0.value()*i1.value());
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
        in(1)._print0(sb.append("*"), visited);
        return sb.append(")");
    }

    @Override public Instr idealize() {
        Instr lhs = in(0);
        Instr rhs = in(1);

        Type t1 = lhs._type;
        Type t2 = rhs._type;

        if(t2.isConstant() && t2 instanceof TypeInteger i && i.value() == 1) return lhs;

        if(t1.isConstant() && !t2.isConstant()) return swap12();

        Instr phicon = AddInstr.phiCon(_bb, this, true);
        if(phicon != null) return phicon;
        return null;
    }
    @Override Instr copy(BB c, Instr lhs, Instr rhs) {
        return new MulInstr(c, lhs, rhs);
    }

}
