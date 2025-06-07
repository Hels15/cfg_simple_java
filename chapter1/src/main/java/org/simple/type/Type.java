package org.simple.type;

public class Type{
    static final byte TBOT    = 0;
    static final byte TTOP    = 1;
    static final byte TCTRL   = 2;
    static final byte TXCTRL  = 3;
    static final byte TSIMPLE = 4;
    static final byte TINT    = 5;
    static final byte TTUPLE  = 6; // parallel unrelated types

    public final byte _type;

    public boolean is_simple() {return _type < TSIMPLE;}
    private static final String[] STRS = new String[]{"Bot", "Top", "Ctrl", "~Ctrl"};
    protected Type(byte type) {_type = type;}

    public static final Type BOTTOM   = new Type(TBOT);
    public static final Type TOP      = new Type(TTOP);
    public static final Type CONTROL  = new Type(TCTRL);
    public static final Type XCONTROL = new Type(TXCTRL);

    public Type meet(Type t) {
        if(t == this) return this;
        if(_type == t._type) return xmeet(t);

        if(is_simple()) return this.xmeet(t);
        if(t.is_simple()) return t.xmeet(this);

        return Type.BOTTOM;
    }
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
