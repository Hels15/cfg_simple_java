package org.simple.instructions;

import org.simple.bbs.BB;
import org.simple.type.Type;
import org.simple.type.TypeInteger;
import org.simple.type.TypeTuple;

import java.util.BitSet;

public class IfInstr extends Instr {
    BB _true_bb;
    BB _false_bb;

    public IfInstr(BB c, Instr pred) {
        super(pred);
        _bb = c;
    }

    @Override
    public String label() {
        return "If";
    }

    @Override
    StringBuilder _print1(StringBuilder sb, BitSet visited) {
        sb.append("if (");
        return in(0)._print0(sb, visited).append(" )");
    }

    public Instr pred() {
        return in(0);
    }

    public BB true_bb() {
        return _true_bb;
    }

    public BB false_bb() {
        return _false_bb;
    }

    @Override
    public Type compute() {
        if (pred()._type instanceof TypeInteger ti && ti.isConstant()) {
            if (ti.value() == 0) return TypeTuple.IF_FALSE;
            else return TypeTuple.IF_TRUE;
        }
        // Already in worklist becuase the if is an output of the predicate
        if(pred() instanceof PhiInstr) {
            System.out.print("Here");
        }

        pred().addDep(this);
        for(BB dom = _bb.idom(), prior=_bb; dom!=null;  prior=dom, dom = dom.idom() )
            if(!dom._instrs.isEmpty() && dom.endInstr() instanceof IfInstr iff && iff.pred()==pred() ) {
                int idx = dom.idx(prior);

                 if(idx != -1) {
                    return idx == 0 ?  TypeTuple.IF_TRUE : TypeTuple.IF_FALSE;
                 } else return null;
            }

        return TypeTuple.IF_BOTH;
    }

    @Override
    public Instr idealize() {
        return null;
    }

    public void create_bbs(BB c) {
        Type trueType, falseType;

        BB truebb = new BB("true");
        truebb._kind = BB.BBKind.CONDITIONAL_BLOCK;

        BB falsebb = new BB("false");
        falsebb._kind = BB.BBKind.CONDITIONAL_BLOCK;

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

        if(c._succs.size() == 2) {
            c._succs.set(0, truebb);
        } else {
            c.addSuccessor(truebb);
        }

        falsebb._type = falseType;
        _false_bb = falsebb;

        if(c._succs.size() == 2) {
            c._succs.set(1, falsebb);
        } else {
            c.addSuccessor(falsebb);
        }

    }

    // This is a specific method to add a successor BB to the 2 branches of the if
    // It looks at BB level optimisations
    // It adds a successor to the 2 branches of the if
    //         if()
    //       /      \
    //    true_bb   false_bb
    //       \       /
    //         merge
    // Add merge BB as a successor to both true_bb and false_bb
    public void addIfSuccessor(BB merge) {
        // only add merge as a successor if its not terminated already

        // have a special case where if its a return then the only successor is the exit
        true_bb().addSuccessor(merge);
        false_bb().addSuccessor(merge);

        for (int i = 0; i < merge._preds.size(); i++) {
            BB pred_1 = merge._preds.get(i);
            BB pred_2 = merge._preds.get(1 - i);
            if (pred_1.dead() && !pred_2._instrs.isEmpty() && pred_2.endInstr() instanceof ReturnInstr) {
                merge._type = Type.XCONTROL;
            }
        }
        if (!merge._preds.getFirst().dead() && !merge._preds.getLast().dead()
                && !merge._preds.getFirst()._instrs.isEmpty() && !merge._preds.getLast()._instrs.isEmpty()
                && merge._preds.getFirst().endInstr() instanceof ReturnInstr && merge._preds.getLast().endInstr() instanceof ReturnInstr) {
            merge._type = Type.XCONTROL;
        }
    }
}
