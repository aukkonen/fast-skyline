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

import java.util.BitSet;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.IntConsumer;

public class SubgroupSetEvaluatorBitCover extends SubgroupSetEvaluator
{
    private BitSet[] bitcovers;

    public SubgroupSetEvaluatorBitCover() {}

    // Constructor for the R interface.
    // quality[] is an array of quality values
    // coverMatrix[] is the data from the cover matrix in column major format
    // public SubgroupSetEvaluatorBitCover( double[] quality,
    //                                      int[]    coverMatrix,
    //                                      int      numSets,
    //                                      int      dataSize )
    // {
    //     super();
    //     this.quality = quality;
    //     this.bitcovers = new BitSet[numSets];
    //     for ( int i = 0; i < numSets; i++ ) {
    //         BitSet bs = new BitSet( dataSize );
    //         for ( int j = 0; j < dataSize; j++ ) {
    //             if ( coverMatrix[i*dataSize + j ] == 1 ) {
    //                 bs.set( j );
    //             }
    //         }
    //         this.bitcovers[i] = bs;
    //     }
    //     System.err.printf( "SubgroupSetEvaluatorBitCover: Got %d subgroups over %d data rows.",
    //                        this.bitcovers.length, dataSize );
    //     this.nrow = dataSize;
    // }

    public void init( Map<String,Object> options )
        throws Exception
    {
        super.init( options );
        // Load bit covers and also set this.nrow:
        BitSet[] bc = loadBitCovers( (String)options.get( "c" ) );
        this.bitcovers = new BitSet[ bc.length ];
        for ( int i = 0; i < bc.length; i++ ) {
            // origItem has been initialised in super.init
            this.bitcovers[ i ] = bc[ this.origItem[i] ];
        }
    }

    protected int countOnes( int item )
    {
        return this.bitcovers[item].cardinality();
    }
    
    protected int[] fillCounters( int[] itemset )
    {
        int[] bucket  = new int[ this.nrow ];
        int[] shift   = new int[1];
        for( int i = 0; i < itemset.length; i++ ) {
            shift[0] = i;
            this.bitcovers[ itemset[i] ].stream().forEach( new IntConsumer() {
                    public void accept( int row ) {
                        bucket[row] |= (1 << shift[0]);
                    }
                } );
        }
        
        int[] counter = new int[ 1 << itemset.length ];
        for ( int i = 0; i < bucket.length; i++ ) {
            counter[ bucket[i] ]++;
        }
        return counter;
    }

    private BitSet[] loadBitCovers( String subsetsFile )
        throws Exception
    {
        List<BitSet> tmp = new LinkedList<BitSet>();
        for ( String cover : Utils.fileLineIterator( subsetsFile, false ) ) {
            cover = cover.trim();
            this.nrow = cover.length();
            BitSet bs = new BitSet( cover.length() );
            for ( int i = 0; i < cover.length(); i++ ) {
                if ( cover.substring( i, i+1 ).equals("1") ) {
                    bs.set( i );
                }
            }
            tmp.add( bs );
        }
        BitSet[] covers = new BitSet[ tmp.size() ];
        for( int i = 0; i < covers.length; i++ ) {
            covers[i] = tmp.get( i );
        }
        return covers;
    }

    public void printCovers()
    {
        for ( BitSet bs : this.bitcovers ) {
            System.err.printf( "%s\n", bs.toString() );
        }
    }
}
