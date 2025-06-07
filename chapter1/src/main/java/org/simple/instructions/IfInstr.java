package org.simple.instructions;

import org.simple.bbs.BB;
import org.simple.type.Type;
import org.simple.type.TypeInteger;
import org.simple.type.TypeTuple;

public class IfInstr extends Instr{
    BB _true_bb;
    BB _false_bb;

    public IfInstr(BB c, Instr pred) {
        super(pred); _bb = c;
    }
    @Override
    public String label() {
        return "If";
    }
    @Override
    StringBuilder _print1(StringBuilder sb) {
        sb.append("if (");
        return in(0)._print0(sb).append(" )");
    }

    public Instr pred() {return in(0);}

    public BB true_bb() { return _true_bb; }
    public BB false_bb() { return _false_bb; }

    @Override
    public Type compute() {
        if (pred()._type instanceof TypeInteger ti && ti.isConstant()) {
            if (ti.value() == 0)   return TypeTuple.IF_FALSE;
            else                   return TypeTuple.IF_TRUE;
        }

        return TypeTuple.IF_BOTH;
    }

    @Override
    public Instr idealize() {
        return null;
    }

    public void create_bbs(BB c) {
        Type trueType, falseType;

        BB truebb = new BB();
        BB falsebb = new BB();

        if (_type == TypeTuple.IF_FALSE) {
            trueType = Type.XCONTROL;
            falseType = Type.CONTROL;
        } else if (_type == TypeTuple.IF_TRUE) {
            trueType = Type.CONTROL;
            falseType = Type.XCONTROL;
        } else if (_type == TypeTuple.IF_BOTH) {
            trueType = Type.CONTROL;
            falseType = Type.CONTROL;
        } else {
            return;
        }

        truebb._type = trueType;
        _true_bb = truebb;
        c.addSuccessor(truebb);

        falsebb._type = falseType;
        _false_bb = falsebb;
        c.addSuccessor(falsebb);
    }
}
