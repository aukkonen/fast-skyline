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

import java.util.Map;
import java.util.BitSet;

public class SetCoverEvaluator extends Evaluator
{
    public double[]  values;
    public IntMatrix sets;
    
    public void init( Map<String,Object> options ) throws Exception
    {
        super.init( options ); // this gets maxSum
        double[] values = (double[])options.get( "values" );

        this.origItem = Utils.sortedIdx( values, this.maxSum );

        this.values = new double[ values.length ];
        for ( int i = 0; i < values.length; i++ ) {
            this.values[ i ] = values[ this.origItem[i] ];
        }

	// In our set-cover instance sets are represented by columns.
        this.sets = new IntMatrix( (int[])options.get( "scdata" ),
                                   Integer.parseInt((String)options.get( "univSize" )),
                                   Integer.parseInt((String)options.get( "numSets" )) );
        this.sets = Utils.rearrangeColumns( this.sets, this.origItem );
    }

    public void init( double[] values, int[] scdata, int univSize, int numSets )
    {
	this.origItem = Utils.sortedIdx( values, this.maxSum );
	this.values = new double[ values.length ];
	for ( int i = 0; i < values.length; i++ ) {
	    this.values[ i ] = values[ this.origItem[i] ];
	}
	this.sets = new IntMatrix( scdata, univSize, numSets );
	this.sets = Utils.rearrangeColumns( this.sets, this.origItem );
    }

    public int numItems()
    {
        return this.values.length;
    }

    public double sumFnc( int item )
    {
        this.sumCalls++;
        return this.values[ item ];
    }

    public double sumFnc( int[] items ) {
        this.sumCalls++;
        double s = 0.0;
        for ( int i = 0; i < items.length; i++ ) {
            s += this.values[ items[i] ];
        }
        return s;
    }

    public double submodularFnc( int item )
    {
        this.submCalls++;
        double s = 0.0;
        for ( int i = 0; i < this.sets.nrow(); i++ ) {
            s += this.sets.get( i, item );
        }
        return s;
    }

    public double submodularFnc( int[] items )
    {
        this.submCalls++;
        BitSet bs = new BitSet();
        for ( int item : items ) {
            for ( int row = 0; row < this.sets.nrow(); row++ ) {
                if ( this.sets.get( row, item )==1 ) {
                    bs.set( row );
                }
            }
        }
        return bs.cardinality();
    }
}
