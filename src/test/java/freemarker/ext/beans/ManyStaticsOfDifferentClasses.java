/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.ext.beans;

// FMPP template source:
// ----
// package freemarker.ext.beans;
// 
// // FMPP template source:
// // ----
// <#assign src><#include "ManyStaticsOfDifferentClasses.java.ftl" parse=false></#assign>
// ${src?trim?replace('(.*\r?\n)|.+', r'// $0', 'r')}
// // ----
// 
// <#assign MAX_CLA = 8 - 1>
// <#assign MAX_MET = 10 - 1>
// <#assign MAX_PRO = 10 - 1>
// 
// public class ManyStaticsOfDifferentClasses {
// 
//     private ManyStaticsOfDifferentClasses() { }
// 
//     <#list 0..MAX_CLA as claI>
//     static public class C${claI} {
//         <#list 0..MAX_PRO as proI>
//         public static int p${proI} = ${claI} * 1000 + ${proI};
//         </#list>
//         <#list 0..MAX_MET as metI>
//         public static int m${metI}() { return ${claI} * 1000 + ${metI}; };
//         </#list>
//     }
//     </#list>
//     
// }
// ----


public class ManyStaticsOfDifferentClasses {

    private ManyStaticsOfDifferentClasses() { }

    static public class C0 {
        public static int p0 = 0 * 1000 + 0;
        public static int p1 = 0 * 1000 + 1;
        public static int p2 = 0 * 1000 + 2;
        public static int p3 = 0 * 1000 + 3;
        public static int p4 = 0 * 1000 + 4;
        public static int p5 = 0 * 1000 + 5;
        public static int p6 = 0 * 1000 + 6;
        public static int p7 = 0 * 1000 + 7;
        public static int p8 = 0 * 1000 + 8;
        public static int p9 = 0 * 1000 + 9;
        public static int m0() { return 0 * 1000 + 0; };
        public static int m1() { return 0 * 1000 + 1; };
        public static int m2() { return 0 * 1000 + 2; };
        public static int m3() { return 0 * 1000 + 3; };
        public static int m4() { return 0 * 1000 + 4; };
        public static int m5() { return 0 * 1000 + 5; };
        public static int m6() { return 0 * 1000 + 6; };
        public static int m7() { return 0 * 1000 + 7; };
        public static int m8() { return 0 * 1000 + 8; };
        public static int m9() { return 0 * 1000 + 9; };
    }
    static public class C1 {
        public static int p0 = 1 * 1000 + 0;
        public static int p1 = 1 * 1000 + 1;
        public static int p2 = 1 * 1000 + 2;
        public static int p3 = 1 * 1000 + 3;
        public static int p4 = 1 * 1000 + 4;
        public static int p5 = 1 * 1000 + 5;
        public static int p6 = 1 * 1000 + 6;
        public static int p7 = 1 * 1000 + 7;
        public static int p8 = 1 * 1000 + 8;
        public static int p9 = 1 * 1000 + 9;
        public static int m0() { return 1 * 1000 + 0; };
        public static int m1() { return 1 * 1000 + 1; };
        public static int m2() { return 1 * 1000 + 2; };
        public static int m3() { return 1 * 1000 + 3; };
        public static int m4() { return 1 * 1000 + 4; };
        public static int m5() { return 1 * 1000 + 5; };
        public static int m6() { return 1 * 1000 + 6; };
        public static int m7() { return 1 * 1000 + 7; };
        public static int m8() { return 1 * 1000 + 8; };
        public static int m9() { return 1 * 1000 + 9; };
    }
    static public class C2 {
        public static int p0 = 2 * 1000 + 0;
        public static int p1 = 2 * 1000 + 1;
        public static int p2 = 2 * 1000 + 2;
        public static int p3 = 2 * 1000 + 3;
        public static int p4 = 2 * 1000 + 4;
        public static int p5 = 2 * 1000 + 5;
        public static int p6 = 2 * 1000 + 6;
        public static int p7 = 2 * 1000 + 7;
        public static int p8 = 2 * 1000 + 8;
        public static int p9 = 2 * 1000 + 9;
        public static int m0() { return 2 * 1000 + 0; };
        public static int m1() { return 2 * 1000 + 1; };
        public static int m2() { return 2 * 1000 + 2; };
        public static int m3() { return 2 * 1000 + 3; };
        public static int m4() { return 2 * 1000 + 4; };
        public static int m5() { return 2 * 1000 + 5; };
        public static int m6() { return 2 * 1000 + 6; };
        public static int m7() { return 2 * 1000 + 7; };
        public static int m8() { return 2 * 1000 + 8; };
        public static int m9() { return 2 * 1000 + 9; };
    }
    static public class C3 {
        public static int p0 = 3 * 1000 + 0;
        public static int p1 = 3 * 1000 + 1;
        public static int p2 = 3 * 1000 + 2;
        public static int p3 = 3 * 1000 + 3;
        public static int p4 = 3 * 1000 + 4;
        public static int p5 = 3 * 1000 + 5;
        public static int p6 = 3 * 1000 + 6;
        public static int p7 = 3 * 1000 + 7;
        public static int p8 = 3 * 1000 + 8;
        public static int p9 = 3 * 1000 + 9;
        public static int m0() { return 3 * 1000 + 0; };
        public static int m1() { return 3 * 1000 + 1; };
        public static int m2() { return 3 * 1000 + 2; };
        public static int m3() { return 3 * 1000 + 3; };
        public static int m4() { return 3 * 1000 + 4; };
        public static int m5() { return 3 * 1000 + 5; };
        public static int m6() { return 3 * 1000 + 6; };
        public static int m7() { return 3 * 1000 + 7; };
        public static int m8() { return 3 * 1000 + 8; };
        public static int m9() { return 3 * 1000 + 9; };
    }
    static public class C4 {
        public static int p0 = 4 * 1000 + 0;
        public static int p1 = 4 * 1000 + 1;
        public static int p2 = 4 * 1000 + 2;
        public static int p3 = 4 * 1000 + 3;
        public static int p4 = 4 * 1000 + 4;
        public static int p5 = 4 * 1000 + 5;
        public static int p6 = 4 * 1000 + 6;
        public static int p7 = 4 * 1000 + 7;
        public static int p8 = 4 * 1000 + 8;
        public static int p9 = 4 * 1000 + 9;
        public static int m0() { return 4 * 1000 + 0; };
        public static int m1() { return 4 * 1000 + 1; };
        public static int m2() { return 4 * 1000 + 2; };
        public static int m3() { return 4 * 1000 + 3; };
        public static int m4() { return 4 * 1000 + 4; };
        public static int m5() { return 4 * 1000 + 5; };
        public static int m6() { return 4 * 1000 + 6; };
        public static int m7() { return 4 * 1000 + 7; };
        public static int m8() { return 4 * 1000 + 8; };
        public static int m9() { return 4 * 1000 + 9; };
    }
    static public class C5 {
        public static int p0 = 5 * 1000 + 0;
        public static int p1 = 5 * 1000 + 1;
        public static int p2 = 5 * 1000 + 2;
        public static int p3 = 5 * 1000 + 3;
        public static int p4 = 5 * 1000 + 4;
        public static int p5 = 5 * 1000 + 5;
        public static int p6 = 5 * 1000 + 6;
        public static int p7 = 5 * 1000 + 7;
        public static int p8 = 5 * 1000 + 8;
        public static int p9 = 5 * 1000 + 9;
        public static int m0() { return 5 * 1000 + 0; };
        public static int m1() { return 5 * 1000 + 1; };
        public static int m2() { return 5 * 1000 + 2; };
        public static int m3() { return 5 * 1000 + 3; };
        public static int m4() { return 5 * 1000 + 4; };
        public static int m5() { return 5 * 1000 + 5; };
        public static int m6() { return 5 * 1000 + 6; };
        public static int m7() { return 5 * 1000 + 7; };
        public static int m8() { return 5 * 1000 + 8; };
        public static int m9() { return 5 * 1000 + 9; };
    }
    static public class C6 {
        public static int p0 = 6 * 1000 + 0;
        public static int p1 = 6 * 1000 + 1;
        public static int p2 = 6 * 1000 + 2;
        public static int p3 = 6 * 1000 + 3;
        public static int p4 = 6 * 1000 + 4;
        public static int p5 = 6 * 1000 + 5;
        public static int p6 = 6 * 1000 + 6;
        public static int p7 = 6 * 1000 + 7;
        public static int p8 = 6 * 1000 + 8;
        public static int p9 = 6 * 1000 + 9;
        public static int m0() { return 6 * 1000 + 0; };
        public static int m1() { return 6 * 1000 + 1; };
        public static int m2() { return 6 * 1000 + 2; };
        public static int m3() { return 6 * 1000 + 3; };
        public static int m4() { return 6 * 1000 + 4; };
        public static int m5() { return 6 * 1000 + 5; };
        public static int m6() { return 6 * 1000 + 6; };
        public static int m7() { return 6 * 1000 + 7; };
        public static int m8() { return 6 * 1000 + 8; };
        public static int m9() { return 6 * 1000 + 9; };
    }
    static public class C7 {
        public static int p0 = 7 * 1000 + 0;
        public static int p1 = 7 * 1000 + 1;
        public static int p2 = 7 * 1000 + 2;
        public static int p3 = 7 * 1000 + 3;
        public static int p4 = 7 * 1000 + 4;
        public static int p5 = 7 * 1000 + 5;
        public static int p6 = 7 * 1000 + 6;
        public static int p7 = 7 * 1000 + 7;
        public static int p8 = 7 * 1000 + 8;
        public static int p9 = 7 * 1000 + 9;
        public static int m0() { return 7 * 1000 + 0; };
        public static int m1() { return 7 * 1000 + 1; };
        public static int m2() { return 7 * 1000 + 2; };
        public static int m3() { return 7 * 1000 + 3; };
        public static int m4() { return 7 * 1000 + 4; };
        public static int m5() { return 7 * 1000 + 5; };
        public static int m6() { return 7 * 1000 + 6; };
        public static int m7() { return 7 * 1000 + 7; };
        public static int m8() { return 7 * 1000 + 8; };
        public static int m9() { return 7 * 1000 + 9; };
    }
    
}
