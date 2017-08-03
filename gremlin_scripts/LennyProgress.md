# August 2nd Meeting
**Goal:** Find linear sequences with *Uniform Mutation* and *Closed Mutation*.
There are two other types of mutation: *alteration*, and *[alteration: uniform mutation]*.
Alteration's are interesting when an individual does alteration with itself, the following results can look like:
* Elitism, if the child looks like its parent.
* Mutation, if the *reshuffling* of the child is descently close to its parent.
* Crossover, if the *reshuflling* is nothing like its parent.

We will need to decide in what cases are alterations a mutation or not, and whether they should be part of a linear sequence that needs to be collapsed.

### Subgraphs
Doing operations on the entire db would take to long, to fix this we are going to make a subgraph of the ancestory tree of the winning individuals.
Here is how do this:
1. In the github repo, switch to the **Additional Gene FIltering** or **Genes as Nodes** branch, and go into `gremlin_Scripts/WinnersWithPercents.groovy`.
2. In gremin, input `winners`, and you should get an individual back.
  ``` gremlin> winners   ==>v[758399192]```.  
3. sfg
