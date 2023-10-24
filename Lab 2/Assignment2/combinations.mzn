int: N;
int: K;

array[1..K] of var 1..N: comb;

constraint forall(i in 1..K-1) (comb[i] < comb[i+1]);
