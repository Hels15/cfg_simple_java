package org.simple.instructions;

import org.simple.type.Type;
import org.simple.type.TypeInteger;

public class MulInstr extends Instr {
    public String label() {return "Mul";}
    public MulInstr(Instr lhs, Instr rhs) {super(lhs, rhs);}

    @Override public Type compute() {
        if (in(0)._type instanceof TypeInteger i0 &&
                in(1)._type instanceof TypeInteger i1) {
            if (i0.isConstant() && i1.isConstant())
                return TypeInteger.constant(i0.value()*i1.value());
            return i0.meet(i1);
        }
        return Type.BOTTOM;
    }

    @Override
    StringBuilder _print1(StringBuilder sb) {
        in(0)._print0(sb.append("("));
        in(1)._print0(sb.append("*"));
        return sb.append(")");
    }

    @Override public Instr idealize() {
        Instr lhs = in(0);
        Instr rhs = in(1);

        Type t1 = lhs._type;
        Type t2 = rhs._type;

        if(t2.isConstant() && t2 instanceof TypeInteger i && i.value() == 1) return lhs;

        if(t1.isConstant() && !t2.isConstant()) return swap12();

        return null;
    }
    @Override Instr copy(Instr lhs, Instr rhs) {
        return new MulInstr(lhs, rhs);
    }

}
