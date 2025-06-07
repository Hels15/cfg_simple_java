package org.simple.bbs;

import org.simple.instructions.Instr;
import org.simple.type.Type;

import java.util.ArrayList;

// TODO: just sucessor interface, entry doesnt have predecessor, exit block doesnt have sucessor
// Todo: otherwise just add predecessor with sucessors
public class BB {

    public final ArrayList<BB> _succs;

    // typed BB
    public Type _type;

    public final int _nid;
    private static int UNIQUE_BB_ID = 1;

    public final ArrayList<BB> _preds;

    public final ArrayList<Instr> _instrs;

    public BB() {
        _instrs = new ArrayList<>();
        _succs = new ArrayList<>();
        _preds = new ArrayList<>();
        _type = Type.CONTROL; // default type
        _nid = UNIQUE_BB_ID++;
    }

    public void addSuccessor(BB bb) {
        if (!_succs.contains(bb)) {
            _succs.add(bb);
            if(_type == Type.XCONTROL) bb._type = Type.XCONTROL;
            bb.addPredecessor(this);
        }
    }

    public void addPredecessor(BB bb) {
        if (!_preds.contains(bb)) {
            if(bb._type == Type.XCONTROL) _type = Type.XCONTROL;
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