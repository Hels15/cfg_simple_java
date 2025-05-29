package org.simple.instructions;

import org.simple.type.Type;
import org.simple.type.TypeInteger;

public class AddInstr extends Instr{
    public AddInstr(Instr lhs, Instr rhs) {super(lhs, rhs);}
    @Override public String label() {
        return "Add";
    }
    @Override public Type compute() {
        if( in(0)._type instanceof TypeInteger i0 &&
                in(1)._type instanceof TypeInteger i1 ) {
            if (i0.isConstant() && i1.isConstant())
                return TypeInteger.constant(i0.value()+i1.value());
        }
        return Type.BOTTOM;
    }
    @Override public Instr idealize() {

        return null;
    }

    @Override
    StringBuilder _print1(StringBuilder sb) {
        in(0)._print0(sb.append("("));
        in(1)._print0(sb.append("+"));
        return sb.append(")");
    }

}
