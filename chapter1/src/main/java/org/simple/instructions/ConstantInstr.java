package org.simple.instructions;

import org.simple.bbs.BB;
import org.simple.type.Type;

import java.util.BitSet;

public class ConstantInstr extends Instr {
    Type _con;
    String _label;
    public ConstantInstr(Type type, BB bb) {
        this(type, null, bb);
    }
    public ConstantInstr(Type type, String label, BB bb) {
        _con = type;
        _label = label;
        _bb = bb;
    }
    @Override
    public String label() {
        return _label != null ? _label : ""+_con;
    }
    @Override public Type compute() {
        return _con;
    }
    @Override
    StringBuilder _print1(StringBuilder sb, BitSet visited) {
        return _label != null && !_con.isConstant()? sb.append(_label): _con._print(sb);
        //return _con._print(sb);

    }

    @Override
    boolean eq(Instr n) {
        ConstantInstr con = (ConstantInstr)n;
        return _con == con._con;
    }

    @Override int hash() {
        return _con.hashCode();
    }
    @Override public boolean graphVis() {
        return false;
    }


    @Override public Instr idealize() {return null;}
}
