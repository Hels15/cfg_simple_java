package org.simple.bbs;

import org.simple.instructions.IfInstr;
import org.simple.instructions.Instr;
import org.simple.instructions.ReturnInstr;
import org.simple.type.Type;

import java.util.ArrayList;

// TODO: just sucessor interface, entry doesnt have predecessor, exit block doesnt have sucessor
// Todo: otherwise just add predecessor with sucessors
public class BB {

    public final ArrayList<BB> _succs;

    public String _label;

    public enum BBKind {
        NONE,
        ENTRY,
        EXIT,
        LOOP_HEADER,
        BACK_EDGE,
        CONDITIONAL_BLOCK,
        LOOP_BODY,
        LOOP_EXIT,
        BREAK,
        CONTINUE,
        RETURN,
        MERGE,
        DEAD,
        GENERIC
    }

    public BBKind _kind = BBKind.NONE;
    // typed BB
    public Type _type;

    int _idepth;


    public final int _nid;
    private static int UNIQUE_BB_ID = 1;

    public final ArrayList<BB> _preds;

    public final ArrayList<Instr> _instrs;

    public boolean dead() {
        return _type == Type.XCONTROL;
    }

    public BB(String label) {
        _instrs = new ArrayList<>();
        _succs = new ArrayList<>();
        _preds = new ArrayList<>();
        _label = label; // no label by default
        _type = Type.CONTROL; // default type
        _nid = UNIQUE_BB_ID++;
    }
    public BB()
    {
       this("");
    }
    public void clear_succs() {
        for (BB succ : _succs) {
            succ._preds.remove(this);
        }
        _succs.clear();
    }

    public void clear_preds() {
        for (BB pred : _preds) {
            pred._succs.remove(this);
        }
        _preds.clear();
    }

    public void addSuccessor(BB bb) {
        if (!_succs.contains(bb)) {
            _succs.add(bb);

            // only can mark current BB as dead if one of the predecessors is alive and returns
//            if(_type == Type.XCONTROL && (endInstr() instanceof ReturnInstr)) bb._type = Type.XCONTROL;
            bb.addPredecessor(this);
        }
    }


    public void addPredecessor(BB bb) {
        if (!_preds.contains(bb)) {
            // only can mark current BB as dead if one of the predecessors is alive and returns
//            if(bb._type == Type.XCONTROL && (bb.endInstr() instanceof ReturnInstr)) _type = Type.XCONTROL;
            _preds.add(bb);
        }
    }

    // For now it just picks the first predecessor but it shouldnt matter
    public BB idom() {
        BB new_idom = null;
        for (BB pred : _preds) {
            if(_kind == BBKind.LOOP_HEADER && (pred._kind == BBKind.BACK_EDGE ||
                pred._kind == BBKind.CONTINUE) && pred._nid > _nid) continue;
            if (new_idom == null)
                new_idom = pred;
            else
                new_idom = intersect(pred, new_idom);
        }
        if (new_idom != null) {
            // Update this block's idepth based on its idom
            this._idepth = new_idom._idepth + 1;
        } else {
            // Entry block or unreachable
            this._idepth = 0;
        }
        return new_idom;
    }

    BB intersect(BB b1, BB b2) {
        assert b1 != null && b2 != null;
        while (b1 != b2) {
            if (b1._idepth > b2._idepth)
                b1 = b1.idom();
            else if (b2._idepth > b1._idepth)
                b2 = b2.idom();
            else {
                b1 = b1.idom();
                b2 = b2.idom();
            }
        }
        return b1;
    }

    public int idx(BB pred) {
        assert endInstr() instanceof IfInstr;
        if(_succs.getFirst() == pred) return 0;
        if(_succs.getLast() == pred)  return 1;
        return -1;
    }
    // ending instruction
    public Instr endInstr() {
        assert !_instrs.isEmpty() : "Basic block has no instructions";
        return _instrs.getLast();
    }

    public void addInstr(Instr instr) {
        if (!_instrs.contains(instr)) {
            _instrs.add(instr);
            instr._bb = this;
        }
    }

}