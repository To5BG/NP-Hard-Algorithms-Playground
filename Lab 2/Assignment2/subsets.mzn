int: N;

array[1..N] of var 0..1: bitstr;

var set of int: ans = {j | j in 1..N where bitstr[j] == 1};

solve satisfy;
