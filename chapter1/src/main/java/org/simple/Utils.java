package org.simple;

import org.simple.bbs.BB;
import org.simple.instructions.Instr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;

public class Utils {
   public static <E> E del(ArrayList<E> array, int i) {
       if ( i >= 0 && i < array.size() ) {
           E tmp = array.get(i);
           E last = array.removeLast();
           if (i < array.size()) array.set(i, last);
           return tmp;
       }
       return null;
   }
    public static <E> int find( ArrayList<E> ary, E x ) {
        for( int i=0; i<ary.size(); i++ )
            if( ary.get(i)==x )
                return i;
        return -1;
    }
    public static RuntimeException TODO(String msg) { return new RuntimeException(msg); }

    // No randomised pull and no swap with last and shrink
    // Simple FIFO worklist
    public static class WorkList<E extends BB> {
       private BB[] _es;
       private int _len;
       private final BitSet _on;
       private long _totalWork = 0;

       WorkList() {
           _es = new BB[1];
           _len = 0;
           _on = new BitSet();
       }

        public E push( E x ) {
            if( x==null ) return null;
            int idx = x._nid;
            if( !_on.get(idx) ) {
                _on.set(idx);
                // _len * 2
                if( _len==_es.length )
                    _es = Arrays.copyOf(_es,_len<<1);
                _es[_len++] = x;
                _totalWork++;
            }
            return x;
        }

        public  boolean isEmpty() {
            return _len == 0;
        }
        public void addAll(ArrayList<E> ary) {
           for(E n: ary) push(n);
        }

        boolean on(E x) {return _on.get(x._nid);}

        // FIFO
        BB pop() {
            if (_len == 0) return null;
            BB x = _es[0];

            for (int i = 1; i < _len; i++) {
                _es[i - 1] = _es[i];
            }

            _len--;

            _on.clear(x._nid);
            return x;
        }

        public void clear() {
            _len = 0;
            _on.clear();
            _totalWork = 0;
        }

    }

    public static class WorkListI<E extends Instr> {
        private Instr[] _es;
        private int _len;
        private final BitSet _on;
        private long _totalWork = 0;

        WorkListI() {
            _es = new Instr[1];
            _len = 0;
            _on = new BitSet();
        }

        public E push( E x ) {
            if( x==null ) return null;
            int idx = x._nid;
            if( !_on.get(idx) ) {
                _on.set(idx);
                // _len * 2
                if( _len==_es.length )
                    _es = Arrays.copyOf(_es,_len<<1);
                _es[_len++] = x;
                _totalWork++;
            }
            return x;
        }

        public  boolean isEmpty() {
            return _len == 0;
        }
        public void addAll(ArrayList<E> ary) {
            for(E n: ary) push(n);
        }

        boolean on(E x) {return _on.get(x._nid);}

        // FIFO
        Instr pop() {
            if (_len == 0) return null;
            Instr x = _es[0];

            for (int i = 1; i < _len; i++) {
                _es[i - 1] = _es[i];
            }

            _len--;

            _on.clear(x._nid);
            return x;
        }

        public void clear() {
            _len = 0;
            _on.clear();
            _totalWork = 0;
        }

    }
}
