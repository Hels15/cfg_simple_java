package org.simple.instructions;


import org.simple.bbs.BB;
import org.simple.type.Type;
import org.simple.type.TypeInteger;

import java.util.BitSet;

abstract public class BoolInstr extends Instr{
    public BoolInstr(BB c, Instr lhs, Instr rhs) {
        super(lhs, rhs);
        _bb = c;
    }
    abstract String op();       // String opcode name
    abstract String gop();      // graphviz op

    @Override
    public String label() { return getClass().getSimpleName(); }


    @Override
    StringBuilder _print1(StringBuilder sb, BitSet visited) {
        in(0)._print0(sb.append("("), visited);
        in(1)._print0(sb.append(gop()).append(this instanceof EQ ? "" : ";"),visited);
        return sb.append(")");
    }

    @Override
    public Type compute() {
        if( in(0)._type instanceof TypeInteger i0 &&
                in(1)._type instanceof TypeInteger i1 ) {
            if (i0.isConstant() && i1.isConstant())
                return TypeInteger.constant(doOp(i0.value(), i1.value()) ? 1 : 0);
            return i0.meet(i1);
        }
        return Type.BOTTOM;
    }

    abstract boolean doOp(long lhs, long rhs);

    @Override
    public Instr idealize() {
        // Compare of same
        if( in(0)==in(1) )
            return new ConstantInstr(TypeInteger.constant(doOp(3,3)?1:0), _bb);

        Instr phicon = AddInstr.phiCon(_bb, this, false);
        if(phicon != null) return phicon;

        return null;
    }

    public static class EQ extends BoolInstr { public EQ(BB c, Instr lhs, Instr rhs) { super(c, lhs,rhs); } String op() { return "=="; } String gop() {return "=";   } boolean doOp(long lhs, long rhs) { return lhs == rhs; } Instr copy(BB c, Instr lhs, Instr rhs) {return new EQ(c, lhs, rhs);}}
    public static class LT extends BoolInstr { public LT(BB c, Instr lhs, Instr rhs) { super(c, lhs,rhs); } String op() { return "<" ; } String gop() {return "&lt"; } boolean doOp(long lhs, long rhs) { return lhs <  rhs; } Instr copy(BB c, Instr lhs, Instr rhs) {return new LT(c, lhs, rhs);}}
    public static class LE extends BoolInstr { public LE(BB c, Instr lhs, Instr rhs) { super(c, lhs,rhs); } String op() { return "<="; } String gop() {return "&le"; }boolean doOp(long lhs, long rhs) { return lhs <= rhs; } Instr copy(BB c, Instr lhs, Instr rhs)  {return new LE(c, lhs, rhs);}}

}
