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
            return i0.meet(i1);
        }
        return Type.BOTTOM;
    }
    @Override public Instr idealize() {

        Instr lhs = in(0);
        Instr rhs = in(1);
        Type t1 = lhs._type;
        Type t2 = rhs._type;

        assert !(t1.isConstant() && t2.isConstant());

        if(t2 instanceof TypeInteger i && i.value() == 0) return lhs;

        if(lhs == rhs) {
            return new MulInstr(lhs, new ConstantInstr(TypeInteger.constant(2), _bb).peephole());
        }
        if(!(lhs instanceof AddInstr) && rhs instanceof AddInstr) {
            return swap12();
        }

        if( rhs instanceof AddInstr add )
            return new AddInstr(new AddInstr(lhs,add.in(0)).peephole(), add.in(1));

        if( !(lhs instanceof AddInstr) )
            return spline_cmp(lhs,rhs) ? swap12() : null;

        if( lhs.in(1)._type.isConstant() && t2.isConstant() )
            return new AddInstr(lhs.in(0),new AddInstr(lhs.in(1),rhs).peephole());

        if( lhs.in(1) instanceof PhiInstr phi && phi.allCons() &&
                // Do we have ((x + (phi cons)) + con) ?
                // Do we have ((x + (phi cons)) + (phi cons)) ?
                // Push constant up through the phi: x + (phi con0+con0 con1+con1...)

                // Note that this is the exact reverse of Phi pulling a common op
                // down to reduce total op-count.  We don't get in an endless push-
                // up push-down peephole cycle because the constants all fold first.
                (t2.isConstant() || (rhs instanceof PhiInstr && phi._bb == rhs._bb && rhs.allCons()) ) ) {
            Instr[] ns = new Instr[phi.nIns()];
            // Push constant up through the phi: x + (phi con0+con0 con1+con1...)
            for( int i=0; i<ns.length; i++ )
                ns[i] = new AddInstr(phi.in(i),t2.isConstant() ? rhs : rhs.in(i)).peephole();
            String label = phi._label + (rhs instanceof PhiInstr rphi ? rphi._label : "");
            return new AddInstr(lhs.in(0),new PhiInstr(label,ns).peephole());
        }

        if( spline_cmp(lhs.in(1),rhs) )
            return new AddInstr(new AddInstr(lhs.in(0),rhs).peephole(),lhs.in(1));

        return null;
    }

    static boolean spline_cmp(Instr hi, Instr lo) {
        if(lo._type.isConstant()) return false;
        if(hi._type.isConstant()) return true;

        if(lo instanceof PhiInstr && lo.allCons()) return false;
        if(hi instanceof PhiInstr && hi.allCons()) return true;

        if( lo instanceof PhiInstr && !(hi instanceof PhiInstr) ) return true;
        if( hi instanceof PhiInstr && !(lo instanceof PhiInstr) ) return false;

        return lo._nid > hi._nid;
    }

    @Override
    StringBuilder _print1(StringBuilder sb) {
        in(0)._print0(sb.append("("));
        in(1)._print0(sb.append("+"));
        return sb.append(")");
    }

    @Override Instr copy(Instr lhs, Instr rhs) {return new AddInstr(lhs, rhs);}

}
