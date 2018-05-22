## The MIT License (MIT)

## Copyright (c) 2015 Antti Ukkonen

## Permission is hereby granted, free of charge, to any person obtaining a copy
## of this software and associated documentation files (the "Software"), to deal
## in the Software without restriction, including without limitation the rights
## to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
## copies of the Software, and to permit persons to whom the Software is
## furnished to do so, subject to the following conditions:

## The above copyright notice and this permission notice shall be included in
## all copies or substantial portions of the Software.

## THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
## IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
## FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
## AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
## LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
## OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
## THE SOFTWARE.

require( rJava );
.jinit(classpath="../java/classes", parameters="-Xmx4G");
## To get R and Java 1.8 to work on OS X, check out:
## http://conjugateprior.org/2014/12/r-java8-osx/
##
## Essentially you must have Apple Java 1.6 installed,
## then run R CMD javareconf,
## then install rJava from source (install.packages( 'rJava', type='source')),
## and this should do the trick.
## Check with DYLD_PRINT_LIBRARIES=1 R
## to see that rJava was linked against the correct version of the jdk.

skyline <- function(k, eval, alg='fast', maxSum=FALSE,
                    qualityFn='', coverFn='',                     ## subgroup evaluator
                    graphFn='', edgeProb='', numSamples=200,      ## infmax evaluator
                    values='', numSets=0, univSize=0, scdata=0,   ## for setcover evaluator
                    maxSize=NA,                                   ## for fast and levelwise
                    submWeight=c(0.0, 0.5, 1.0),                  ## for scalar
                    intervals=10, numThreads=4, fastInit=FALSE    ## for exact parallel
                    ) {
    sl <- .jnew( 'Skyline' )

    .jcall( sl, 'V', 'setOption', 'e', eval )
    .jcall( sl, 'V', 'setOption', 'a', alg )

    ## This is for all evaluators.
    if ( maxSum ) {
        .jcall( sl, 'V', 'setOption', 'max', '' );
    }

    ## These are for the sgset and sgsetbit evaluators
    .jcall( sl, 'V', 'setOption', 'q', qualityFn )
    .jcall( sl, 'V', 'setOption', 'c', coverFn )
    
    ## These are for the infmax evaluator
    .jcall( sl, 'V', 'setOption', 'g', graphFn )
    .jcall( sl, 'V', 'setOption', 'p', as.character(edgeProb) )
    .jcall( sl, 'V', 'setOption', 's', as.character(numSamples) )

    ## These are for the setcover evaluator
    .jcall( sl, 'V', 'setOption', 'values', values ) ## these are set weights/scores
    .jcall( sl, 'V', 'setOption', 'scdata', as.integer(scdata) ) ## also coverts to vector
    .jcall( sl, 'V', 'setOption', 'univSize', as.character(univSize) )
    .jcall( sl, 'V', 'setOption', 'numSets', as.character(numSets) )

    ## These are for the parallel algorithm
    .jcall( sl, 'V', 'setOption', 'i', as.character(intervals) )
    .jcall( sl, 'V', 'setOption', 't', as.character(numThreads) )
    if ( fastInit ) {
        .jcall( sl, 'V', 'setOption', 'fastInit', '' )
    }

    ## This is for the fast and levelwise algorithms
    if ( !is.na( maxSize ) ) {
        .jcall( sl, 'V', 'setOption', 'f', as.character(maxSize) )
    }

    ## This is for the scalarization algorithm
    .jcall( sl, 'V', 'setOption', 'submWeight', submWeight )

    out <- .jcall( sl, '[D', 'run', as.integer(k) )
    out <- matrix( out, ncol=(k+2) )
    list( sum=out[,1], sub=out[,2], sets=out[,3:ncol(out)],
         sumCalls=as.numeric(.jcall( sl, 'S', 'getEvaluatorStat', 'sumCalls' )),
         submCalls=as.numeric(.jcall( sl, 'S', 'getEvaluatorStat', 'submCalls' )) )
}








## NO LONGER IN USE
##
## subgroupSkyline <- function( k, quality, cover, alg='fast' ) {
##     if ( length(quality) != ncol(cover) ) {
##         stop( sprintf( "Input dimensions don't match: len(quality) = %d, ncol(cover) = %d\n",
##                        length(quality), ncol(cover) ) )
##     }
##     jalg <- instantiate_alg( alg )
##     ## The skyline library by default tries to MINIMISE the sum function (quality).
##     ## However, for this application it must be maximised. We subtract all qualities
##     ## from the maximum quality for the optimizer, and convert back to the original
##     ## qualities before returning results.
##     maxqual <- max(quality)
##     jeval <- .jnew("SubgroupSetEvaluatorBitCover",
##                    maxqual-quality,
##                    as.integer(cover),
##                    as.integer(length(quality)),
##                    as.integer(length(cover)/length(quality)))
##     jsl <- .jcall( jalg, "[D", "computeSkylineR", as.integer(k), .jcast(jeval, "Evaluator") )
##     jsl <- matrix( jsl, ncol=(k+2) )
##     list( quality=k*maxqual-jsl[,1], diversity=jsl[,2], sets=jsl[,3:ncol(jsl)] )
## }
