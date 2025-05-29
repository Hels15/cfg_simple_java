package org.simple.type;

public class Type{
    static final byte TBOT    = 0;
    static final byte TTOP    = 1;
    static final byte TSIMPLE = 2;
    static final byte TINT    = 3;
    public final byte _type;

    public boolean is_simple() {return _type < TSIMPLE;}
    private static final String[] STRS = new String[]{"BOT", "TOP"};
    protected Type(byte type) {_type = type;}

    public static final Type BOTTOM = new Type(TBOT);

    public boolean isConstant() {return _type == TTOP;}

    public StringBuilder _print(StringBuilder sb) {return is_simple() ? sb.append(STRS[_type]) : sb;}

}
