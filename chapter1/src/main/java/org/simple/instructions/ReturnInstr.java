package org.simple.instructions;


import org.simple.type.Type;

public class ReturnInstr extends Instr {
    public ReturnInstr(Instr data) {super(data);}
    @Override
    public String label() {
        return "Return";
    }

    @Override
    public Type compute() {
        return Type.BOTTOM;
    }

    public Instr expr() { return in(0); }

    @Override
    StringBuilder _print1(StringBuilder sb) {
        return expr()._print0(sb.append("return ")).append(";");
    }

    @Override
    public Instr idealize() { return null; }
}
