package org.simple.instructions;

import org.simple.bbs.BB;
import org.simple.type.Type;

import java.util.BitSet;

public class PhiInstr extends Instr{
    final String _label;

    public PhiInstr(BB cb, String label, Instr... inputs) {
        super(inputs);
        _label = label;
        _bb = cb;
    }
    @Override public String label() {return "Phi_" + _label;}

    @Override
    StringBuilder _print1(StringBuilder sb, BitSet visited) {
        // Still in progress
        if(inProgress()) {
            sb.append("Z");
        }
        sb.append("Phi(");
        assert _bb != null;
        sb.append("bb").append(_bb._nid);
        if(!_inputs.isEmpty()) sb.append(",");
        for( Instr in : _inputs ) {
            if (in == null) sb.append("___");
            else in._print0(sb, visited);
            if( in != _inputs.getLast()) sb.append(",");
        }
        sb.append(")");
        return sb;
    }

    @Override
    public Type compute() {
        // In progress
        if(inProgress()) return Type.BOTTOM;
        Type t = Type.TOP;

        for(int i = 0; i < nIns(); i++) {
            t = t.meet(in(i)._type);
        }

        return t;
    }

    @Override
    boolean allCons() {
        if(inProgress()) return false;
        return super.allCons();
    }

    // Todo: use this everywhere
    boolean inProgress() {
        return in(nIns() -1 ) == null;
    }

    @Override
    public Instr idealize() {
        if(same_inputs()) return in(0);

        Instr op = in(0);

        // Single unique input when a bb is dead is handled in the passManager
        // When an input is same as the phi itself we handle it here

        Instr live = singleUniqueInput();
        if(live != null) return live;

//       Handled in pass-manager(DCE)
//        if(!_bb.dead()) {
//            // invariant corresponding pred - corresponding index in phi
//            if(_bb._preds.getFirst().dead()) return in(nIns()-1);
//            if(_bb._preds.getLast().dead()) return in(0);
//        }
        // Pull "down" a common data op.  One less op in the world.  One more
        // Phi, but Phis do not make code.
        //   Phi(op(A,B),op(Q,R),op(X,Y)) becomes
        //     op(Phi(A,Q,X), Phi(B,R,Y)).

        // Only do it with pure nodes? in(0) == null
        if(op.nIns() == 2 && pure() && !inProgress() && same_op()) {
            Instr[] lhss = new Instr[nIns()];
            Instr[] rhss = new Instr[nIns()];
            for( int i=0; i<nIns(); i++ ) {
                lhss[i] = in(i).in(0);
                rhss[i] = in(i).in(1);
            }

            Instr phi_lhs = new PhiInstr(_bb, _label,lhss).peephole();

            // here phi_rhs id is 33 and fails in the compute call
            Instr phi_rhs = new PhiInstr(_bb, _label,rhss).peephole();

            return op.copy(_bb, phi_lhs,phi_rhs);
        }

        return null;
    }

    private Instr singleUniqueInput() {
        Instr live = null;
        for(int i = 0; i < nIns(); i++) {
        if(in(i) != this && _bb._preds.get(i)._type != Type.XCONTROL) {
            if(live == null || live == in(i)) live = in(i);
            else return null;
        }
        }
        return live;
    }
    public @Override boolean pure() {
        return false;
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
