package org.simple.instructions;

import org.simple.bbs.BB;
import org.simple.type.Type;

public class MultiReturnInstr extends Instr{
    public MultiReturnInstr(BB c, Instr... data) {
        super(data);
        _bb = c;
    }

    @Override
    public String label() {
        return "MultiReturn";
    }

    @Override
    public Type compute() {
        return Type.BOTTOM;
    }

    @Override
    StringBuilder _print1(StringBuilder sb) {
        sb.append("[");
        for (int i = 0; i < nIns(); i++) {
            in(i)._print0(sb);
            if (i < nIns() - 1)  sb.append(" ");
        }
        sb.append("]");
        return sb;
    }

    @Override
    public Instr idealize() {
        if (nIns() == 1) {
            // folds to a single return
            return in(0).peephole();
        }
        return null;
    }
}
