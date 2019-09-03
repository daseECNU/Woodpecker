package edu.ecnu.woodpecker.executor.keyword;

import edu.ecnu.woodpecker.executor.GrammarType;

/**
 * Using state pattern
 *
 */
@FunctionalInterface
public interface Keyword
{
    /**
     * Handle various keywords in Woodpecker
     * 
     * @param keyword The line of middle result exclude line number and grammar type
     * @param type grammar type
     * @throws Exception
     */
    public abstract void handle(String keyword, GrammarType type) throws Exception;

}
