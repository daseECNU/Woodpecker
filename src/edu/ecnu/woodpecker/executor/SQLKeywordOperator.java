package edu.ecnu.woodpecker.executor;

import java.util.NoSuchElementException;
import java.util.stream.Stream;

/**
 *  The operator of SQL keyword
 *
 */
public enum SQLKeywordOperator
{
    READ(1), READ_ERROR(-1),
    WRITE(2), WRITE_ERROR(-2),
    CEDAR_EXPLAIN(3), CEDAR_EXPLAIN_ERROR(-3);
    
    /**
     * The id of operator
     */
    private int value;
    
    private SQLKeywordOperator(int value)
    {
        this.value = value;
    }
    
    public int getValue()
    {
        return value;
    }
    
    /**
     * Returns an enum constant of the specified value
     * 
     * @param value
     * @return
     * @throws NoSuchElementException
     */
    public static SQLKeywordOperator valueOf(int value) throws NoSuchElementException
    {
        Stream<SQLKeywordOperator> stream = Stream.of(SQLKeywordOperator.class.getEnumConstants());
        return stream.filter(ele -> ele.getValue() == value).findAny().get();
    }
}
