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

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentSkipListMap;

public class ExactSkylineParallel extends ExactSkyline
{
    // These are default values.
    private int _INTERVAL_COUNT     = 10;
    private int _NUM_THREADS        = 4;
    private boolean _INIT_WITH_FAST = false;
    
    private class Spine extends LinkedList<SearchTreeNode> { }

    //////////////////////////////////////////////////////////////////////
    private class NodeRankPair
    {
        private SearchTreeNode node;
        private BigInteger     rank;

        public NodeRankPair( SearchTreeNode node, BigInteger rank )
        {
            this.node = node;
            this.rank = rank;
        }
    }

    //////////////////////////////////////////////////////////////////////
    private class IntervalRunnable implements Runnable
    {
        private int k;
        private Evaluator eval;
        private MutableDouble submBest;
        private int numSegments;
        private int interval;
        private List<SkylinePoint> skyline;
        private double duration;
        
        public IntervalRunnable( int k, Evaluator eval, MutableDouble submBest,
                                 int numSegments, int interval )
        {
            this.k           = k;
            this.eval        = eval;
            this.submBest    = submBest;
            this.numSegments = numSegments;
            this.interval    = interval;
            this.skyline     = null;
        }

        public void run()
        {
            long begTime = System.nanoTime();
            this.skyline = computeInterval( this.k, this.eval, this.submBest,
                                            this.numSegments, this.interval );
            this.duration = (System.nanoTime() - begTime)/1000000000.0;
        }
    }

    //////////////////////////////////////////////////////////////////////
    protected class SynchronizedMutableDouble extends MutableDouble
    {
        private ConcurrentSkipListMap<Double,Double> skyline;

        private long time;

        public SynchronizedMutableDouble( boolean maxSum )
        {
            Comparator<Double> cmp = null;
            if ( maxSum ) {
                cmp = new Comparator<Double>() {
                        public int compare( Double a, Double b ) {
                            return (int)Math.signum( b - a );
                        }
                    };
            }
            else {
                cmp = new Comparator<Double>() {
                        public int compare( Double a, Double b ) {
                            return (int)Math.signum( a - b );
                        }
                    };
            }
            this.skyline = new ConcurrentSkipListMap<>( cmp );
            // make sure there is always something in the list.
            // this should guarantee the floorEntry method always returns something
            if ( maxSum ) {
                this.skyline.put( Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY );
            } else {
                this.skyline.put( Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY );
            }
        }
        
        public double value( double sumvalue )
        {
            return this.skyline.floorEntry( sumvalue ).getValue();
        }

        public synchronized void setValue( double sumValue, double submValue )
        {
            long btime = System.nanoTime();
            if ( value( sumValue ) > submValue  ) {
                return;
            }

            Iterator<Map.Entry<Double,Double>> it =
                this.skyline.tailMap(sumValue, true).entrySet().iterator();
            
            while( it.hasNext() ) {
                Map.Entry<Double,Double> kvpair = it.next();
                if ( kvpair.getValue() <= submValue ) {
                    it.remove();
                }
                else {
                    break;
                }
            }
            this.skyline.put( sumValue, submValue );
            this.time += (System.nanoTime() - btime);
        }
    }

    //////////////////////////////////////////////////////////////////////
    public ExactSkylineParallel() { }

    // Make sure to set _INTERVAL_COUNT to some value before calling this method...
    protected MutableDouble getSubmBestObject( boolean maxSum )
    {
        return new SynchronizedMutableDouble( maxSum );
    }

    public void configure( Map<String,Object> config )
    {
        this._INTERVAL_COUNT = Integer.parseInt( (String)config.get( "i" ) );
        this._NUM_THREADS    = Integer.parseInt( (String)config.get( "t" ) );
        this._INIT_WITH_FAST = config.containsKey( "fastInit" );
    }

