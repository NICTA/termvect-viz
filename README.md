# t3as - Term Vector Visualization

This directory contains 3 related projects:

- bh_tsne: minor modifications to software from http://homepage.tudelft.nl/19j49/t-SNE.html
- termvect: generates term vectors for all documents in a corpus (as input for bh_tsne)
- clusteval: evaluates a cluster quality metric for labeled documents

# Building

See the README for each project.

# Running

See the script `termvect/src/main/scripts/run.sh` for experiments using the 20NewsGroups data set.

# Results

See `clusteval/3NewsGroups.pdf` for results with 3 news-groups from the 20NewsGroups data set.
