package edu.ecnu.woodpecker.executor;

import java.util.NoSuchElementException;
import java.util.stream.Stream;

/**
 * The enum of stress test's operator, include "clean", "remain", "synchronous",
 * "asynchronous"
 *
 */
public enum StressTestOperator
{
    CLEAN("clean"), REMAIN("remain"), SYNCHRONOUS("synchronous"), ASYNCHRONOUS("asynchronous");

    private String value = null;

    private StressTestOperator(String value)
    {
        this.value = value;
    }

    public String getValue()
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
    public static StressTestOperator of(String value) throws NoSuchElementException
    {
        Stream<StressTestOperator> stream = Stream.of(StressTestOperator.class.getEnumConstants());
        return stream.filter(ele -> ele.getValue().equals(value)).findAny().get();
    }
}
