package org.simple.bbs;

import org.simple.instructions.Instr;

import java.util.ArrayList;

// TODO: just sucessor interface, entry doesnt have predecessor, exit block doesnt have sucessor
// Todo: otherwise just add predecessor with sucessors
public class BB {

    public final ArrayList<BB> _succs;

    public final ArrayList<BB> _preds;

    public final ArrayList<Instr> _instrs;

    public BB() {
        _instrs = new ArrayList<>();
        _succs = new ArrayList<>();
        _preds = new ArrayList<>();
    }

    public void addSuccessor(BB bb) {
        if (!_succs.contains(bb)) {
            _succs.add(bb);
            bb.addPredecessor(this);
        }
    }

    public void addPredecessor(BB bb) {
        if (!_preds.contains(bb)) {
            _preds.add(bb);
        }
    }

    public void addInstr(Instr instr) {
        if (!_instrs.contains(instr)) {
            _instrs.add(instr);
            instr._bb = this;
        }
    }

}