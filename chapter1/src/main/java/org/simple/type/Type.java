package org.simple.type;

import org.simple.Utils;

import java.util.HashMap;

public class Type{
    static final byte TBOT    = 0;
    static final byte TTOP    = 1;
    static final byte TCTRL   = 2;
    static final byte TXCTRL  = 3;
    static final byte TSIMPLE = 4;
    static final byte TINT    = 5;
    static final byte TTUPLE  = 6; // parallel unrelated types

    public final byte _type;

    private int _hash;

    public boolean is_simple() {return _type < TSIMPLE;}
    private static final String[] STRS = new String[]{"Bot", "Top", "Ctrl", "~Ctrl"};
    protected Type(byte type) {_type = type;}

    static final HashMap<Type, Type> INTERN = new HashMap<>();

    public static final Type BOTTOM   = new Type(TBOT).intern();
    public static final Type TOP      = new Type(TTOP).intern();
    public static final Type CONTROL  = new Type(TCTRL).intern();
    public static final Type XCONTROL = new Type(TXCTRL).intern();


    protected <T extends Type> T intern() {
        T nnn = (T)INTERN.get(this);
        if(nnn == null) INTERN.put(nnn=(T)this, this);
        return nnn;
    }

    @Override
    public final int hashCode() {
        if( _hash!=0 ) return _hash;
        _hash = hash();
        if( _hash==0 ) _hash = 0xDEADBEEF; // Bad hash from subclass; use some junk thing
        return _hash;
    }

    @Override
    public final boolean equals( Object o ) {
        if( o==this ) return true;
        if( !(o instanceof Type t)) return false;
        if( _type != t._type ) return false;
        return eq(t);
    }

    boolean eq(Type t) { return true; }

    int hash() { return _type; }

    public Type meet(Type t) {
        if(t == this) return this;
        if(_type == t._type) return xmeet(t);

        if(is_simple()) return this.xmeet(t);
        if(t.is_simple()) return t.xmeet(this);

        return Type.BOTTOM;
    }

    public final Type join(Type t) {
        if( this==t ) return this;
        return dual().meet(t.dual()).dual();
    }
    public Type dual() {
        return switch( _type ) {
            case TBOT -> TOP;
            case TTOP -> BOTTOM;
            case TCTRL -> XCONTROL;
            case TXCTRL -> CONTROL;
            default -> throw Utils.TODO("Should not reach here!"); // Should not reach here
        };
    }

    public boolean isa(Type t) {return meet(t) == t;}
    protected Type xmeet(Type t) {
        assert is_simple();
        if(_type == TBOT || t._type == TTOP) return this;
        if(_type == TTOP || t._type == TBOT) return t;

        if(!t.is_simple()) return Type.BOTTOM;

        return _type == TCTRL || t._type == TCTRL ? Type.CONTROL : Type.XCONTROL;
    }

    @Override
    public final String toString() {
        return _print(new StringBuilder()).toString();
    }

    public boolean isConstant() {return _type == TTOP || _type == TXCTRL;}

    public StringBuilder _print(StringBuilder sb) {return is_simple() ? sb.append(STRS[_type]) : sb;}

}
