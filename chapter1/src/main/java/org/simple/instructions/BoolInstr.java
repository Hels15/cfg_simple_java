package org.simple.instructions;


import org.simple.type.Type;
import org.simple.type.TypeInteger;

abstract public class BoolInstr extends Instr{
    public BoolInstr(Instr lhs, Instr rhs) {
        super(lhs, rhs);
    }
    abstract String op();       // String opcode name

    @Override
    public String label() { return getClass().getSimpleName(); }


    @Override
    StringBuilder _print1(StringBuilder sb) {
        in(0)._print0(sb.append("("));
        in(1)._print0(sb.append(op()));
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
            return new ConstantInstr(TypeInteger.constant(doOp(3,3)?1:0));

        return null;
    }

    public static class EQ extends BoolInstr { public EQ(Instr lhs, Instr rhs) { super(lhs,rhs); } String op() { return "=="; } boolean doOp(long lhs, long rhs) { return lhs == rhs; } }
    public static class LT extends BoolInstr { public LT(Instr lhs, Instr rhs) { super(lhs,rhs); } String op() { return "<" ; } boolean doOp(long lhs, long rhs) { return lhs <  rhs; } }
    public static class LE extends BoolInstr { public LE(Instr lhs, Instr rhs) { super(lhs,rhs); } String op() { return "<="; } boolean doOp(long lhs, long rhs) { return lhs <= rhs; } }

}
