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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;

public class FastSkyline extends SkylineAlg
{
    private int skylineSize = Integer.MAX_VALUE;
    
    public FastSkyline() {}
    
    public List<SkylinePoint> computeSkyline( int k, Evaluator eval )
    {
        MarginalGainUB mgub = new MarginalGainUB( eval );
        List<SkylinePoint> skyline = levelOneSkyline( eval );
        for ( int level = 1; level < k; level++ ) {
            System.err.printf( "FastSkyline: at level %d, expanding %d points, ",
                               level, skyline.size() );
            skyline = expandSkyline( skyline, eval, mgub );
            System.err.printf( "got %d points.\n", skyline.size() );
            skyline = filterSkyline( skyline, eval.maxSum() );
        }
        // the items in p currently correspond to ones in sorted order (of sumFnc),
        // we must replace these with the actual item ids before returning
        Utils.toActualItems( skyline, eval );
        return skyline;
    }

    public void configure( Map<String,Object> config )
    {
        if ( config.containsKey( "f" ) ) {
            this.skylineSize = Integer.parseInt( (String)config.get( "f" ) );
        }
    }

    private List<SkylinePoint> expandSkyline( List<SkylinePoint> skyline,
                                              Evaluator eval,
                                              MarginalGainUB mgub )
    {
        List<SkylinePoint> nextSkyline = new LinkedList<>();
        SkylineExpander sle = new SkylineExpander( skyline, eval, mgub );
        double maxSubmValue = Double.NEGATIVE_INFINITY;
        CandidatePoint previous = new CandidatePoint( SkylinePoint.getEmptyPoint(), -1, -1 );
        while( sle.hasNext() ) {
            // SkylineExpander may generate equivalent points one after the other.
            // We only process a point if it is not equal to the previous one.
            CandidatePoint next = sle.next( maxSubmValue );
            if ( !next.equals( previous ) ) {
                maxSubmValue = processCandidate( next, nextSkyline, eval, mgub, maxSubmValue );
                previous = next;
            }
        }
        return nextSkyline;
    }

    private double processCandidate( CandidatePoint     cand,
                                     List<SkylinePoint> nextSkyline,
                                     Evaluator          eval,
                                     MarginalGainUB     mgub,
                                     double             maxSubmValue )
    {
        if ( mgub.boundExceedsMax( cand.basePoint, cand.v, maxSubmValue ) ) {
            // int[] newitems   = cand.basePoint.extend( cand.v );
            int[] newitems = cand.newitems;
            double submValue = eval.submodularFnc( newitems );
            mgub.addGain( cand.basePoint, cand.v, submValue - cand.basePoint.submodularValue() );
            if ( submValue > maxSubmValue ) {
                nextSkyline.add( new SkylinePoint( newitems, eval.sumFnc(newitems), submValue ) );
                updatePeakMemory();
                maxSubmValue = submValue;
            }
        }
        return maxSubmValue;
    }

    private List<SkylinePoint> filterSkyline( List<SkylinePoint> skyline, boolean maxSum )
    {
        if ( skyline.size() <= this.skylineSize ) {
            return skyline;
        }
        return new BasicHypervolumeFilter().filter( skyline, this.skylineSize, maxSum );
    }

    private List<SkylinePoint> levelOneSkyline( Evaluator eval ) {
        LevelwiseSkyline lvl = new LevelwiseSkyline();
        return lvl.computeSkyline( 1, eval, false );
    }

    //////////////////////////////////////////////////////////////////////
    private static class SkylineExpander
    {
        private PriorityQueue<CandidatePoint> queue;
        private ExpansionIterator[] pointIterator;
        
        public SkylineExpander( List<SkylinePoint> skyline,
                                Evaluator          eval,
                                MarginalGainUB     mgub )
        {
            this.queue = new PriorityQueue<>( skyline.size(),
                                              getCandidateComparator( eval ) );

            this.pointIterator = new ExpansionIterator[ skyline.size() ];
            for ( int i = 0; i < this.pointIterator.length; i++ ) {
                this.pointIterator[i] = new ExpansionIterator( skyline.get(i), i, mgub,
                                                               eval.numItems() );
                CandidatePoint c = this.pointIterator[i].next( Double.NEGATIVE_INFINITY );
                if ( c != null ) {
                    this.queue.add( c );
                }
            }
        }

        public boolean hasNext()
        {
            return this.queue.size() > 0;
        }

        public CandidatePoint next( double maxSubmValue )
        {
            CandidatePoint p = this.queue.poll();
            CandidatePoint c = this.pointIterator[ p.basePointPos ].next( maxSubmValue );
            if ( c != null ) {
                this.queue.add( c );
            }
            return p;
        }

        // Returns a comparator that orders candidate points in increasing
        // (maxSum = false) or decreasing (maxSum = true) order of the sum function.
        // Ties are broken by looking at the items of a candidate.
        private Comparator<CandidatePoint> getCandidateComparator( Evaluator eval )
        {
            if ( eval.maxSum() ) {
                return new Comparator<CandidatePoint>() {
                    public int compare( CandidatePoint a, CandidatePoint b ) {
                        int rv = (int)Math.signum( b.sumFnc(eval) - a.sumFnc(eval) );
                        if ( rv == 0 ) {
                            rv = Utils.lexicographicArrayComparison( a.newitems, b.newitems );
                        }
                        return rv;
                    }
                };
            }
            else {
                return new Comparator<CandidatePoint>() {
                    public int compare( CandidatePoint a, CandidatePoint b ) {
                        int rv = (int)Math.signum( a.sumFnc(eval) - b.sumFnc(eval) );
                        if ( rv == 0 ) {
                            rv = Utils.lexicographicArrayComparison( a.newitems, b.newitems );
                        }
                        return rv;
                    }
                };
            }
        }
    }