    public List<SkylinePoint> computeSkyline( int k, Evaluator eval )
    {
        ThreadPoolExecutor exec = new ThreadPoolExecutor( this._NUM_THREADS,
                                                          this._NUM_THREADS,
                                                          1L, TimeUnit.DAYS,
                                                          new LinkedBlockingQueue<Runnable>() );

        MutableDouble submBest = getSubmBestObject( eval.maxSum() );
        if ( this._INIT_WITH_FAST ) {
            // we first run FastSkyline to get an initial set of upper bounds
            // to be used in submBest.
            List<SkylinePoint> initialSkyline = new FastSkyline().computeSkyline( k, eval );
            for ( SkylinePoint p : initialSkyline ) {
                submBest.setValue( p.sumValue(), p.submodularValue() );
            }
        }
        
        // Set up runnables for parallel computation.
        List<IntervalRunnable> runnables = new LinkedList<>();
        for ( int interval = 1; interval <= this._INTERVAL_COUNT; interval++ ) {
            System.err.printf( "Launching interval %d.\n", interval );
            IntervalRunnable ir = new IntervalRunnable( k, eval, submBest,
                                                        this._INTERVAL_COUNT, interval );
            runnables.add( ir );
            exec.execute( ir );
        }

        System.err.println( "Waiting for parallel execution to finish..." );
        try {
            exec.shutdown(); // runs executor to completion
            exec.awaitTermination( 1, TimeUnit.DAYS );
        } catch ( InterruptedException ioe ) {
            ioe.printStackTrace();
        }
        System.err.println( "All intervals are processed!" );
        System.err.printf( "Synchronisation time: %.2f msec\n",
                           ((SynchronizedMutableDouble)submBest).time/1000000.0 );
        
        // here we should still merge the individual skylines from different parts...
        return joinSkylines( runnables, eval.maxSum() );
    }

    private LinkedList<SkylinePoint> joinSkylines( List<IntervalRunnable> runnables,
                                                   boolean maxSum )
    {
        LinkedList<SkylinePoint> combined = new LinkedList<>();
        for ( IntervalRunnable ir : runnables ) {
            System.err.printf( "Interval %d ran for %.2f sec and got %d points.\n",
                               ir.interval, ir.duration, ir.skyline.size() );
            combined.addAll( ir.skyline );
        }

        Collections.sort( combined, Utils.getSkylinePointComparator( maxSum ) );

        LinkedList<SkylinePoint> finalSkyline = new LinkedList<>();
        SkylinePoint first = combined.poll();
        finalSkyline.add( first );
        double submMax = first.submodularValue();
        for ( SkylinePoint p: combined ) {
            if ( p.submodularValue() >= submMax ) {
                submMax = p.submodularValue();
                finalSkyline.add( p );
            }
        }
        return finalSkyline;
    }

    // It should be possible to run this in separate threads...
    // numSegments is the number of equal sized intervals (or blocks) in which the
    // search tree will be partitioned.
    // interval selects one of these for processing. The algorithm starts
    // at the subset at the beginning of the corresponding interval, and
    // proceeds until it reaches the end.
    private List<SkylinePoint> computeInterval( int           k,
                                                Evaluator     eval,
                                                MutableDouble submBest,
                                                int           numSegments,
                                                int           interval )
    {
        int        numItems     = eval.numItems();
        BigInteger totalSubsets = Utils.nchoosek( numItems, k );
        BigInteger delta        = totalSubsets.divide( BigInteger.valueOf( (long)numSegments ) );
        // if ( totalSubsets % numSegments != 0 )
        if ( totalSubsets.remainder( BigInteger.valueOf( (long)numSegments ) ).
             compareTo( BigInteger.ZERO ) != 0 ) {
            delta.add( BigInteger.ONE );
        }
        List<SearchTreeNode> beginGenerators =
            findInterval(delta.multiply(BigInteger.valueOf(interval-1)).add(BigInteger.ONE),
                         delta, numItems, k);
        System.err.printf( "computeInterval: got %d begin generators:\n",
                           beginGenerators.size() );
        for ( SearchTreeNode stn : beginGenerators ) {
            System.err.printf( "%s, rank = %s\n",
                               stn.toString(), rank(stn, numItems).toString() );
        }
        Set<SearchTreeNode> endGenerators = new HashSet<>();
        if ( interval < numSegments ) {
            List<SearchTreeNode> tmp =
                findInterval( delta.multiply(BigInteger.valueOf(interval)).
                              add(BigInteger.ONE), delta, numItems, k );
            for ( SearchTreeNode node : tmp ) {
                endGenerators.add( node );
            }
        }
        System.err.printf( "computeInterval: got %d end generators.\n",
                           endGenerators.size() );
        return computeSkylineInterval( k, eval,
                                       submBest, beginGenerators, endGenerators );
    }

