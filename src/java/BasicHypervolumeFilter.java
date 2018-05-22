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

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.PriorityQueue;

public class BasicHypervolumeFilter
{
    public static int SKYLINE_MAX_SIZE = 10;
    
    public BasicHypervolumeFilter() { }

    // skyline must be sorted in increasing order of the sum function!
    public List<SkylinePoint> filter( List<SkylinePoint> skyline,
                                      int k,
                                      boolean maxSum )
    {
        // initialize the filtered skyline to contain the 1st and last points
        // in the skyline. this is required by some subsequent functions!
        // it makes certain things easier / more elegant to implement.
        // these points also define the boundaries of the hypervolume,
        // and do thus not contribute to the objective function.
        List<SkylinePoint> filteredSkyline = new LinkedList<>();
        filteredSkyline.add( skyline.get(0) );
        filteredSkyline.add( skyline.get(skyline.size()-1) );

        // the priority queue used by the CELF++ optimisations to the
        // greedy method for constrained maximisation of submodular functions.
        PriorityQueue<MarginalGain> mgQueue = new PriorityQueue<>();

        Comparator<SkylinePoint> cmp = Utils.getSkylinePointComparator( maxSum );
        
        Collections.sort( filteredSkyline, cmp );
        for ( SkylinePoint p : skyline ) {
            mgQueue.add( new MarginalGain( p, gainFnc( p, filteredSkyline ) ) );
        }

        MarginalGain currentBest = new MarginalGain( null, 0.0 );

        while ( filteredSkyline.size() < k ) {
            MarginalGain topgain = mgQueue.poll();
            if ( topgain.mg < currentBest.mg ) {
                // we know currentBest must enter the solution
                // topgain can thus go back into the queue
                mgQueue.add( topgain );
                filteredSkyline.add( currentBest.p );
                // filteredSkyline must be kept sorted
                Collections.sort( filteredSkyline, cmp );
                currentBest = new MarginalGain( null, 0.0 );
            }
            else {
                // topgain might enter the solution, if its true marginal gain is high enough
                double gain = gainFnc( topgain.p, filteredSkyline );
                if ( gain > currentBest.mg ) {
                    // yes! gain improved.
                    // previous best guy goes back into the queue
                    mgQueue.add( currentBest );
                    // and we have a new currentBest
                    currentBest = new MarginalGain( topgain.p, gain );
                }
                else {
                    // the guy didn't make it this time, we insert it back into mg
                    // with its new marginal gain
                    mgQueue.add( new MarginalGain( topgain.p, gain ) );
                    if ( gain == topgain.mg ) {
                        // We get here if the gain did not change from its previous value.
                        // This means we must update the solution, because
                        // all following guys in the heap can have at most this gain anyway.
                        filteredSkyline.add( currentBest.p );
                        Collections.sort( filteredSkyline, cmp );
                        currentBest = new MarginalGain( null, 0.0 );
                    }
                }
            }
        }
        
        return filteredSkyline;
    }

    // Gain of a skyline point when added to an existing skyline.
    // Note: skyline must be sorted in increasing order of sumFnc!!
    // We also know that skyline always contains the 1st and last points
    // of the candidate skyline.
    private double gainFnc( SkylinePoint point, List<SkylinePoint> skyline )
    {
        Iterator<SkylinePoint> it = skyline.iterator();
        SkylinePoint prev = it.next();
        while ( it.hasNext() ) {
            SkylinePoint next = it.next();
            
            if ( prev.sumValue() <= point.sumValue() && point.sumValue() <= next.sumValue() ) {
                return Math.abs(next.sumValue() - point.sumValue()) *
                    (point.submodularValue() - prev.submodularValue());
            }
            
            prev = next;
        }

        System.err.printf( "BasicHypervolumeFilter: We shouldn't get here, ever!\n" );
        System.err.printf( "input point: %s\ninput skyline: %s",
                           point.toString(), skyline.toString() );
        return -1.0;
    }

    private static class MarginalGain implements Comparable
    {
        private SkylinePoint p;
        private double       mg;
        
        public MarginalGain( SkylinePoint p, double mg )
        {
            this.p  = p;
            this.mg = mg;
        }

        public int compareTo( Object other )
        {
            return (int)Math.signum( ((MarginalGain)other).mg - this.mg );
        }
    }
}
