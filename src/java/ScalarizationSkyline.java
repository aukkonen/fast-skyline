// The MIT License (MIT)

// Copyright (c) 2016 Antti Ukkonen

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

import java.util.List;
import java.util.LinkedList;
import java.util.Map;

public class ScalarizationSkyline extends SkylineAlg
{
    private double[] submWeight = new double[] {0.0, 0.5, 1.0}; // default
    
    public ScalarizationSkyline() {}

    public void configure( Map<String,Object> config )
    {
        if ( config.containsKey( "submWeight" ) ) {
            this.submWeight = (double[])config.get( "submWeight" );
            System.err.println( java.util.Arrays.toString( this.submWeight ) );
        }
    }

    public List<SkylinePoint> computeSkyline( int k, Evaluator eval )
    {
        GreedySubmodularMaximizer greedy = new GreedySubmodularMaximizer();
        List<SkylinePoint> skyline = new LinkedList<>();

        for ( int i = 0; i < this.submWeight.length; i++ ) {
            SetFunction f = getFunction( this.submWeight[i], eval, k );
            int[] solution = greedy.findMax( k, eval.numItems(), f );
            skyline.add( new SkylinePoint( eval.actualItems( solution ),
                                           eval.sumFnc( solution ),
                                           eval.submodularFnc( solution ) ) );
        }
        return skyline;
    }

    public SetFunction getFunction( double w, Evaluator eval, int k )
    {
        if ( eval.maxSum() ) {
            return new SetFunction() {
                public double value( int item ) {
                    return eval.sumFnc( item ) + w*eval.submodularFnc( item );
                }
                
                public double value( int[] set ) {
                    return eval.sumFnc( set ) + w*eval.submodularFnc( set );
                }
            };
        }
        else {
            // we are minimizing the sum function, this means
            // that the Evaluator has sorted item weights in increasing order.
            double m = 0.0;
            for ( int i = eval.numItems()-1; i >= eval.numItems()-k; i-- ) {
                m += eval.sumFnc( i );
            }
            final double M = m;
            return new SetFunction() {
                public double value( int item ) {
                    return (M-eval.sumFnc( item )) + w*eval.submodularFnc( item );
                }
                
                public double value( int[] set ) {
                    return (M-eval.sumFnc( set )) + w*eval.submodularFnc( set );
                }
            };
        }
    }
}
