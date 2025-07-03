package org.simple.instructions;

import org.simple.type.Type;

import java.util.BitSet;

// Todo: visualise type
// Just an instruction for visualisation purposes
public class AssignmentInstr extends Instr{
    public String _name;
    public boolean _init;
    public AssignmentInstr(boolean init, String name, Instr expr) {
        super(expr);
        _name = name;
        _init = init;
    }

    @Override public boolean debug() {
        return true;
    }

    @Override
    public String label() {
        return _name;
    }

    @Override
    StringBuilder _print1(StringBuilder sb, BitSet visited) {
        sb.append(_init ? "def " : "").append(_name).append(" = ").append(in(0).print());
        return sb;
    }

    @Override
    public Type compute() {
        return null;
    }

    @Override
    public Instr idealize() {
        return null;
    }

}
