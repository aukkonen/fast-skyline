import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class SearchTreeNode
{
    public int[]  pos;
    public int    j;
        
    public SearchTreeNode( int[] pos, int j )
    {
        this.pos = pos;
        this.j   = j;
    }

    public int size()
    {
        return this.pos.length;
    }
    
    public SearchTreeNode shiftLeft()
    {
        int[] newpos = Arrays.copyOf( this.pos, this.pos.length );
        for ( int i = 0; i < newpos.length; i++ ) {
            newpos[i]--;
        }
        return new SearchTreeNode( newpos, this.j );
    }

    public SearchTreeNode left()
    {
        int[] newpos = Arrays.copyOf( this.pos, this.pos.length );
        int   jprime   = this.j - 1;
        if ( jprime == -1 ) {
            // I think we're never supposed to get here... let's see.
            System.err.printf( "ExactSkyline: FORBIDDEN PLACE!\n" );
            System.err.printf( "%s, %d\n", Arrays.toString( this.pos ), this.j );
            return null;
        }
        newpos[jprime] = this.pos[jprime] + 1;
        return new SearchTreeNode( newpos, jprime );
    }
    
    public int[] copyOfItems()
    {
        return Arrays.copyOf( this.pos, this.pos.length );
    }

    public List<SearchTreeNode> children( Evaluator eval )
    {
        List<SearchTreeNode> children = new LinkedList<SearchTreeNode>();
        // left child exists if j > 0.
        if ( this.j > 0 ) {
            children.add( child( this.j-1 ) );
        }
        // right child exists if pos.pos[j] + 1 < pos.pos[j+1]
        int x;
        if ( this.j == this.pos.length-1 ) {
            x = eval.numItems();
        }
        else {
            x = this.pos[ this.j + 1 ];
        }
        if ( this.pos[ this.j ] + 1 < x ) {
            children.add( child( this.j ) );
        }
        return children;
    }
    
    // This mimics the hash function used in Python for tuples.
    public int hashCode()
    {
        int value = Arrays.hashCode( this.pos );
        value = value*1000003 ^ j;
        value = value ^ 2;
        return value;
    }
    
    public boolean equals( Object other )
    {
        SearchTreeNode stn = (SearchTreeNode)other;
        if ( this.j != stn.j ) {
            return false;
        }
        return Arrays.equals( this.pos, stn.pos );
    }
    
    public String toString()
    {
        return Arrays.toString( this.pos ) + ", j = " + this.j;
    }

    private SearchTreeNode child( int j )
    {
        int[] newPos = Arrays.copyOf( this.pos, this.pos.length );
        newPos[j]++;
        return new SearchTreeNode( newPos, j );
    }
}
