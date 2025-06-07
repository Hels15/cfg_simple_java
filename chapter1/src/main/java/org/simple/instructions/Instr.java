package org.simple.instructions;

import org.simple.Utils;
import org.simple.bbs.BB;
import org.simple.type.Type;

import java.util.*;

// Todo add label
public abstract class Instr {
    public final int _nid;

    public BB _bb; // Basic Block this instruction is in, null if not in a BB
    // for use-def chains
    public final ArrayList<Instr> _inputs;
    public Type _type;

    public static boolean _disablePeephole = false;
    public static boolean _disablePasses = false;

    public final ArrayList<Instr> _outputs;

    private static int UNIQUE_ID = 1;


    public abstract String label();

    Instr(Instr... inputs) {
        _nid = UNIQUE_ID++;
        _inputs = new ArrayList<>();
        _outputs = new ArrayList<>();

        Collections.addAll(_inputs,inputs);
        for( Instr n : _inputs )
            if( n != null )
                n.addUse( this );
    }

    public Instr in(int i) {
        return _inputs.get(i);
    }

    @Override
    public final String toString() {
        return print();
    }
    public final String print() {
        return _print0(new StringBuilder()).toString();

    }

    Instr copy(BB c, Instr lhs, Instr rhs) { throw Utils.TODO("Binary ops need to implement copy"); }

    public String uniqueName() {return label() + _nid;}
    final StringBuilder _print0(StringBuilder sb) {
        return isDead()
                ? sb.append(uniqueName()).append(":DEAD")
                : _print1(sb);
    }

    public Instr addDef(Instr new_def) {
        _inputs.add(new_def);
        if(new_def != null) new_def.addUse(this);
        return new_def;
    }
    abstract StringBuilder _print1(StringBuilder sb);

    public <I extends Instr> I keep() {return addUse(null);}
    public <I extends Instr> I unkeep() {delUse(null); return (I)this;}
    private Instr deadCodeElim(Instr m) {
        if(m != this && isUnused()) {
            m.keep();
            kill();
            m.unkeep();
        }
        return m;
    }
    public abstract Type compute();
    public abstract Instr idealize();

    public int nIns() {return _inputs.size();}

    public Instr out(int i) {return _outputs.get(i);}
    public int nOuts() {return _outputs.size();}

    public boolean isUnused(){return nOuts() == 0;}

    public void kill() {
        assert isUnused();
        for( int i=0; i<nIns(); i++ )
            setDef(i,null);
        _inputs.clear();
        _type=null;
        assert isDead();
    }

    boolean isDead() { return isUnused() && nIns()==0 && _type==null; }


    void popN(int n ) {
        for (int i = 0; i < n; i++) {
            Instr old_def = _inputs.removeLast();
            if(old_def != null && old_def.delUse(this)) {
                old_def.kill();
            }
        }
    }

    Instr setDef(int idx, Instr new_def) {
        Instr old_def = in(idx);
        if(old_def == new_def) return this;

        if(new_def != null) new_def.addUse(this);

        if(old_def != null && old_def.delUse(this)) {
            old_def.kill(); // Kill old_def
        }
        _inputs.set(idx, new_def);

        return new_def;
    }
    protected <I extends Instr> I addUse(Instr n) {
        _outputs.add(n);
        return (I)this;
    }

    void delDef(int idx) {
        Instr old_def = in(idx);
        if(old_def != null && old_def.delUse(this)) {
            old_def.kill();
        }
        Utils.del(_inputs, idx);
    }

    Instr swap12() {
        Instr tmp = in(0);
        _inputs.set(0, in(1));
        _inputs.set(1, tmp);
        return this;
    }
    protected boolean delUse(Instr n) {
        Utils.del(_outputs, Utils.find(_outputs, n));
        return _outputs.isEmpty();
    }

    boolean allCons() {
        for(int i = 0; i < nIns(); i++)
            if(!(in(i)._type.isConstant()))
                return false;
        return true;
    }

    public final Instr peephole() {
        Type type = _type = compute();

        if(_disablePeephole) return this;

        // Replace constant computations from non-constants with a constant node
        if (!(this instanceof ConstantInstr) && type.isConstant()) {
            return  deadCodeElim(new ConstantInstr(type, _bb).peephole());
        }

        Instr n = idealize();
        if(n != null) return deadCodeElim(n.peephole());

        return this;

    }
}
