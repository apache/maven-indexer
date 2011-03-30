package org.apache.maven.index.util.zip;

public interface EntryNameFilter
{
    boolean accepts( String entryName );
}
