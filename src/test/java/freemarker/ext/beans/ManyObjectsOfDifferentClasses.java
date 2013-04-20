package freemarker.ext.beans;

// FMPP template source:
// ----
// package freemarker.ext.beans;
// 
// // FMPP template source:
// // ----
// <#assign src><#include "ManyObjectsOfDifferentClasses.java.ftl" parse=false></#assign>
// ${src?trim?replace('(.*\r?\n)|.+', r'// $0', 'r')}
// // ----
// 
// <#assign MAX_CLA = 8 - 1>
// <#assign MAX_MET = 10 - 1>
// <#assign MAX_PRO = 10 - 1>
// 
// public class ManyObjectsOfDifferentClasses {
// 
//     public static final Object[] OBJECTS = new Object[] {
//         <#list 0..MAX_CLA as claI>
//         new C${claI}(),
//         </#list>
//     };
// 
//     <#list 0..MAX_CLA as claI>
//     static public class C${claI} {
//         <#list 0..MAX_PRO as proI>
//         public int getP${proI}() { return ${claI} * 1000 + ${proI}; }
//         </#list>
//         <#list 0..MAX_MET as metI>
//         public int m${metI}() { return ${claI} * 1000 + ${metI}; };
//         </#list>
//     }
//     </#list>
//     
// }
// ----


public class ManyObjectsOfDifferentClasses {

    public static final Object[] OBJECTS = new Object[] {
        new C0(),
        new C1(),
        new C2(),
        new C3(),
        new C4(),
        new C5(),
        new C6(),
        new C7(),
    };

