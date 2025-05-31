package org.simple.instructions;

import org.simple.type.Type;

public class ConstantInstr extends Instr {
    Type _con;
    String _label;
    public ConstantInstr(Type type) {
        this(type, null);
    }
    public ConstantInstr(Type type, String label) {
        _con = type;
        _label = label;
    }
    @Override
    public String label() {
        return _label != null ? _label : "Constant";
    }
    @Override public Type compute() {
        return _con;
    }
    @Override
    StringBuilder _print1(StringBuilder sb) {
        return _label != null && !_con.isConstant()? sb.append(_label): _con._print(sb);
    }

    @Override public Instr idealize() {return null;}
}
