package org.simple.instructions;

import org.simple.bbs.BB;
import org.simple.type.Type;

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
        for(int i = 0; i < nIns(); i++) {
            if(!loop) dup.addDef(in(i));
            else {
                String[] names = reverseNames();

                dup.addDef(new PhiInstr(_bb, names[i], in(i), null)).peephole();
                setDef(i, dup.in(i));
            }
        }
        dup._bb = _bb;
        return dup;
    }

    public void endLoop(ScopeInstr back) {
        for(int i = 0; i < nIns(); i++) {
            PhiInstr phi = (PhiInstr)in(i);
            phi.setDef(1, back.in(i));
            Instr in = phi.peephole();
            if(in != phi) {
                phi.subsume(in);
            }
        }
        back.kill();
    }
    public void mergeScopes(ScopeInstr that, BB cb) {
        String[] ns = reverseNames();
        for(int i = 0; i < nIns(); i++) {
            if( in(i) != that.in(i) ) { // No need for redundant Phis
                Instr phi = new PhiInstr(cb, ns[i], in(i), that.in(i)).peephole();
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

        return n==null ? old : setDef(idx,n);
    }
}
