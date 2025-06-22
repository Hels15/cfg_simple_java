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


    public static final HashMap<Instr, Instr> GVN = new HashMap<>();

    public abstract String label();

    // Cached hash.  If zero, then not computed AND this Node is NOT in the GVN
    // table - and can have its edges hacked (which will change his hash
    // anyway).  If Non-Zero then this Node is IN the GVN table, or is being
    // probed to see if it can be inserted.  His edges are "locked", because
    // hacking his edges will change his hash.
    int _hash;

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
        return _print0(new StringBuilder(), new BitSet()).toString();

    }

    Instr copy(BB c, Instr lhs, Instr rhs) { throw Utils.TODO("Binary ops need to implement copy"); }

    public String uniqueName() {return label() + _nid;}
    final StringBuilder _print0(StringBuilder sb, BitSet visited) {
        if(visited.get(_nid)) return sb.append(label());
        visited.set(_nid);

        return isDead()
                ? sb.append(uniqueName()).append(":DEAD")
                : _print1(sb, visited);
    }

    public Instr addDef(Instr new_def) {
        _inputs.add(new_def);
        if(new_def != null) new_def.addUse(this);
        return new_def;
    }
    abstract StringBuilder _print1(StringBuilder sb, BitSet visited);

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


    // Subclasses add extra checks (such as ConstantNodes have same constant),
    // and can assume "this!=n" and has the same Java class.
    boolean eq( Instr i) { return true; }

    @Override public final boolean equals(Object o) {
        if( o==this ) return true;
        if( o.getClass() != getClass() ) return false;
        Instr n = (Instr)o;
        int len = _inputs.size();
        if( len != n._inputs.size() ) return false;
        for( int i=0; i<len; i++ )
            if( in(i) != n.in(i) )
                return false;
        return eq(n);
    }

    void unlock() {
        if(_hash == 0) return;
        Instr old = GVN.remove(this);
        assert old == this;
        _hash = 0;
    }

    void popN(int n ) {
        for (int i = 0; i < n; i++) {
            Instr old_def = _inputs.removeLast();
            if(old_def != null && old_def.delUse(this)) {
                old_def.kill();
            }
        }
    }

    public Instr setDef(int idx, Instr new_def) {
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

    void subsume(Instr nnn) {
        while(nOuts() > 0) {
            Instr n = _outputs.removeLast();
            int idx = Utils.find(n._inputs, this);
            n._inputs.set(idx, nnn);
            nnn.addUse(n);
        }
        kill();
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

    public boolean pure() {return true;}
    public final Instr peephole() {
        if(_disablePeephole) {
            _type = compute();
            return this;
        }

        Instr n = peepholeOpt();
        return n == null ? this: deadCodeElim(n.peephole());

    }
    public Type setType(Type type) {
        Type old = _type;
        // gradual type increment
        assert old == null || type.isa(old);
        if(old == type) return old;
        _type = type;
        // Iterpeeps call here
        // move deps to worklist
        return old;
    }

    public final Instr peepholeOpt() {
        Type old = setType(compute());

        if(!(this instanceof ConstantInstr) && _type.isConstant())
            return new ConstantInstr(_type, _bb).peephole();

        if(_hash == 0) {
            Instr n = GVN.get(this);
            if(n == null) {
                GVN.put(this, this);
            }
            else {
                // Because of random worklist ordering, the two equal nodes
                // might have different types.  Because of monotonicity, both
                // types are valid.  To preserve monotonicity, the resulting
                // shared Node has to have the best of both types.
                n.setType(n._type.join(_type));
                _hash = 0;
                return deadCodeElim(n);
            }

        }
        Instr n = idealize();
        if(n != null) {
            return n;
        }
        return old ==_type ? null: this;
    }

    @Override public final int hashCode() {
        if(_hash != 0) return _hash;
        int hash = hash();
        for(Instr n: _inputs) {
            if(n!= null) hash = hash ^ (hash<<17) ^ (hash>>13) ^ n._nid;

        }
        if(hash == 0) hash = 0xDEADBEEF; // Bad hash from subclass; use some junk thing
        return (_hash = hash);
    }

    int hash() {return 0;}
}
