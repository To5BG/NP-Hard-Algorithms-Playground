% Model for a scheduling problem using 2-D matrices of 0..1 variables.

% For this model, we use the following terminology:
%  - A time point is a single notch on the time line
%  - A time step is an interval of time. The convention is that time step s
%    lies between time points s-1 and s (i.e. time step s ends at time 
%    point s).
%
% This graphic illustrates this more clearly:
%
% Timeline:       +-----+-----+
%                 |     |     |
% Time point:     0     1     2
% Time step:         1     2


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Parameters
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
int: CAPACITY;                                     % the capacity of the resource

int: N_TASKS;                                      % the number of tasks
set of int: Tasks = 1..N_TASKS;                    % the tasks to schedule
array[Tasks] of int: d;                            % d[i] is the number of time steps required by task i
array[Tasks] of int: rr;                           % rr[i] is the resource requirement of task i
array[Tasks] of set of int: suc;                   % suc[i] is the set of tasks that can only start after i has completed

int: T_MAX = sum(d);                               % the last time step
set of int: TimePoints = 0..T_MAX;                 % a time point on our time line
set of int: TimeSteps = 1..T_MAX;                  % time step s is between time points s-1 and s


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Variables
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
array[1..N_TASKS, TimePoints] of var 0..1 : S;     % S[i, t] = 1 iff task i starts at time point t
array[1..N_TASKS, TimeSteps] of var 0..1 : X;      % X[i, t] = 1 iff task i is running at time point t

array[TimePoints] of var 0..1 : S_sink;            % S_sink[t] = 1 iff the sink node starts at time point t
var TimePoints: makespan;                          % the start time of the sink node


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Solve item
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
solve minimize makespan;


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Constraints
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% each task has one starting time
constraint forall(i in Tasks) (sum (j in TimePoints) (S[i, j]) == 1);

% each task has a starting time such that it ends at T_MAX at the latest (tasks cannot be partially scheduled)
constraint forall(i in Tasks) (not exists (j in TimePoints) (S[i, j] == 1 /\ j + d[i] > T_MAX));

% each task i is active from its starting time point t until time  point t + d[i]
constraint forall(i in Tasks) (exists (z in TimePoints) (S[i,z] == 1 /\
  sum (zz in 1..d[i]) (X[i, z + zz]) == d[i]));

% tasks cannot start before the tasks they depend on are finished (the precendence constraint)
constraint forall(i in Tasks) (forall (j in suc[i]) (not exists (t in TimePoints, tt in TimePoints)
  (S[i, t] == 1 /\ S[j, tt] == 1 /\ t + d[i] > tt)));

% the capacity of the resource can never be exceeded
constraint forall (i in TimeSteps) (sum (j in Tasks) (X[j, i] * rr[j]) <= CAPACITY);

%% Sink constraints

% there is only one start time for the sink
constraint sum(S_sink) == 1;

% the sink task can only start after all tasks are finished
constraint forall (i in Tasks) (not exists (j in TimePoints) (S[i, j] == 1 /\ j + d[i] > makespan));

% the objective is the start time of the sink node
constraint exists (i in TimePoints) (makespan == i /\ S_sink[i] == 1);


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Output
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
output[show(makespan)];