    //////////////////////////////////////////////////////////////////////
    private static class ExpansionIterator
    {
        private SkylinePoint   basePoint;
        private int            basePointPos;
        private MarginalGainUB mgub;
        private int            numItems;
        private int            currentItem;
        
        public ExpansionIterator( SkylinePoint p, int basePointPos,
                                  MarginalGainUB mgub, int numItems )
        {
            this.basePoint    = p;
            this.basePointPos = basePointPos;
            this.mgub         = mgub;
            this.numItems     = numItems;
            this.currentItem  = -1;
        }

        public CandidatePoint next( double maxSubmValue )
        {
            if ( this.currentItem == this.numItems ) {
                return null;
            }
            this.currentItem++;
            if ( this.exhausted( maxSubmValue ) ) {
                this.currentItem = this.numItems;
                return null;
            }
            while( this.basePoint.contains( this.currentItem ) ||
                   !this.mgub.boundExceedsMax( basePoint,
                                               this.currentItem,
                                               maxSubmValue ) ) {
                this.currentItem++;
                if ( this.exhausted( maxSubmValue ) ) {
                    this.currentItem = this.numItems;
                    return null;
                }
            }
            // when we get here, basePoint should not contain currentItem
            return new CandidatePoint( this.basePoint,
                                       this.basePointPos,
                                       this.currentItem );
        }

        // The iterator becomes exhausted when pos reaches the end, or when
        // we know that further items down the array cannot lead to an increase
        // in submodular value.
        private boolean exhausted( double maxSubmValue )
        {
            return ( this.currentItem >= this.numItems ||
                     (this.mgub.unitBound(this.currentItem) +
                      this.basePoint.submodularValue()) < maxSubmValue );
        }
    }

    //////////////////////////////////////////////////////////////////////
    private static class CandidatePoint
    {
        private SkylinePoint basePoint;
        private int          basePointPos;
        private int          v;
        private int[]        newitems;
        
        public CandidatePoint( SkylinePoint basePoint, int basePointPos, int v )
        {
            this.basePoint    = basePoint;
            this.basePointPos = basePointPos;
            this.v            = v;
            this.newitems     = this.basePoint.extend( this.v );
        }

        public double sumFnc( Evaluator eval )
        {
            return this.basePoint.sumValue() + eval.sumFnc( this.v );
        }

        // Two candidate points are equal whenever they result in the same point,
        // i.e. basePoint + v is the same set.
        public boolean equals( Object o )
        {
            CandidatePoint other = (CandidatePoint)o;
            return Arrays.equals( this.newitems, other.newitems );
        }
    }

    //////////////////////////////////////////////////////////////////////
    private static class SetGainPair
    {
        private SkylinePoint point;
        private double       gain;

        public SetGainPair( SkylinePoint point, double gain )
        {
            this.point = point;
            this.gain  = gain;
        }
    }

    //////////////////////////////////////////////////////////////////////
    private static class MarginalGainUB
    {
        private double[] unitBound;
        private double[] singleItemSubmValue;
        private ArrayList<LinkedList<SetGainPair>> subsetGain;
        
        public MarginalGainUB( Evaluator eval )
        {
            this.unitBound   = initUnitBound( eval );

            this.subsetGain  = new ArrayList<LinkedList<SetGainPair>>(eval.numItems());
            for ( int i = 0; i < eval.numItems(); i++ ) {
                this.subsetGain.add(i, new LinkedList<SetGainPair>());
            }

            this.singleItemSubmValue = new double[ eval.numItems() ];
            for ( int i = 0; i < this.singleItemSubmValue.length; i++ ) {
                this.singleItemSubmValue[ i ] = eval.submodularFnc( i );
            }
        }

        public double unitBound( int pos )
        {
            return this.unitBound[ pos ];
        }

        public void addGain( SkylinePoint point, int item, double gain )
        {
            this.subsetGain.get(item).add( new SetGainPair( point, gain ) );
        }

        public boolean boundExceedsMax( SkylinePoint basePoint,
                                        int item,
                                        double maxSubmValue )
        {
            double mingain = this.singleItemSubmValue[ item ];
            if ( basePoint.submodularValue() + mingain < maxSubmValue ) {
                return false;
            }
            for( SetGainPair sgp : this.subsetGain.get( item ) ) {
                if ( sgp.gain < mingain &&
                     sgp.point.size() < basePoint.size() &&
                     basePoint.hasSubset( sgp.point ) )
                    {
                        mingain = sgp.gain;
                        if ( basePoint.submodularValue() + mingain < maxSubmValue ) {
                            return false;
                        }
                    }
            }
            return ( basePoint.submodularValue() + mingain >= maxSubmValue );
        }

        private double[] initUnitBound( Evaluator eval  )
        {
            double[] unitbound = new double[ eval.numItems() ];
            unitbound[unitbound.length-1] = eval.submodularFnc( unitbound.length-1 );
            for ( int i = unitbound.length-2; i >= 0; i-- ) {
                unitbound[i] = Math.max( eval.submodularFnc( i ), unitbound[i+1] );
            }
            return unitbound;
        }
    }
}
