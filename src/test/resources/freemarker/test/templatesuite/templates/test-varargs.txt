<#setting number_format="#">

${m.bar()} == 0
${m.bar([])} == 0
${m.bar(11)} == 11
${m.bar(null, 11)} == 11
${m.bar(11, 22)} == 1122
${m.bar(11.6, 22.4)} == 1122
${m.bar(11, 22, 33)} == 112233
${m.bar([11, 22, 33])} == 112233

${m.bar2(11, [22, 33, 44])} == -22334411
${m.bar2(11, 22, 33)} == -223311
${m.bar2(11, 22)} == -2211
${m.bar2(11)} == -11

${m.overloaded()} == 0
${m.overloaded(11)} == -11
${m.overloaded(11, 22)} == 1122
${m.overloaded(11, 22, 33)} == -112233
${m.overloaded(11, 22, 33, 44)} == -11223344
${m.overloaded([11, 22, 33, 44, 55])} == -1122334455

${m.overloaded(11, 22)} == 1122
${m.overloaded([11, 22])} == -1122

${m.noVarArgs("string", true, 123, 1000000?number_to_date)} == string, true, 123, 1000000
