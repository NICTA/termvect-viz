# t3as - Cluster Evaluation

Compute a clustering quality metric:

	(average distance between pairs of documents in same class) / (average distance between pairs of documents in different classes).

# Building

Just run maven:

	mvn

# Running

	java -jar  clusteval/target/clusteval-0.1-SNAPSHOT.one-jar.jar files ...

See the script `src/main/scripts/run.sh` in the `termvect` project to see how this was used to evaluate experiments using the 20NewsGroups data set.

## Inputs

Each input file represents a set of data points or vectors, one per line.
An additional column at the end provides the class label.

## Outputs

For each input file, a single number, the clustering quality metric defined above, is written to stdout.

