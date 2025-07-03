package org.simple.type;


public class TypeInteger extends Type {
    public final static TypeInteger TOP  = make( false, 0);
    public final static TypeInteger BOT  = make(false, 1);
    public final static TypeInteger ZERO = make(true, 0);

    private final boolean _is_con;
    private final long _con;

    public TypeInteger(boolean is_con, long con) {
        super(TINT);
        _is_con = is_con;
        _con = con;
    }

    public static TypeInteger constant(long con) { return make(true, con); }

    boolean isTop() {return !_is_con && _con == 0;}
    boolean isBot() {return !_is_con && _con == 1;}

    @Override
    public StringBuilder _print(StringBuilder sb) {
        if(isTop()) return sb.append("IntTop");
        if(isBot()) {
            return sb.append("IntBot");
        }
        return sb.append(_con);
    }

    @Override
    public boolean isHighOrConst() { return _is_con || _con==0; }

    public static TypeInteger make(boolean is_con, long con) {
        return new TypeInteger(is_con, con).intern();
    }

    @Override
    public boolean isConstant() { return _is_con; }

    public long value() { return _con; }

    @Override
    public Type xmeet(Type other) {
        // Invariant from caller: 'this' != 'other' and same class (TypeInteger)
        TypeInteger i = (TypeInteger)other; // Contract
        // BOT wins
        if ( this==BOT ) return this;
        if ( i   ==BOT ) return i   ;
        // TOP loses
        if ( i   ==TOP ) return this;
        if ( this==TOP ) return i   ;
        // Since both are constants, and are never equals (contract) unequals
        // constants fall to bottom
        return BOT;
    }

    @Override
    public Type dual() {
        if( isConstant() ) return this; // Constants are a self-dual
        return _con==0 ? BOT : TOP;
    }

    @Override
    int hash() { return (int)(_con ^ (_is_con ? 0 : 0x4000)); }

    @Override
    public boolean eq( Type t ) {
        TypeInteger i = (TypeInteger)t; // Contract
        return _con==i._con && _is_con==i._is_con;
    }
}
