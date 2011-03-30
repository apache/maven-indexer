package org.apache.maven.index.util.zip;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * A very simplistic approach to hide the underlying mech to deal with ZipFiles, suited for use cases happening in Maven
 * Indexer.
 * 
 * @author cstamas
 */
public interface ZipHandle
{
    /**
     * Returns true if Zip file this handle is pointing to contains an entry at given path.
     * 
     * @param path
     * @return
     */
    boolean hasEntry( String path )
        throws IOException;

    /**
     * Returns a list of string, with each string representing a valid path for existing entry in this Zip handle.
     * 
     * @return
     */
    List<String> getEntries();

    /**
     * Returns a list of string, with each string representing a valid path for existing entry in this Zip handle.
     * 
     * @return
     */
    List<String> getEntries( EntryNameFilter filter );

    /**
     * Returns the "payload" (uncompressed) of the entry at given path, or null if no such path exists in the Zip file
     * this handle points to.
     * 
     * @param path
     * @return
     */
    InputStream getEntryContent( String path )
        throws IOException;

    /**
     * Closes the zip handle (performs resource cleanup). This method should be called when this zip handle is not
     * needed anymore, and calling it should be obligatory to prevent resource leaks.
     */
    void close()
        throws IOException;
}
