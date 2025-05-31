package org.simple.instructions;

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
