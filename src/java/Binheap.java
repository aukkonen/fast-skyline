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

public class Binheap
{
    private BinheapObject[] data;
    private int              e;
    
    public Binheap( int size )
    {
        this.data = new BinheapObject[ size ];
        this.e    = 0;
    }

    public boolean containsData()
    {
        return this.e > 0;
    }

    public void push( BinheapObject bo )
    {
        int node = this.e;
        int parent = binheapParent( node );
        while ( parent >= 0 && bo.value() < this.data[ parent ].value() ) {
            this.data[ node ] = this.data[ parent ];
            node = parent;
            parent = binheapParent( node );
        }
        this.data[ node ] = bo;
        this.e++;
    }

    public BinheapObject pop()
    {
        BinheapObject retval = this.data[ 0 ];
        this.e--;
        BinheapObject bhobj  = this.data[ this.e ];
        this.data[ this.e ] = null;
        if ( this.e == 0 ) {
            return retval;
        }
        double value  = bhobj.value();
        int    node   = 0;
        int    rchild = binheapRightChild( node );
        while( value > nodeValue( rchild ) || value > nodeValue( rchild-1 ) ) {
            // right child is smaller
            if ( nodeValue( rchild ) < nodeValue( rchild-1 ) ) {
                this.data[ node ] = this.data[ rchild ];
                node = rchild;
            }
            // left child is smaller
            else {
                this.data[ node ] = this.data[ rchild-1 ];
                node = rchild-1;
            }
            rchild = binheapRightChild( node );
        }
        this.data[ node ] = bhobj;
        return retval;
    }

    // Private methods begin here.
    private int binheapParent( int node )
    {
        return (int)Math.floor( (node-1)/2.0 );
    }

    private int binheapRightChild( int node )
    {
        return 2*node + 2;
    }

    private double nodeValue( int node )
    {
        if (node >= this.e ) {
            return Double.POSITIVE_INFINITY;
        }
        return this.data[ node ].value();
    }
}
