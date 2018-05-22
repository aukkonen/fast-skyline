// The MIT License (MIT)

// Copyright (c) 2017 Antti Ukkonen

// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:

// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

public class SeedSelector
{
    public SeedSelector() { }
    
    public int[] selectSeeds( int k, int[] i, int[] j, double[] prob,
                              int numItems, int numSamples )
    {
        System.err.printf( "SeedSelector: called with vectors of lengths %d, %d, %d\n",
                           i.length, j.length, prob.length );
        InfmaxEvaluator eval = new InfmaxEvaluator();
        eval.init( i, j, prob, numSamples );
        System.err.println( "SeedSelector: init done." );

        GreedySubmodularMaximizer gsm = new GreedySubmodularMaximizer();

        int[] seeds = gsm.findMax( k, numItems, new SetFunction() {
                public double value( int item ) {
                    return eval.submodularFnc( item );
                }
                public double value( int[] items ) {
                    return eval.submodularFnc( items );
                }
            } );

        // Finally, re-map seed identifiers to original ones in case
        // InfmaxEvaluator has changed these.
        int[] out = new int[ seeds.length ];
        for ( int idx = 0; idx < out.length; idx++ ) {
            out[idx] = eval.vertexIdToName( seeds[idx] );
        }

        return out;
    }
}
