// The MIT License (MIT)

// Copyright (c) 2015 Antti Ukkonen

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

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class ExactSkyline extends SkylineAlg
{
    public ExactSkyline() {}
    
    public List<SkylinePoint> computeSkyline( int k, Evaluator eval )
    {
        // init beginGenerators
        List<SearchTreeNode> beginGenerators = new LinkedList<SearchTreeNode>();
        beginGenerators.add( new SearchTreeNode( Utils.range(k), k-1 ) );

        return computeSkylineInterval( k, eval,
                                       getSubmBestObject( eval.maxSum() ),
                                       beginGenerators,
                                       new HashSet<SearchTreeNode>() );
    }

    public void configure( Map<String,Object> config ) {
        return;
    }

    protected List<SkylinePoint> computeSkylineInterval( int k,
                                                         Evaluator eval,
                                                         MutableDouble submBest,
                                                         List<SearchTreeNode> beginGenerators,
                                                         Set<SearchTreeNode> endGenerators )
    {
        System.err.printf( "ExactSkyline: got %d begin and %d end generators.\n",
                           beginGenerators.size(), endGenerators.size() );
        DoubleMatrix B = getPrefixBound( eval, k );

        // The priority queue maintains a set of search tree nodes that we have not
        // expanded yet.
        Border border = initBorder( eval, beginGenerators );

        int wereInQueue = 0;
        List<SkylinePoint> skyline = new LinkedList<SkylinePoint>();
        while ( border.size() > 0 ) {
            wereInQueue++;
            updateBorder( border, skyline, eval, B, submBest, endGenerators, k );
        }
        System.err.println( "wereInQueue = " + wereInQueue );
        return skyline;
    }

    private void updateBorder( Border              border,
                               List<SkylinePoint>  skyline,
                               Evaluator           eval,
                               DoubleMatrix        B,
                               MutableDouble       submBest,
                               Set<SearchTreeNode> endGenerators,
                               int                 k )
    {
        SearchTreeNode node = border.poll();
        double submValue    = eval.submodularFnc( node.pos );
        double sumValue     = eval.sumFnc( node.pos );
        if ( submValue > submBest.value( sumValue ) ) {
            skyline.add( new SkylinePoint( eval.actualItems(node.pos), sumValue, submValue ) );
            updatePeakMemory();
            submBest.setValue( sumValue, submValue );
            System.err.printf( "q = %.3f, d = %.3f, border_size = %d\n",
                               sumValue, submValue, border.size() );
        }
        // Points from the previous skyline are identified by j = -1,
        // these should not be expanded further in the search tree.
        if ( node.j != -1 ) {
            for ( SearchTreeNode next : node.children( eval ) ) {
                if ( !endGenerators.contains( next ) ) {
                    processChildNode( border, next, eval, B, submBest, k );
                }
            }
        }
    }

    private Border initBorder( Evaluator eval,
                               List<SearchTreeNode> beginGenerators )
    {
        Border border = new Border( 1<<16, getNodeComparator( eval ) );

        for ( SearchTreeNode s : beginGenerators ) {
            border.add( s );
        }
        return border;
    }

    private Comparator<SearchTreeNode> getNodeComparator( Evaluator eval )
    {
        if ( eval.maxSum() ) {
            return new Comparator<SearchTreeNode>() {
                public int compare( SearchTreeNode a, SearchTreeNode b ) {
                    return (int)Math.signum( eval.sumFnc(b.pos) - eval.sumFnc(a.pos) );
                }
            };
        }
        else {
            return new Comparator<SearchTreeNode>() {
                public int compare( SearchTreeNode a, SearchTreeNode b ) {
                    return (int)Math.signum( eval.sumFnc(a.pos) - eval.sumFnc(b.pos) );
                }
            };
        }
    }
    
    private void processChildNode( Border border,
                                   SearchTreeNode next,
                                   Evaluator eval,
                                   DoubleMatrix B,
                                   MutableDouble submBest,
                                   int k )
    {
        double prefixBound = 0.0;
        double suffixValue = 0.0;
        double totalBound  = Double.POSITIVE_INFINITY;
        int    z           = k - next.j;
        if ( z >= 2 ) {
            if ( z == 2 ) {
                suffixValue = eval.submodularFnc( next.pos[ next.j+1 ] );
            }
            else {
                suffixValue = eval.submodularFnc( Utils.arraySuffix( next.pos, next.j+1 ) );
            }
            prefixBound = B.get( next.pos[ next.j+1 ], next.j );
            totalBound  = prefixBound + suffixValue;
        }
        if ( totalBound >= submBest.value( eval.sumFnc( next.pos ) ) ) {
            border.add( next );
        }
    }

    private double[] getSingletonValues( Evaluator eval ) {
        double[] x = new double[ eval.numItems() ];
        for ( int i = 0; i < x.length; i++ ) {
            x[i] = eval.submodularFnc( i );
        }
        return x;
    }

    private DoubleMatrix getPrefixBound( Evaluator eval, int k )
    {
        double[]     tmp = getSingletonValues( eval );
        DoubleMatrix B   = new DoubleMatrix( tmp.length, k-1 );
        for ( int x = 1; x < tmp.length; x++ ) {
            int j = x;
            // This loop makes sure the (x+1)-length prefix of tmp
            // is always sorted in decreasing order.
            while ( j > 0 && tmp[j] > tmp[j-1] ) {
                double foo = tmp[j];
                tmp[j]     = tmp[j-1];
                tmp[j-1]   = foo;
                j--;
            }
            B.set( x, 0, tmp[0] );
            for ( int i = 1; i < Math.min(x, k-1); i++ ) {
                B.set( x, i, B.get( x, i-1 ) + tmp[i] );
            }
        }
        return B;
    }

    protected MutableDouble getSubmBestObject( boolean maxSum )
    {
        return new MutableDouble();
    }

    protected class MutableDouble
    {
        protected double value;

        public MutableDouble()
        {
            this.value = Double.NEGATIVE_INFINITY;
        }

        // ignore sumValue parameter
        public double value( double sumValue )
        {
            return this.value;
        }

        // ignore sumValue parameter
        public void setValue( double sumValue, double submValue )
        {
            this.value = submValue;
        }
    }

    private class Border extends PriorityQueue<SearchTreeNode>
    {
        public Border( int size, Comparator<SearchTreeNode> cmp )
        {
            super( size, cmp );
        }
    }  
}