    static public class C0 {
        public int getP0() { return 0 * 1000 + 0; }
        public int getP1() { return 0 * 1000 + 1; }
        public int getP2() { return 0 * 1000 + 2; }
        public int getP3() { return 0 * 1000 + 3; }
        public int getP4() { return 0 * 1000 + 4; }
        public int getP5() { return 0 * 1000 + 5; }
        public int getP6() { return 0 * 1000 + 6; }
        public int getP7() { return 0 * 1000 + 7; }
        public int getP8() { return 0 * 1000 + 8; }
        public int getP9() { return 0 * 1000 + 9; }
        public int m0() { return 0 * 1000 + 0; };
        public int m1() { return 0 * 1000 + 1; };
        public int m2() { return 0 * 1000 + 2; };
        public int m3() { return 0 * 1000 + 3; };
        public int m4() { return 0 * 1000 + 4; };
        public int m5() { return 0 * 1000 + 5; };
        public int m6() { return 0 * 1000 + 6; };
        public int m7() { return 0 * 1000 + 7; };
        public int m8() { return 0 * 1000 + 8; };
        public int m9() { return 0 * 1000 + 9; };
    }
    static public class C1 {
        public int getP0() { return 1 * 1000 + 0; }
        public int getP1() { return 1 * 1000 + 1; }
        public int getP2() { return 1 * 1000 + 2; }
        public int getP3() { return 1 * 1000 + 3; }
        public int getP4() { return 1 * 1000 + 4; }
        public int getP5() { return 1 * 1000 + 5; }
        public int getP6() { return 1 * 1000 + 6; }
        public int getP7() { return 1 * 1000 + 7; }
        public int getP8() { return 1 * 1000 + 8; }
        public int getP9() { return 1 * 1000 + 9; }
        public int m0() { return 1 * 1000 + 0; };
        public int m1() { return 1 * 1000 + 1; };
        public int m2() { return 1 * 1000 + 2; };
        public int m3() { return 1 * 1000 + 3; };
        public int m4() { return 1 * 1000 + 4; };
        public int m5() { return 1 * 1000 + 5; };
        public int m6() { return 1 * 1000 + 6; };
        public int m7() { return 1 * 1000 + 7; };
        public int m8() { return 1 * 1000 + 8; };
        public int m9() { return 1 * 1000 + 9; };
    }
    static public class C2 {
        public int getP0() { return 2 * 1000 + 0; }
        public int getP1() { return 2 * 1000 + 1; }
        public int getP2() { return 2 * 1000 + 2; }
        public int getP3() { return 2 * 1000 + 3; }
        public int getP4() { return 2 * 1000 + 4; }
        public int getP5() { return 2 * 1000 + 5; }
        public int getP6() { return 2 * 1000 + 6; }
        public int getP7() { return 2 * 1000 + 7; }
        public int getP8() { return 2 * 1000 + 8; }
        public int getP9() { return 2 * 1000 + 9; }
        public int m0() { return 2 * 1000 + 0; };
        public int m1() { return 2 * 1000 + 1; };
        public int m2() { return 2 * 1000 + 2; };
        public int m3() { return 2 * 1000 + 3; };
        public int m4() { return 2 * 1000 + 4; };
        public int m5() { return 2 * 1000 + 5; };
        public int m6() { return 2 * 1000 + 6; };
        public int m7() { return 2 * 1000 + 7; };
        public int m8() { return 2 * 1000 + 8; };
        public int m9() { return 2 * 1000 + 9; };
    }
    static public class C3 {
        public int getP0() { return 3 * 1000 + 0; }
        public int getP1() { return 3 * 1000 + 1; }
        public int getP2() { return 3 * 1000 + 2; }
        public int getP3() { return 3 * 1000 + 3; }
        public int getP4() { return 3 * 1000 + 4; }
        public int getP5() { return 3 * 1000 + 5; }
        public int getP6() { return 3 * 1000 + 6; }
        public int getP7() { return 3 * 1000 + 7; }
        public int getP8() { return 3 * 1000 + 8; }
        public int getP9() { return 3 * 1000 + 9; }
        public int m0() { return 3 * 1000 + 0; };
        public int m1() { return 3 * 1000 + 1; };
        public int m2() { return 3 * 1000 + 2; };
        public int m3() { return 3 * 1000 + 3; };
        public int m4() { return 3 * 1000 + 4; };
        public int m5() { return 3 * 1000 + 5; };
        public int m6() { return 3 * 1000 + 6; };
        public int m7() { return 3 * 1000 + 7; };
        public int m8() { return 3 * 1000 + 8; };
        public int m9() { return 3 * 1000 + 9; };
    }
    static public class C4 {
        public int getP0() { return 4 * 1000 + 0; }
        public int getP1() { return 4 * 1000 + 1; }
        public int getP2() { return 4 * 1000 + 2; }
        public int getP3() { return 4 * 1000 + 3; }
        public int getP4() { return 4 * 1000 + 4; }
        public int getP5() { return 4 * 1000 + 5; }
        public int getP6() { return 4 * 1000 + 6; }
        public int getP7() { return 4 * 1000 + 7; }
        public int getP8() { return 4 * 1000 + 8; }
        public int getP9() { return 4 * 1000 + 9; }
        public int m0() { return 4 * 1000 + 0; };
        public int m1() { return 4 * 1000 + 1; };
        public int m2() { return 4 * 1000 + 2; };
        public int m3() { return 4 * 1000 + 3; };
        public int m4() { return 4 * 1000 + 4; };
        public int m5() { return 4 * 1000 + 5; };
        public int m6() { return 4 * 1000 + 6; };
        public int m7() { return 4 * 1000 + 7; };
        public int m8() { return 4 * 1000 + 8; };
        public int m9() { return 4 * 1000 + 9; };
    }
    static public class C5 {
        public int getP0() { return 5 * 1000 + 0; }
        public int getP1() { return 5 * 1000 + 1; }
        public int getP2() { return 5 * 1000 + 2; }
        public int getP3() { return 5 * 1000 + 3; }
        public int getP4() { return 5 * 1000 + 4; }
        public int getP5() { return 5 * 1000 + 5; }
        public int getP6() { return 5 * 1000 + 6; }
        public int getP7() { return 5 * 1000 + 7; }
        public int getP8() { return 5 * 1000 + 8; }
        public int getP9() { return 5 * 1000 + 9; }
        public int m0() { return 5 * 1000 + 0; };
        public int m1() { return 5 * 1000 + 1; };
        public int m2() { return 5 * 1000 + 2; };
        public int m3() { return 5 * 1000 + 3; };
        public int m4() { return 5 * 1000 + 4; };
        public int m5() { return 5 * 1000 + 5; };
        public int m6() { return 5 * 1000 + 6; };
        public int m7() { return 5 * 1000 + 7; };
        public int m8() { return 5 * 1000 + 8; };
        public int m9() { return 5 * 1000 + 9; };
    }
    static public class C6 {
        public int getP0() { return 6 * 1000 + 0; }
        public int getP1() { return 6 * 1000 + 1; }
        public int getP2() { return 6 * 1000 + 2; }
        public int getP3() { return 6 * 1000 + 3; }
        public int getP4() { return 6 * 1000 + 4; }
        public int getP5() { return 6 * 1000 + 5; }
        public int getP6() { return 6 * 1000 + 6; }
        public int getP7() { return 6 * 1000 + 7; }
        public int getP8() { return 6 * 1000 + 8; }
        public int getP9() { return 6 * 1000 + 9; }
        public int m0() { return 6 * 1000 + 0; };
        public int m1() { return 6 * 1000 + 1; };
        public int m2() { return 6 * 1000 + 2; };
        public int m3() { return 6 * 1000 + 3; };
        public int m4() { return 6 * 1000 + 4; };
        public int m5() { return 6 * 1000 + 5; };
        public int m6() { return 6 * 1000 + 6; };
        public int m7() { return 6 * 1000 + 7; };
        public int m8() { return 6 * 1000 + 8; };
        public int m9() { return 6 * 1000 + 9; };
    }
    static public class C7 {
        public int getP0() { return 7 * 1000 + 0; }
        public int getP1() { return 7 * 1000 + 1; }
        public int getP2() { return 7 * 1000 + 2; }
        public int getP3() { return 7 * 1000 + 3; }
        public int getP4() { return 7 * 1000 + 4; }
        public int getP5() { return 7 * 1000 + 5; }
        public int getP6() { return 7 * 1000 + 6; }
        public int getP7() { return 7 * 1000 + 7; }
        public int getP8() { return 7 * 1000 + 8; }
        public int getP9() { return 7 * 1000 + 9; }
        public int m0() { return 7 * 1000 + 0; };
        public int m1() { return 7 * 1000 + 1; };
        public int m2() { return 7 * 1000 + 2; };
        public int m3() { return 7 * 1000 + 3; };
        public int m4() { return 7 * 1000 + 4; };
        public int m5() { return 7 * 1000 + 5; };
        public int m6() { return 7 * 1000 + 6; };
        public int m7() { return 7 * 1000 + 7; };
        public int m8() { return 7 * 1000 + 8; };
        public int m9() { return 7 * 1000 + 9; };
    }
    
}
