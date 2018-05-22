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
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Map;

public class SubgroupSetEvaluatorIntCover extends SubgroupSetEvaluator
{
    private IntMatrix covers;
    
    public SubgroupSetEvaluatorIntCover() {}

    public void init( Map<String,Object> options )
        throws Exception
    {
        super.init( options );
        IntMatrix c = loadCovers( (String)options.get( "c" ) );
        this.covers = Utils.rearrangeColumns( c, this.origItem );

        // Don't forget to set this.nrow!
        this.nrow = this.covers.nrow();
    }

    protected int countOnes( int item )
    {
        int ones = 0;
        for ( int i = 0; i < this.covers.nrow(); i++ ) {
            ones += this.covers.get( i, item );
        }
        return ones;
    }

    protected int[] fillCounters( int[] itemset )
    {
        int[] counter = new int[ 1 << itemset.length ];
        for ( int row = 0; row < this.covers.nrow(); row++ ) {
            int bucket = 0;
            for ( int j = 0; j < itemset.length; j++ ) {
                bucket |= this.covers.get( row, itemset[j] ) << j;
            }
            counter[ bucket ]++;
        }
        return counter;
    }

    private IntMatrix loadCovers( String subsetsFile )
        throws Exception
    {
        System.err.print( "Loading subgroup covers..." );
        List<String> covers = new LinkedList<String>();
        for ( String cover : Utils.fileLineIterator( subsetsFile, false ) ) {
            covers.add( cover.trim() );
        }
        IntMatrix C = new IntMatrix( covers.get( 0 ).length(), covers.size() );
        for ( int j = 0; j < C.ncol(); j++ ) {
            String s = covers.get( j );
            for ( int i = 0; i < C.nrow(); i++ ) {
                C.set( i, j, Integer.parseInt( s.substring( i, i+1 ) ) );
            }
        }
        System.err.println( "done!!" );
        return C;
    }

    public void printCovers()
    {
        for ( int j = 0; j < this.covers.ncol(); j++ ) {
            for ( int i = 0; i < this.covers.nrow(); i++ ) {
                if ( this.covers.get( i, j ) == 1 ) {
                    System.err.printf( "%d ", i );
                }
            }
            System.err.println();
        }
    }
}
