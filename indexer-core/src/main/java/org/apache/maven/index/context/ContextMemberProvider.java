package org.apache.maven.index.context;

import java.util.Collection;

public interface ContextMemberProvider
{
    Collection<IndexingContext> getMembers();
}
