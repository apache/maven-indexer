package org.apache.maven.index.artifact;

/**
 * Deprecated exception, just made into Runtime exception but NEVER THROWN anymore (since 4.0.1),
 * 
 * @author cstamas
 * @deprecated Not throwed anymore.
 */
public class IllegalArtifactCoordinateException
    extends RuntimeException
{
    private static final long serialVersionUID = 7137593998855995199L;

    public IllegalArtifactCoordinateException( String message )
    {
        super( message );
    }

    public IllegalArtifactCoordinateException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
