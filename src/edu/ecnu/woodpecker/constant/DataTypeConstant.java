package edu.ecnu.woodpecker.constant;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据类型常量类
 * 
 */
public final class DataTypeConstant
{
    public final static String INT_SHORT = "int";
    public final static String INT_FULL = "java.lang.Integer";
    
    public final static String LONG_SHORT = "long";
    public final static String LONG_FULL = "java.lang.Long";
    
    public final static String BOOLEAN_SHORT = "boolean";
    public final static String BOOLEAN_FULL = "java.lang.Boolean";
    
    public final static String CHAR_SHORT = "char";
    public final static String CHAR_FULL = "java.lang.Character";
    
    public final static String DOUBLE_SHORT = "double";
    public final static String DOUBLE_FULL = "java.lang.Double";
    
    public final static String FLOAT_SHORT = "float";
    public final static String FLOAT_FULL = "java.lang.Float";
    
    public final static String DECIMAL_SHORT = "Decimal";
    public final static String DECIMAL_FULL = "java.math.BigDecimal";
    
    public final static String STRING_SHORT = "String";
    public final static String STRING_FULL = "java.lang.String";
    
    public final static String LIST = "List";
    public final static String LIST_INTGER = "List<Integer>";
    public final static String LIST_DOUBLE = "List<Double>";
    public final static String LIST_FLOAT = "List<Float>";
    public final static String LIST_LONG = "List<Long>";
    public final static String LIST_BOOLEAN = "List<Boolean>";
    public final static String LIST_BIGDECIMAL = "List<BigDecimal>";
    public final static String LIST_STRING = "List<String>";
    
    public final static String RESULT_SET = "ResultSet";
    
    public final static String IDEAL_RESULT_SET = "IdealResultSet";
    
    public final static String CONNECTION = "Connection";
    
    public final static String TIMESTAMP_SHORT = "Timestamp";
    public final static String TIMESTAMP_FULL = "java.sql.Timestamp";
    
    /**
     * The map between short data type and full data type
     */
    public static Map<String, String> typeMap = new HashMap<>(20);

    static
    {
        typeMap.put(INT_SHORT, "java.lang.Integer");
        typeMap.put(BOOLEAN_SHORT, "java.lang.Boolean");
        typeMap.put(CHAR_SHORT, "java.lang.Character");
        typeMap.put(FLOAT_SHORT, "java.lang.Float");
        typeMap.put(DOUBLE_SHORT, "java.lang.Double");
        typeMap.put(STRING_SHORT, "java.lang.String");
        typeMap.put(TIMESTAMP_SHORT, "java.sql.Timestamp");
    }
}
