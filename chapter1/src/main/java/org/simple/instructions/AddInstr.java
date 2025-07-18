package org.simple.instructions;

import org.simple.bbs.BB;
import org.simple.type.Type;
import org.simple.type.TypeInteger;

import java.util.BitSet;

public class AddInstr extends Instr{
    public AddInstr(BB c, Instr lhs, Instr rhs) {super(lhs, rhs); _bb = c;}
    @Override public String label() {
        return "Add";
    }
    @Override public Type compute() {
        if( in(0)._type instanceof TypeInteger i0 &&
                in(1)._type instanceof TypeInteger i1 ) {
            if (i0.isConstant() && i1.isConstant())
                return TypeInteger.constant(i0.value()+i1.value());
            return i0.meet(i1);
        }
        return Type.BOTTOM;
    }

    @Override
    public boolean graphVis() {
       return false;
    }

    @Override public Instr idealize() {

        Instr lhs = in(0);
        Instr rhs = in(1);
        Type t1 = lhs._type;
        Type t2 = rhs._type;

        assert !(t1.isConstant() && t2.isConstant());

        if(t2 instanceof TypeInteger i && i.value() == 0) return lhs;

        if(lhs == rhs) {
            return new MulInstr(_bb, lhs, new ConstantInstr(TypeInteger.constant(2), _bb).peephole());
        }
        if(!(lhs instanceof AddInstr) && rhs instanceof AddInstr) {
            return swap12();
        }

        if( rhs instanceof AddInstr add )
            return new AddInstr(_bb, new AddInstr(_bb, lhs,add.in(0)).peephole(), add.in(1));

        if( !(lhs instanceof AddInstr) )
            return spline_cmp(lhs,rhs, this) ? swap12() : phiCon(_bb,this, true);

        if( lhs.in(1).addDep(this)._type.isConstant() && t2.isConstant() )
            return new AddInstr(_bb, lhs.in(0),new AddInstr(_bb, lhs.in(1),rhs).peephole());

        Instr phicon = phiCon(_bb, this, true);
        if(phicon != null) return phicon;
        return null;
    }

    static boolean spline_cmp(Instr hi, Instr lo, Instr dep) {
        if(lo._type.isConstant()) return false;
        if(hi._type.isConstant()) return true;

        if(lo instanceof PhiInstr && lo.allCons(dep)) return false;
        if(hi instanceof PhiInstr && hi.allCons(dep)) return true;

        if( lo instanceof PhiInstr && !(hi instanceof PhiInstr) ) return true;
        if( hi instanceof PhiInstr && !(lo instanceof PhiInstr) ) return false;

        return lo._nid > hi._nid;
    }

    static Instr phiCon(BB c, Instr op, boolean rotate) {
        Instr lhs = op.in(0);
        Instr rhs = op.in(1);

        PhiInstr lphi = pcon(lhs, op);
        if(rotate && lphi == null && lhs.nIns() > 1) {
            if(lhs.getClass() != op.getClass()) return null;
            lphi  = pcon(lhs.in(1), op);
        }

        if( lphi==null ) return null;

        // RHS is a constant or a Phi of constants
        if( !(rhs instanceof ConstantInstr con) && pcon(rhs, op)==null )
            return null;

        // If both are Phis, must be same Region
        if( rhs instanceof PhiInstr && lphi._bb != rhs._bb )
            return null;

        Instr[] ns = new Instr[lphi.nIns()];
        // Push constant up through the phi: x + (phi con0+con0 con1+con1...)
        for( int i=0; i<ns.length; i++ )
            ns[i] = op.copy(c, lphi.in(i), rhs instanceof PhiInstr ? rhs.in(i) : rhs).peephole();
        String label = lphi._label + (rhs instanceof PhiInstr rphi ? rphi._label : "");
        Instr phi = new PhiInstr(c, label,ns).peephole();
        // Rotate needs another op, otherwise just the phi
        return lhs==lphi ? phi : op.copy(c,lhs.in(0),phi);
    }

    static PhiInstr pcon(Instr op, Instr dep) {
        return op instanceof PhiInstr phi && phi.allCons(dep) ? phi: null;
    }
    @Override
    StringBuilder _print1(StringBuilder sb, BitSet visited) {
        in(0)._print0(sb.append("("), visited);
        in(1)._print0(sb.append("+"), visited);
        return sb.append(")");
    }

    @Override Instr copy(BB c, Instr lhs, Instr rhs) {return new AddInstr(c, lhs, rhs);}

}
