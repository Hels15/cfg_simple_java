package org.simple.instructions;

import org.simple.bbs.BB;
import org.simple.type.Type;

public class PhiInstr extends Instr{
    final String _label;

    public PhiInstr(BB cb, String label, Instr... inputs) {
        super(inputs);
        _label = label;
        _bb = cb;
    }
    @Override public String label() {return "Phi" + _label;}

    @Override
    StringBuilder _print1(StringBuilder sb) {
        sb.append("Phi(");
        assert _bb != null;
        sb.append("bb").append(_bb._nid);
        sb.append(",");
        for( Instr in : _inputs )
            in._print0(sb).append(",");
        sb.setLength(sb.length()-1);
        sb.append(")");
        return sb;
    }

    @Override
    public Type compute() {
        Type t = Type.TOP;
        for(int i = 0; i < nIns(); i++)
            t = t.meet(in(i)._type);
        return t;
    }

    @Override
    public Instr idealize() {
        if(same_inputs()) return in(0);

        Instr op = in(0);

        if(!_bb.dead()) {
            // invariant corresponding pred - corresponding index in phi
            if(_bb._preds.getFirst().dead()) return in(nIns()-1);
            if(_bb._preds.getLast().dead()) return in(0);
        }

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

            Instr phi_lhs = new PhiInstr(_bb, _label,lhss).peephole();
            Instr phi_rhs = new PhiInstr(_bb, _label,rhss).peephole();
            return op.copy(_bb, phi_lhs,phi_rhs);
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