    // index: Starting index of the interval.
    // delta: Length of the interval.
    public List<SearchTreeNode> findInterval( BigInteger index,
                                              BigInteger delta,
                                              int        numItems,
                                              int        subsetSize )
    {
        List<SearchTreeNode> generators = new LinkedList<SearchTreeNode>();
        SearchTreeNode node = unrank(index,
                                     new SearchTreeNode(Utils.range(1, subsetSize+1), subsetSize-1),
                                     numItems);
        generators.add( node.shiftLeft() );
        int[] realEnd = Utils.range( numItems - subsetSize + 1, numItems + 1);
        BigInteger ss = subtreeSize( node, findEnd( node, numItems ) );
        while ( ss.compareTo( delta ) < 0 ) {
            node = unrank( index.add( ss ),
                           new SearchTreeNode( Utils.range( 1, subsetSize+1 ), subsetSize-1 ),
                           numItems );
            generators.add( node.shiftLeft() );
            int[] end = findEnd( node, numItems );
            if ( Arrays.equals( end, realEnd ) ) {
                break;
            }
            ss = ss.add( subtreeSize( node, end ) );
        }
        return generators;
    }

    // Computes the rank of a search tree node.
    private BigInteger rank( SearchTreeNode root, int numItems )
    {
        int[] end = Utils.range( numItems - root.size() + 1, numItems + 1);
        BigInteger treesize = Utils.nchoosek( numItems, root.size() );
        return rank( root, end, treesize );
    }

    private BigInteger rank( SearchTreeNode root, int[] end, BigInteger treesize )
    {
        return treesize.subtract( subtreeSize( root, end ) ).add( BigInteger.ONE );
    }

    private SearchTreeNode unrank( BigInteger index, SearchTreeNode root, int numItems )
    {
        NodeRankPair nrp = findNprime( index, root, numItems );
        // test for equality ( nrp == index )
        if ( nrp.rank.compareTo( index ) == 0 ) {
            return nrp.node;
        }
        else {
            SearchTreeNode l = nrp.node.left();
            if ( l == null ) {
                System.err.printf( "nrp.rank = %s, index = %s\n",
                                   nrp.rank.toString(), index.toString() );
            }
            return unrank( index.subtract( nrp.rank ), l, numItems );
        }
    }

    private int[] findEnd( SearchTreeNode root, int n )
    {
        int[] end = root.copyOfItems();
        int   j   = root.j;
        if ( j + 1 < end.length ) {
            end[j] = end[ j+1 ] - 1;
        }
        else {
            end[j] = n;
        }
        for ( int i = j-1; i >= 0; i-- ) {
            end[i] = end[i+1] - 1;
        }
        return end;
    }

    private NodeRankPair findNprime( BigInteger index, SearchTreeNode root, int numItems )
    {
        int[] end = findEnd( root, numItems );
        BigInteger treesize = subtreeSize( root, end );
        // tests for equality ( treesize == 1 )
        if ( treesize.compareTo( BigInteger.ONE ) == 0 ) {
            return new NodeRankPair( root, BigInteger.ONE );
        }
        Spine spine = materializeSpine( root, end );
        SearchTreeNode prevnode = spine.poll();
        BigInteger prevrank = rank( prevnode, end, treesize );
        for ( SearchTreeNode node : spine ) {
            BigInteger thisrank = rank( node, end, treesize );
            // check that ( prevrank <= index < thisrank )
            if ( prevrank.compareTo( index ) <= 0 && index.compareTo( thisrank ) < 0 ) {
                break;
            }
            prevnode = node;
            prevrank = thisrank;
        }
        return new NodeRankPair( prevnode, prevrank );
    }

    private BigInteger subtreeSize( SearchTreeNode root, int[] end )
    {
        BigInteger size = BigInteger.ZERO;
        for ( int i = root.pos[ root.j ]; i < end[root.j]+1; i++ ) {
            size = size.add( Utils.nchoosek( i-1, root.j ) );
        }
        return size;
    }

    private Spine materializeSpine( SearchTreeNode root, int[] end )
    {
        Spine spine = new Spine();
        spine.add( root );
        int j = root.j;
        for ( int q = j; q >= 0; q-- ) {
            if ( root.pos[q] == end[q] ) {
                continue;
            }
            SearchTreeNode newnode = null;
            for ( int v = root.pos[q]+1; v < end[q]+1; v++ ) {
                int[] newitems = root.copyOfItems();
                newitems[q] = v;
                newnode = new SearchTreeNode( newitems, q );
                spine.add( newnode );
            }
            root = newnode;
        }
        return spine;
    }
}
