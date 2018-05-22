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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class LevelwiseSkyline extends SkylineAlg
{
    // max size of the intermediary skyline
    private int skylineSize = Integer.MAX_VALUE;
    
    public LevelwiseSkyline() {}

    public List<SkylinePoint> computeSkyline( int k, Evaluator eval )
    {
        return computeSkyline( k, eval, true );
    }

    public List<SkylinePoint> computeSkyline( int k, Evaluator eval, boolean returnActual )
    {
        // First generate skyline of points of size 1.
        Set<Candidate> candidates = new HashSet<>();
        for ( int i = 0; i < eval.numItems(); i++ ) {
            candidates.add( new Candidate(new int[] { i }) );
        }
        List<SkylinePoint> skyline = updateSkyline( candidates, eval );
        System.err.printf( "LevelwiseSkyline: level 1 skyline_size: %d\n",
                           skyline.size() );
        for ( int level = 1; level < k; level++ ) {
            candidates = updateCandidates( skyline, eval );
            System.err.printf( "LevelwiseSkyline: on level %d, got %d candidates",
                               level+1, candidates.size() );
            skyline    = updateSkyline( candidates, eval );
            System.err.printf( ", kept %d", skyline.size() );
            skyline    = filterSkyline( skyline, eval.maxSum() );
            System.err.printf( ", %d post filter.\n", skyline.size() );
        }
        if ( returnActual ) {
            Utils.toActualItems( skyline, eval );
        }
        return skyline;
    }

    public void configure( Map<String,Object> config )
    {
        if ( config.containsKey( "f" ) ) {
            this.skylineSize = Integer.parseInt( (String)config.get( "f" ) );
        }
    }

    private List<SkylinePoint> filterSkyline( List<SkylinePoint> skyline, boolean maxSum )
    {
        if ( skyline.size() <= this.skylineSize ) {
            return skyline;
        }
        return new BasicHypervolumeFilter().filter( skyline, this.skylineSize, maxSum );
    }

    private Set<Candidate> updateCandidates( List<SkylinePoint> skyline,
                                             Evaluator eval )
    {
        Set<Candidate> candidates = new HashSet<>();
        for ( SkylinePoint p : skyline ) {
            for ( int[] e : p.extensionSets( eval.numItems() ) ) {
                candidates.add( new Candidate(e) );
            }
        }
        return candidates;
    }

    private List<SkylinePoint> updateSkyline( Set<Candidate> candidates,
                                              Evaluator eval )
    {
        Candidate[] candArray = candidates.toArray( new Candidate[0] );
        Arrays.sort( candArray, getSetComparator( eval ) );
        List<SkylinePoint> skyline = new LinkedList<SkylinePoint>();
        double submBest = Double.NEGATIVE_INFINITY;
        for( Candidate c : candArray ) {
            double psubv = eval.submodularFnc(c.set);
            if ( psubv > submBest ) {
                skyline.add( new SkylinePoint( c.set, eval.sumFnc(c.set), psubv) );
                updatePeakMemory();
                submBest = psubv;
            }
        }
        return skyline;
    }

    private Comparator<Candidate> getSetComparator( Evaluator eval )
    {
        if ( eval.maxSum() ) {
            return new Comparator<Candidate>() {
                public int compare( Candidate a, Candidate b ) {
                    return (int)Math.signum( eval.sumFnc(b.set) - eval.sumFnc(a.set) );
                }
            };
        }
        else {
            return new Comparator<Candidate>() {
                public int compare( Candidate a, Candidate b ) {
                    return (int)Math.signum( eval.sumFnc(a.set) - eval.sumFnc(b.set) );
                }
            };
        }
    }

    private class Candidate
    {
        private int[] set;

        public Candidate( int[] set )
        {
            this.set = set;
        }

        public int hashCode()
        {
            return Utils.arrayHashCode( this.set );
        }

        public boolean equals( Object o )
        {
            Candidate c = (Candidate)o;
            return Arrays.equals( this.set, c.set );
        }
    }
}
