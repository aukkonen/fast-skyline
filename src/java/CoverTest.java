import java.util.Map;
import java.util.HashMap;

public class CoverTest
{
    public static void main( String[] args ) throws Exception
    {
        Evaluator sgs    = new SubgroupSetEvaluatorIntCover();
        Evaluator sgsbit = new SubgroupSetEvaluatorBitCover();

        Map<String,String> opts = new HashMap<>();
        opts.put( "q", args[0] );
        opts.put( "c", args[1] );

        if ( args.length > 2 ) {
            opts.put( "max", "" );
        }

        sgs.init( opts );
        sgsbit.init( opts );

        System.err.printf( "%f\t%f\n", sgs.submodularFnc( 0 ), sgsbit.submodularFnc( 0 ) );
        System.err.printf( "%f\t%f\n",
                           sgs.submodularFnc( new int[] {0, 1, 2} ),
                           sgsbit.submodularFnc( new int[] {0, 1, 2} ) );

    }
}
