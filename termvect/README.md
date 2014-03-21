# t3as - Term Vector

Create a Lucene index with term vectors, then use it to write a tf-idf matrix suitable as input to `bh_tsne`.

# Building

Just run maven:

	mvn

# Running

	java -jar target/termvect-0.1-SNAPSHOT.one-jar.jar --help

See the script `src/main/scripts/run.sh` for experiments using the 20NewsGroups data set.

## Inputs

Either specify:

 - `--docDir dir` and `--docSuffix suffix` to index all files under the tree rooted at `dir` that end with `suffix`; or
 - `--docList fileList` to specify a file containing the paths of the files to be indexed, one per line.
 
## Outputs

### -terms termsFile

The tf-idf matrix (preceded by its dimensions).
Each row in the matrix is a tf-idf term vector for one document and the rows are in index order.

### --paths pathsFile

Contains the paths of the indexed documents, also in index order.
The ordering of points is preserved by `bh_tsne`, so if class labels are associated with each file path, this can be used to attach the class labels to
the result of `bh_tsne` for evaluation.

### --corpus corpusFile

Contains a line for each term in the corpus, in lexical order. The line has the term, its document frequency and it's total occurrence frequency
(sum of term frequency over all documents).
This can be used to explore the distribution of terms, elimination of terms considered unhelpful for the problem at hand and elimination of
infrequent terms in order to reduce the size of the data for `bh_tsne`.

