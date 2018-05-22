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

public class SkylinePoint
{
    private int[] items;
    private double sumValue;
    private double submValue;

    public static SkylinePoint getEmptyPoint()
    {
        return new SkylinePoint( new int[] {}, 0.0, 0.0 );
    }
    
    public SkylinePoint( int[] items, double sumValue, double submValue )
    {
        this.items     = items;
        this.sumValue  = sumValue;
        this.submValue = submValue;
    }

    public int size()
    {
        return this.items.length;
    }

    public int[] suffixItems( int beginPos )
    {
        return Utils.arraySuffix( this.items, beginPos );
    }

    public void toActualItems( Evaluator eval )
    {
        this.items = eval.actualItems( this.items );
    }

    public int itemAt( int pos ) {
        return this.items[ pos ];
    }

    public boolean contains( int item )
    {
        return (Utils.arrayIndexOf( item, this.items ) >= 0);
    }

    public boolean hasSubset( int[] set )
    {
        return Utils.isSubset( set, this.items );
    }

    public boolean hasSubset( SkylinePoint other )
    {
        return Utils.isSubset( other.items, this.items );
    }

    public double sumValue()
    {
        return this.sumValue;
    }

    public double submodularValue()
    {
        return this.submValue;
    }

    public int[] extend( int item ) {
        int[] newitems = Arrays.copyOf( this.items, this.items.length+1 );
        newitems[ newitems.length-1 ] = item;
        Arrays.sort( newitems );
        return newitems;
    }

    public List<int[]> extensionSets( int numItems )
    {
        List<int[]> ext = new LinkedList<>();
        for ( int item = 0; item < numItems; item++ ) {
            if ( Utils.arrayIndexOf( item, this.items ) == -1 ) {
                int[] newItems = Arrays.copyOf( this.items,
                                                this.items.length+1 );
                newItems[ newItems.length-1 ] = item;
                Arrays.sort( newItems );
                ext.add( newItems );
            }
        }
        return ext;
    }

    // This mimics the hash function used in Python for tuples.
    public int hashCode()
    {
        return Utils.arrayHashCode( this.items );
    }

    // Does not consider sumValue or submValue.
    // These are assumed to have been computed using the same Evaluator instance.
    // If not, results may be weird. Or not.
    public boolean equals( Object other )
    {
        SkylinePoint point = (SkylinePoint)other;
        for ( int i = 0; i < this.items.length; i++ ) {
            if ( this.items[i] != point.items[i] )
                return false;
        }
        return true;
    }

    public String itemsString()
    {
        return Arrays.toString( this.items );
    }
}
