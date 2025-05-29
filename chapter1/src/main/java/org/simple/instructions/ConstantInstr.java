package org.simple.instructions;

import org.simple.type.Type;

public class ConstantInstr extends Instr {
    Type _con;
    public ConstantInstr(Type type) {
        _con = type;
    }
    @Override
    public String label() {
        return "Constant";
    }
    @Override public Type compute() {
        return _con;
    }
    @Override
    StringBuilder _print1(StringBuilder sb) {
        return _con._print(sb);
    }


    @Override public Instr idealize() {return null;}
}
