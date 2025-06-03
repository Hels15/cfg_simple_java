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
        if(same_inputs()) return in(0);

        Instr op = in(0);

        // Pull "down" a common data op.  One less op in the world.  One more
        // Phi, but Phis do not make code.
        //   Phi(op(A,B),op(Q,R),op(X,Y)) becomes
        //     op(Phi(A,Q,X), Phi(B,R,Y)).
        if(op.nIns() == 2 && same_op()) {
            Instr[] lhss = new Instr[nIns()];
            Instr[] rhss = new Instr[nIns()];
            for( int i=0; i<nIns(); i++ ) {
                lhss[i] = in(i).in(0);
                rhss[i] = in(i).in(1);
            }
            Instr phi_lhs = new PhiInstr(_label,lhss).peephole();
            Instr phi_rhs = new PhiInstr(_label,rhss).peephole();
            return op.copy(phi_lhs,phi_rhs);
        }

        return null;
    }

    private boolean same_inputs() {
        for(int i = 1; i < nIns(); i++) {
            if(in(0) != in(i)) {
                return false;
            }
        }
        return true;
    }

    private boolean same_op() {
        for( int i=1; i<nIns(); i++ )
            if( in(0).getClass() != in(i).getClass() )
                return false;
        return true;
    }


}
