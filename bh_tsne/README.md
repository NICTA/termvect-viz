# Barnes-Hut-SNE

This is a modified version of Barnes-Hut-SNE from http://homepage.tudelft.nl/19j49/t-SNE.html
(tarball link is http://homepage.tudelft.nl/19j49/t-SNE_files/bh_tsne.tar.gz).


## Build

File compile_linux (in the original distribution):

	echo "Make sure to change the path to CBLAS in this file before running it!" 
	g++ quadtree.cpp tsne.cpp -o bh_tsne -O3 -I./CBLAS/include -L./ -lcblas

21 Mar 2014:
After installing g++, I have files: /usr/include/cblas.h and /usr/lib/libblas.so so I compiled with:

	g++ quadtree.cpp tsne.cpp -o bh_tsne -O3 -lblas 

26 Jul 2019 update:
After: `sudo apt install libblas-dev` I have files: `/usr/include/x86_64-linux-gnu/cblas.h` and `/usr/lib/x86_64-linux-gnu/libblas.so`
Error from above compilation command:

    quadtree.h:41:34: error: ‘double abs(double)’ conflicts with a previous declaration
    ...
    /usr/include/c++/7/bits/std_abs.h:70:3: note: previous declaration ‘constexpr double std::abs(double)’

Commented out the definition of abs on line 41 and it compiles OK.

## Run

After the modifications described below, it is run like this:

	./bh_tsne 2.5 3.0 500 < in > out
	
where: 2.5 is the value for `theta` (permissable gradient error), 3.0 is the `perplexity` (similar to k in k-NN), 500 is `max_iter` and
`in` and `out` are the input and output files. The number of output dimensions is hard-coded to 2.

`in` contains whitespace separated input values (new lines treated the same as other whitespace):

 - N number of rows,
 - D number of columns,
 - then the data as N*D doubles.
 
`out` contains the output values:

 - first line: N number of rows (same as input),
 - second line: D number of columns = 2,
 - then the data as N lines of D doubles.

## Modifications

Modified tsne.cpp:
- changed IO from binary with hard-coded file names to text using stdin/out;
- removed landmarks and costs initialized in main, not passed to tsne.run(), output in save_data();
- changed other existing output to stdout (progress messages, errors) to stderr;
- take params: `theta`, `perplexity` and `max_iter` from command line args instead of `theta` and `perplexity` from second line of input and `max_iter` hard-coded to 1000;
- allocate TSNE on stack instead of heap.

## Notes

The following functions:

 - computeExactGradient
 - evaluateError
 - computeGaussianPerplexity

all contain:

	// Compute the squared Euclidean distance matrix
	double* DD = (double*) malloc(N * N * sizeof(double));
	...
	computeSquaredEuclideanDistance(X, N, D, DD);

computeSquaredEuclideanDistance computes:

	DD = -2 . D . D' + Sum D(i,c) + Sum D(j,c)

where

 - D is the input matrix
 - ' is transpose; and
 - the sums are over all cols for the rows i & j

which would appear to make the program O(N**2) in memory, however there are two versions of
evaluateError & computeGaussianPerplexity and as long as theta != .0
the functions that are O(N**2) in memory are not used. Instead the Barnes-Hut approximation
is used in the variants of these functions.



