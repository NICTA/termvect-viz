#! /bin/bash

# Data preparation
# ----------------

# Testing with the "20 Newsgroups" data set from: http://qwone.com/~jason/20Newsgroups/
# using data from the "20news-bydate.tar.gz" link.
#
# Using just the 3 news groups chosen in:
#   "Exemplar-based Visualization of Large Document Corpus", Yanhua Chen et. al.
#   http://www.cs.wayne.edu/~mdong/tvcg09.pdf
# as covering very different tropics: comp.sys.ibm.pc.hardware, rec.sport.baseball, sci.med

## shuffled list of all posts:
# find data/20news-18828/ -type f | sort -R > newsFiles.txt
## 2000 posts to our chosen 3 new-groups:
# egrep 'comp.sys.ibm.pc.hardware|rec.sport.baseball|sci.med' newsFiles.txt | head -2000 > newsFiles3-2000.txt

# I think news groups entries should be ASCII, but 20news-18828/rec.sport.hockey/54769 contains a 0xfe char twice,
# which is invalid as both ASCII and UTF-8. The software now discards any invalid chars without warning.


# Term Vectors
# ------------

# term matrix = all term vectors for all docs in the corpus
# Generate a term matrix for each combination of 4 tf formulae with 4 idf formulae.

TERM_VECT='time java -jar ../target/termvect-0.1-SNAPSHOT.one-jar.jar'

for (( tfCalc=0; tfCalc <= 3; ++tfCalc )); do
  for (( idfCalc=0; idfCalc <= 3; ++idfCalc )); do
    DIR=tfCalc${tfCalc}idfCalc${idfCalc}
    mkdir -p $DIR
    OPTS="--indexDir index --minDocFreq 20 --maxDocFreq 1000 --excludeTerm '^[0-9.,-]+$' --tfCalc $tfCalc --idfCalc $idfCalc --terms ${DIR}/terms"
    if [[ $tfCalc = 0 && $idfCalc = 0 ]]
      then OPTS="--docList newsFiles3-2000.txt --corpus corpus --paths paths $OPTS" # create index on 1st run then reuse
    fi
    echo "tfCalc = $tfCalc, idfCalc = $idfCalc, OPTS = $OPTS"
    $TERM_VECT $OPTS
  done
done


# tSNE
# ----

# For each term matrix generated above, run bh_tsne with 4 different perplexity values
# and plot the results.

TSNE=../../bh_tsne/bh_tsne
PLOT_TSNE=../src/main/scripts/plot.py

for perp in 6.0 3.0 1.0 30.0
do
  for (( tfCalc=0; tfCalc <= 3; ++tfCalc )); do
    for (( idfCalc=0; idfCalc <= 3; ++idfCalc )); do
      DIR=tfCalc${tfCalc}idfCalc${idfCalc}
      OUT=${DIR}/perp${perp}
      echo "OUT = $OUT"

      $TSNE 0.001 $perp 500 < ${DIR}/terms > ${OUT}.out
      # extract matrix, exclude 1st 2 lines
      sed 1,2d ${OUT}.out > ${OUT}.matrix
      # Add col for news-group name from 'paths' file.
      awk -F/ '{print $3}' paths | paste -d ' ' ${OUT}.matrix - > ${OUT}.matrix.withGroup

      # Partition matrix by news-group name into separate files. Writes files named after each news-group.
      rm -f comp.* rec.* sci.* # next line appends to these files, so must start empty or non-existant
      awk '{ print $1 " " $2 >> $3 }' ${OUT}.matrix.withGroup
      $PLOT_TSNE ${OUT}.png sci.* rec.* comp.*
    done
  done
done


# Cluster Evaluation
# ------------------

# Although we're arranging docs by similarity, not really clustering, evaluate the results by how well
# the 3 news-groups have been clustered.
# Find the parameters (tfCalc, idfCalc and perplexity) that produce the optimum (minimum) value of the
# clustering metric, and plot how varying each parameter individually around this optimum affects its value.

CLUST_EVAL='time java -jar ../../clusteval/target/clusteval-0.1-SNAPSHOT.one-jar.jar'
PLOT_CEVAL=../../clusteval/src/main/scripts/plot.py

$CLUST_EVAL `find . -name \*.withGroup` > out
sed -e 's/.*tfCalc//' -e 's/idfCalc/ /' -e 's/[/]perp/ /' -e 's/.matrix.withGroup//' out > clustEval.results
$PLOT_CEVAL clustEval.png clustEval.results

# Result
# ------

# Optimal values are: tfCalc=3, idfCalc=0, perplexity=6.0
# Further improvement may be possible with values of perplexity around 6.0, however it is claimed that tSNE is
# insensitive to this parameter.