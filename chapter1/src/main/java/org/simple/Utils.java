package org.simple;

import java.util.ArrayList;

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
}
