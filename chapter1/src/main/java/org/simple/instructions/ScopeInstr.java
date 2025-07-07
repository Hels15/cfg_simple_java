package org.simple.instructions;

import org.simple.IterPeeps;
import org.simple.bbs.BB;
import org.simple.type.Type;

import java.lang.foreign.MemorySegment;
import java.util.*;

public class ScopeInstr extends Instr{
    public final Stack<HashMap<String, Integer>> _scopes;

    public static final String ARG0 = "arg";

    public ScopeInstr() {
        _scopes = new Stack<>();
        _type = Type.BOTTOM;
    }
    public void ctrl(BB ctrl) {
        _bb = ctrl;
    }
    @Override public String label() {return "Scope";}


    @Override
    StringBuilder _print1(StringBuilder sb, BitSet visited) {
        sb.append("Scope[ ");
        String[] names = reverseNames();
        for(int j = 0; j < nIns(); j++) {
            sb.append(names[j]).append(":");
            Instr instr = in(j);
            while(instr instanceof ScopeInstr loop) {
                sb.append("Lazy_");
                instr = loop.in(j);
            }
            instr._print0(sb, visited).append(" ");
        }
        return sb.append("]");
    }

    public String[] reverseNames() {
        String[] names = new String[nIns()];
        for(HashMap<String, Integer> sysms: _scopes) {
            for(String name: sysms.keySet()) {
                names[sysms.get(name)] = name;
            }
        }
        return names;
    }

    public ScopeInstr dup() {
        return dup(false);
    }
    public ScopeInstr dup(boolean loop) {
        assert _bb != null : "ScopeInstr must have a BB";

        ScopeInstr dup = new ScopeInstr();
        for(HashMap<String, Integer> sysms: _scopes) {
            dup._scopes.push(new HashMap<>(sysms));
        }
        for(int i = 0; i < nIns(); i++)
            dup.addDef(loop ? this: in(i));

        dup._bb = _bb;
        return dup;
    }

    public void endLoop(ScopeInstr back, ScopeInstr exit) {
        for(int i = 0; i < nIns(); i++) {
            if(back.in(i) != this) {
                PhiInstr phi = (PhiInstr)in(i);
                phi.setDef(1, back.in(i));
            }
            if(exit.in(i) == this) exit.setDef(i, in(i));
        }

        back.kill();
        for(int i = 0; i < nIns(); i++) {
            if(in(i) instanceof PhiInstr phi) {
                Instr in = phi.peephole();
                IterPeeps.addAll(phi._outputs);
                // folds phi and becomes 0
                if(phi._nid == 13) {
                    System.out.print("Here");
                }
                phi.moveDepsToWorkList();
                if(in != phi) {
                    phi.subsume(in);
                    setDef(i, in);
                }
            }
        }
    }
    public void mergeScopes(ScopeInstr that, BB cb) {
        String[] ns = reverseNames();
        for(int i = 0; i < nIns(); i++) {
            if( in(i) != that.in(i) ) { // No need for redundant Phis
                Instr phi = new PhiInstr(cb, ns[i], lookup(ns[i]), that.lookup(ns[i])).peephole();
                if(phi._nid == 19) {
                    System.out.print("Here");
                }
                cb.addInstr(phi);
                setDef(i, phi);
            }
        }
        that.kill();
    }
    @Override public Type compute() {return Type.BOTTOM;}
    @Override public Instr idealize() {return null;}

    public void push() {_scopes.push(new HashMap<>());}
    public void pop() {popN(_scopes.pop().size()); }

    public Instr define(String name, Instr n) {
        HashMap<String, Integer> sysms = _scopes.lastElement();
        if(sysms.put(name, nIns()) != null) return null;
        return addDef(n);
    }

    public Instr lookup(String name) {return update(name, null, _scopes.size() -1);}
    public Instr update(String name, Instr n) {return update(name, n, _scopes.size() -1);}
    public Instr update(String name, Instr n, int nestingLevel) {
        if(nestingLevel < 0) return null;
        var sysm = _scopes.get(nestingLevel);

        var idx = sysm.get(name);
        if( idx == null ) return update(name,n,nestingLevel-1);

        Instr old = in(idx);

        if( old instanceof ScopeInstr loop ) {
            // Lazy Phi!
            old = loop.in(idx) instanceof PhiInstr phi
                    // Loop already has a real Phi, use it
                    ? loop.in(idx)
                    // Set real Phi in the loop head
                    // The phi takes its one input (no backedge yet) from a recursive
                    // lookup, which might have insert a Phi in every loop nest.
                    : loop.setDef(idx,new PhiInstr(_bb, name, loop.update(name,null,nestingLevel),null).peephole());
            setDef(idx,old);
        }

        return n==null ? old : setDef(idx,n);
    }
}
