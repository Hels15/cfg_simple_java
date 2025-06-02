package org.simple.instructions;

import org.simple.type.Type;

public class IfInstr extends Instr{
    public IfInstr(Instr pred) {
        super(pred);
    }
    @Override
    public String label() {
        return "If";
    }
    @Override
    StringBuilder _print1(StringBuilder sb) {
        sb.append("if (");
        return in(0)._print0(sb).append(" )");
    }

    public Instr pred() {return in(0);}

    @Override
    public Type compute() {
        return Type.BOTTOM;
    }

    @Override
    public Instr idealize() {
        return null;
    }

}
