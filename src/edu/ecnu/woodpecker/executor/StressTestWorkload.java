package edu.ecnu.woodpecker.executor;

import java.util.NoSuchElementException;
import java.util.stream.Stream;

/**
 * The enum of stress test's workload
 *
 */
public enum StressTestWorkload
{
    SELECT(1, "select_test"), INSERT(2, "insert_test"), REPLACE(3, "replace_test"), DELETE(4, "delete_test"), UPDATE(5,
            "update_test"), SELECT_UPDATE_BLEND(6, "select_update_test");

    /**
     * The number of workload
     */
    private int value;

    /**
     * The sql table name of workload
     */
    private String tableName = null;

    private StressTestWorkload(int value, String tableName)
    {
        this.value = value;
        this.tableName = tableName;
    }

    public int getValue()
    {
        return value;
    }

    public String getTableName()
    {
        return tableName;
    }

    /**
     * Returns an enum constant of the specified value
     * 
     * @param value
     * @return
     * @throws NoSuchElementException
     */
    public static StressTestWorkload valueOf(int value) throws NoSuchElementException
    {
        Stream<StressTestWorkload> stream = Stream.of(StressTestWorkload.class.getEnumConstants());
        return stream.filter(ele -> ele.getValue() == value).findAny().get();
    }
}
