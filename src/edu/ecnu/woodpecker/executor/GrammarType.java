package edu.ecnu.woodpecker.executor;

/**
 * Grammar type of middle result
 *
 */
public enum GrammarType
{
    ZERO_GRAMMAR(0), FIRST_GRAMMAR(1), SECOND_GRAMMAR(2), THIRD_GRAMMAR(3), FOURTH_GRAMMAR(4), FIFTH_GRAMMAR(5), SIXTH_GRAMMAR(6);

    private final int value;

    private GrammarType(int value)
    {
        this.value = value;
    }

    public int getValue()
    {
        return value;
    }
}
