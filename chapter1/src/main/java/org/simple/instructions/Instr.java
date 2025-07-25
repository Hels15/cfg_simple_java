package org.simple.instructions;

import org.simple.IterPeeps;
import org.simple.Parser;
import org.simple.Utils;
import org.simple.bbs.BB;
import org.simple.type.Type;
import org.simple.type.TypeTuple;

import javax.sound.midi.SysexMessage;
import javax.swing.plaf.synth.SynthTableHeaderUI;
import java.util.*;

// Todo add label
public abstract class Instr {
    public final int _nid;

    public BB _bb; // Basic Block this instruction is in, null if not in a BB
    // for use-def chains
    public final ArrayList<Instr> _inputs;
    public Type _type;

    public static boolean _disablePeephole = false;
    public static boolean _disableGvn      = false;
    public static boolean _disablePasses   = false;

    public final ArrayList<Instr> _outputs;

    private static int UNIQUE_ID = 1;


    ArrayList<Instr> _deps;

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

        if(_nid == 19) {
            System.out.print("Here");
        }
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
        unlock();
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
    // Use this to indicate that the instruction should be included in the graph visualization.

    public boolean graphVis() {return true;}
    public abstract Type compute();
    public abstract Instr idealize();

    public int nIns() {return _inputs.size();}

    public Instr out(int i) {return _outputs.get(i);}
    public int nOuts() {return _outputs.size();}

    public boolean isUnused(){return nOuts() == 0;}

    public void kill() {
        unlock();
        assert isUnused();
        _type=null;
        while( nIns()>0 ) {
            Instr old_def = _inputs.removeLast();
            if( old_def != null ) {
                IterPeeps.add(old_def);
                if( old_def.delUse(this) )
                    old_def.kill();
            }
        }
        assert isDead();
    }

    public boolean isDead() { return isUnused() && nIns()==0 && _type==null; }


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
        unlock();
        for (int i = 0; i < n; i++) {
            Instr old_def = _inputs.removeLast();
            if(old_def != null && old_def.delUse(this)) {
                old_def.kill();
            }
        }
    }

    public Instr setDef(int idx, Instr new_def) {
        unlock();
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

    public void subsume(Instr nnn) {
        while(nOuts() > 0) {
            Instr n = _outputs.removeLast();
            int idx = Utils.find(n._inputs, this);
            n._inputs.set(idx, nnn);
            nnn.addUse(n);
        }
        kill();
    }
    Instr swap12() {
        unlock();
        Instr tmp = in(0);
        _inputs.set(0, in(1));
        _inputs.set(1, tmp);
        return this;
    }
    Instr addDep(Instr dep) {
        if(_deps == null) _deps = new ArrayList<>();
        if(Utils.find(_deps, dep) != -1)    return this;
        if(Utils.find(_inputs, dep) != -1)  return this;
        if(Utils.find(_outputs, dep) != -1) return this;
        _deps.add(dep);
        return this;
    }

    public void moveDepsToWorkList() {
        if(_deps == null) return;
        IterPeeps.addAll(_deps);
        _deps.clear();
    }
    protected boolean delUse(Instr n) {
        Utils.del(_outputs, Utils.find(_outputs, n));
        return _outputs.isEmpty();
    }

    boolean allCons(Instr dep) {
        for(int i = 0; i < nIns(); i++)
            if(!(in(i)._type.isConstant())) {
                in(i).addDep(dep);
                return false;
            }

        return true;
    }

    public Instr iterate() {
        IterPeeps.iterate(Parser._entry);
        return this;
    }

    public boolean debug() {
        return false;
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

        //assert old == null || type.isa(old);
        if (!(old == null || type.isa(old))) {
            // Place a regular breakpoint on the line below
            System.out.println("Breakpoint condition met");
        }
        assert old==null || type.isa(old);
        if(old == type) return old;
        _type = type;
        // This is the main populator
        IterPeeps.addAll(_outputs);
        moveDepsToWorkList();
        // Iterpeeps call here
        // move deps to worklist
        return old;
    }

    public final Instr peepholeOpt() {
        Type old = setType(compute());
        // means that after the iterative algo we finally figure out that the cond is dead
        if(_nid == 14 && _type != null && _type != TypeTuple.IF_BOTH) {
            System.out.print("Here");
        }
        if(!(this instanceof ConstantInstr) && _type.isConstant())
            return new ConstantInstr(_type, _bb).peephole();

        if(!_disableGvn) {
            if(_hash == 0) {
                Instr n = GVN.get(this);
                if (n == null) {
                    GVN.put(this, this);
                } else {
                    // Because of random worklist ordering, the two equal nodes
                    // might have different types.  Because of monotonicity, both
                    // types are valid.  To preserve monotonicity, the resulting
                    // shared Node has to have the best of both types.
                    n.setType(n._type.join(_type));
                    _hash = 0;
                    return deadCodeElim(n);
                }

            }
        }
        Instr n = idealize();
        if(_nid == 20 &&n != null) {
            System.out.print("Here");
        }
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
