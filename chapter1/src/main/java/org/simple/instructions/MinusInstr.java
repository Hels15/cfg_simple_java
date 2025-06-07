package org.simple.instructions;

import org.simple.bbs.BB;
import org.simple.type.Type;
import org.simple.type.TypeInteger;

public class MinusInstr extends Instr{
    public MinusInstr(BB c, Instr in) {super(in); _bb = c;}
    @Override public String label() {
        return "Minus";
    }
    @Override public Type compute() {
        if (in(0)._type instanceof TypeInteger i0)
            return i0.isConstant() ? TypeInteger.constant(-i0.value()) : i0;
        return Type.BOTTOM;
    }
    @Override public Instr idealize() {return null;}


    @Override
    StringBuilder _print1(StringBuilder sb) {
        in(0)._print0(sb.append("(-"));
        return sb.append(")");
    }

}
