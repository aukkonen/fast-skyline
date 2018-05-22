random_instance <- function( numItems, univSize, setSize ) {
    scdata <- sapply( 1:numItems, function(i) {
        x <- rep( 0, univSize )
        x[ sample( univSize, setSize ) ] <- 1
        x
    } )

    values <- runif( numItems );
    list( numItems=numItems, univSize=univSize, values=values, scdata=scdata )
}

