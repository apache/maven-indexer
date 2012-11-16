package org.apache.maven.index;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple support for components.
 *
 * @author cstamas
 * @since 5.1
 */
public abstract class ComponentSupport
{

    private final Logger logger;

    protected ComponentSupport()
    {
        this.logger = createLogger();
    }

    protected Logger createLogger()
    {
        return LoggerFactory.getLogger( getClass() );
    }

    protected Logger getLogger()
    {
        return logger;
    }
}
