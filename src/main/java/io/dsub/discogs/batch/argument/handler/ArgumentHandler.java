package io.dsub.discogs.batch.argument.handler;

import io.dsub.discogs.batch.exception.InvalidArgumentException;

/**
 * argument handler that takes care about the application argument. i.e. jdbc url, username,
 * password existence, formatting, etc.
 */
public interface ArgumentHandler {

    /**
     * single method to resolve if argument requirements are met.
     *
     * @param args given arguments.
     * @return resolved arguments, or corrected arguments.
     * @throws InvalidArgumentException thrown if argument requirements are not met.
     */
    String[] resolve(String[] args) throws InvalidArgumentException;
}