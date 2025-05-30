package org.simple.instructions;

import org.simple.type.Type;
import org.simple.type.TypeInteger;

public class SubInstr extends Instr{
    public SubInstr(Instr lhs, Instr rhs) {super(lhs, rhs);}
    public @Override String label() {
        return "Sub";
    }

    @Override
    public Type compute() {
        if(in(0) == in(1)) return TypeInteger.ZERO;
        if (in(0)._type instanceof TypeInteger i0 &&
                in(1)._type instanceof TypeInteger i1) {
            if (i0.isConstant() && i1.isConstant())
                return TypeInteger.constant(i0.value()-i1.value());
            return i0.meet(i1);
        }
        return Type.BOTTOM;
    }
    @Override
    StringBuilder _print1(StringBuilder sb) {
        in(0)._print0(sb.append("("));
        in(1)._print0(sb.append("-"));
        return sb.append(")");
    }

    @Override
    public Instr idealize() { return null; }
}