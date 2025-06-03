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
    @Override public String label() {return "Scope";}


    @Override
    StringBuilder _print1(StringBuilder sb) {
        sb.append(label());
        for( HashMap<String,Integer> scope : _scopes ) {
            sb.append("[");
            boolean first=true;
            for( String name : scope.keySet() ) {
                if( !first ) sb.append(", ");
                first=false;
                sb.append(name).append(":");
                Instr n = in(scope.get(name));
                if( n==null ) sb.append("null");
                else n._print0(sb);
            }
            sb.append("]");
        }
        return sb;
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
        ScopeInstr dup = new ScopeInstr();
        for(HashMap<String, Integer> sysms: _scopes) {
            dup._scopes.push(new HashMap<>(sysms));
        }
        for(int i = 0; i < nIns(); i++) {
            dup.addDef(in(i));
        }
        return dup;
    }

    public void mergeScopes(ScopeInstr that, BB cb) {
        String[] ns = reverseNames();
        for(int i = 0; i < nIns(); i++) {
            if( in(i) != that.in(i) ) { // No need for redundant Phis
                Instr phi = new PhiInstr(ns[i], in(i), that.in(i)).peephole();
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
        try {
            //  Block of code to try
            Instr a = in(idx);
        }
        catch(Exception e) {
            //  Block of code to handle errors
            System.out.print("Here");
        }
        Instr old = in(idx);

        return n==null ? old : setDef(idx,n);
    }
}
