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

/**
 * Implements an Evaluator for subgroup set skyline computation.
 *
 * SumFnc is the sum of subgroup qualities.
 * SubmodularFnc is subgroup diversity measured as the entropy of the cover.
 */
import java.util.Arrays;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Map;

public abstract class SubgroupSetEvaluator extends Evaluator
{
    protected double[]     quality;
    protected int          nrow;
    
    public SubgroupSetEvaluator() { };

    public void init( Map<String,Object> options )
        throws Exception
    {
        super.init( options );
        String stats2File = (String)options.get( "q" );
        init( stats2File );
    }
    
    protected void init( String stats2File )
        throws Exception
    {
        double[] q = loadQualities( stats2File );
        // rearrange q so that it is in increasing or decreasing order
        this.origItem = Utils.sortedIdx( q, this.maxSum );
        this.quality  = new double[ q.length ];
        for ( int i = 0; i < q.length; i++  ) {
            this.quality[ i ] = q[ this.origItem[i] ];
        }
    }

    public double sumFnc( int item )
    {
        this.sumCalls++;
        return this.quality[item];
    }
    
    public double sumFnc( int[] itemset )
    {
        this.sumCalls++;
        double s = 0.0;
        for ( int item : itemset ) {
            s += this.quality[ item ];
        }
        return s;
    }

    public double submodularFnc( int item )
    {
        this.submCalls++;
        int ones      = countOnes( item );
        int zeros     = this.nrow - ones;
        double N      = this.nrow * 1.000001;
        double pOnes  = ((double)ones)*1.000001/N;
        double pZeros = ((double)zeros)*1.000001/N;
        return -1.0 * ( pOnes * Math.log10(pOnes) + pZeros * Math.log10(pZeros) ) / Math.log10(2);
    }

    public double submodularFnc( int[] itemset )
    {
        this.submCalls++;
        return calculateEntropy( fillCounters( itemset ) );
    }

    public int numItems()
    {
        return this.quality.length;
    }

    protected abstract int countOnes( int item );
    
    protected abstract int[] fillCounters( int[] itemset );

    private double calculateEntropy( int[] counter )
    {
        double entropy = 0.0;
        double N       = this.nrow*1.000001;
        for ( int i = 0; i < counter.length; i++ ) {
            double p = ((double)counter[ i ] + 0.000001)/N;
            entropy += p * Math.log10(p);
        }
        // convert to base-2 log and positive value
        return -1.0*(entropy/Math.log10( 2.0 ));
    }

    // private List<String> loadDescriptors( String stats2File )
    //     throws Exception
    // {
    //     List<String> tmp = new LinkedList<>();
    //     for( String line : Utils.fileLineIterator( stats2File, true ) ) {
    //         String[] tokens = line.split( ";" );
    //         tmp.add( tokens[5] );
    //     }
    //     return tmp;
    // }

    private double[] loadQualities( String stats2File )
        throws Exception
    {
        List<Double> qualities = new ArrayList<Double>();
        for ( String line : Utils.fileLineIterator( stats2File, true ) ) {
            String[] tokens = line.split( ";" );
            double q = Double.parseDouble( tokens[2] );
            qualities.add( q );
        }
        double[] q = new double[ qualities.size() ];
        for ( int i = 0; i < q.length; i++ ) {
            q[i] = qualities.get( i );
        }
        return q;
    }
}
