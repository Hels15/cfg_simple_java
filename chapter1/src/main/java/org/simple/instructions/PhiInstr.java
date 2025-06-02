package org.simple.instructions;

import org.simple.type.Type;

public class PhiInstr extends Instr{
    final String _label;

    public PhiInstr(String label, Instr... inputs) {
        super(inputs);
        _label = label;
    }
    @Override public String label() {return "Phi" + _label;}

    @Override
    StringBuilder _print1(StringBuilder sb) {
        sb.append("Phi(");
        for( Instr in : _inputs )
            in._print0(sb).append(",");
        sb.setLength(sb.length()-1);
        sb.append(")");
        return sb;
    }

    @Override
    public Type compute() {
        return Type.BOTTOM;
    }

    @Override
    public Instr idealize() {
        // Todo: only works with two inputs
        if(same_inputs()) return in(0);
        // Todo: extend it here
        return null;
    }

    private boolean same_inputs() {
        for(int i = 0; i < nIns(); i++) {
            if(in(1) != in(i)) {
                return false;
            }
        }
        return true;
    }


}
