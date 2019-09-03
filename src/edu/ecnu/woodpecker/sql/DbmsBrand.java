package edu.ecnu.woodpecker.sql;

import java.util.stream.Stream;

import edu.ecnu.woodpecker.constant.ConfigConstant;

public enum DbmsBrand
{
    MYSQL(ConfigConstant.MYSQL), CEDAR(ConfigConstant.CEDAR), CEDAR_DATA_SOURCE(ConfigConstant.CEDAR_DATASOURCE),
    VOLTDB(ConfigConstant.VOLTDB), POSTGRESQL(ConfigConstant.POSTGRESQL), SQL_SERVER(ConfigConstant.SQL_SERVER);

    private String brand = null;

    private DbmsBrand(String brand)
    {
        this.brand = brand;
    }

    public String getBrand()
    {
        return brand;
    }
    
    /**
     * Return brand except cedar, convert cedar to mysql
     * @param brand
     * @return
     */
    public static String getBrand(String brand)
    {
        return brand.equals(ConfigConstant.CEDAR) ? MYSQL.brand : brand;
    }
    
    public String toString()
    {
        return brand;
    }
    
    /**
     * Return a DbmsBrand object according specified value
     * @param value
     * @return
     */
    public static DbmsBrand of(String value)
    {
        String trimBrand = value.trim().toLowerCase();
        Stream<DbmsBrand> stream = Stream.of(DbmsBrand.class.getEnumConstants());
        return stream.filter(ele -> ele.getBrand().equals(trimBrand)).findAny().get();
    }
}
