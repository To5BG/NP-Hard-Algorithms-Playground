int: N;

array[1..N] of var 1..N: comb;

constraint forall(i in 1..N-1) (
  forall (j in i+1..N) (comb[i] != comb[j])
);

solve satisfy;