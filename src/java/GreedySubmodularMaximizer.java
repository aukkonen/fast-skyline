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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class GreedySubmodularMaximizer
{
    public GreedySubmodularMaximizer() {}

    public int[] findMax( int k, int numItems, SetFunction f )
    {
        PriorityQueue<ItemGainPair> mg = new PriorityQueue<>( new Comparator<ItemGainPair>() {
                public int compare( ItemGainPair a, ItemGainPair b ) {
                    // a < b if a.gain > b.gain
                    return (int)Math.signum( b.gain - a.gain );
                }
            } );

        for ( int item = 0; item < numItems; item++ ) {
            mg.add( new ItemGainPair( item, f.value( item ) ) );
        }

        List<Integer> solution = new ArrayList<Integer>();

        ItemGainPair best = mg.poll();

        solution.add( best.item );
        double fncValue = best.gain;
        System.err.printf( "GreedySubmodularMaximizer: fncValue = %.2f\n", fncValue );
        best.gain = 0;

        // construct a partial solution that we can use to evaluate f
        int[] partial = getPartialSolution( solution );

        while( solution.size() < k ) {
            ItemGainPair top = mg.poll();
            if ( top.gain <= best.gain ) {
                // best candidate we have can be at most as good as current best,
                // let's insert current best into solution.
                solution.add( best.item );
                fncValue += best.gain;
                System.err.printf( "GreedySubmodularMaximizer: fncValue = %.2f\n", fncValue );
                // re-insert top into mg as it may be used later
                mg.add( top );
                // current best no longer has any marginal gain
                best.gain = 0;
                // update partial so that it matches the new solution
                partial = getPartialSolution( solution );
            }
            else {
                // compute actual gain of topmost candidate given current solution.
                partial[ partial.length - 1 ] = top.item;
                top.gain = f.value( partial ) - fncValue;
                if ( top.gain > best.gain ) {
                    // top is the new best, old best must go back into mg
                    mg.add( best );
                    best = top;
                }
                else {
                    // re-insert top back into mg for later use
                    mg.add( top );
                }
            }
        }

        return Utils.intListToArray( solution );
    }

    private int[] getPartialSolution( List<Integer> solution )
    {
        int[] set = new int[ solution.size() + 1 ];
        for ( int i = 0; i < set.length-1; i++ ) {
            set[i] = solution.get(i);
        }
        return set;
    }

    private class ItemGainPair
    {
        private int    item;
        private double gain;
        
        public ItemGainPair( int item, double gain )
        {
            this.item = item;
            this.gain = gain;
        }
    }
}
