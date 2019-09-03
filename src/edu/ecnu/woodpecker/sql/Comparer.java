package edu.ecnu.woodpecker.sql;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.jsp.jstl.sql.Result;

import edu.ecnu.woodpecker.constant.DataTypeConstant;
import edu.ecnu.woodpecker.constant.LogLevelConstant;
import edu.ecnu.woodpecker.constant.SignConstant;
import edu.ecnu.woodpecker.log.WpLog;

/**
 * 支持测试返回结果集与正确结果集的对比
 */
public class Comparer
{

    /**
     * 支持两个元素的对比 支持的数据类型有： 基本类型：boolean，char，int，long，float，double
     * 对象类型：Boolean，Character，Integer，Long，Float，Double，String，
     * BigDecimal，ResultSet，IdealResultSet. input1与input2数据类型必须相同（不相同则返回false）
     * ResultSet和IdealResultSet在逻辑上可认为是相同的数据类型，比较时IdealResultSet
     * 必须作为第一个参数！！！并且它俩之间仅支持“==”比较 基本类型数据传入后会自动转化为对应的对象类型
     * 
     * @param input1 作对比的两个元素之一
     * @param input2 作对比的两个元素之一
     * @param operator 比较运算符，可为：>，>=，<，<=，==
     * @param isSetType True means the result set's row order is not matter
     * @return 比较结果。input1与input2相等则返回true，否则返回false
     */
    public static <T> boolean verify(T input1, String operator, T input2, boolean isSetType)
    {
        String dataType1 = input1.getClass().getName();
        String dataType2 = input2.getClass().getName();
        boolean result = false;

        if (dataType1.matches(".*IdealResultSet") && dataType2.matches(".*ResultImpl"))
        {
        	if (!operator.equals(SignConstant.EQUAL) && 
            		!operator.equals(SignConstant.ALL_ARE) &&
            		!operator.equals(SignConstant.CONTAIN))
            {
                WpLog.recordLog(LogLevelConstant.ERROR,
                        "Unsupport relation operator %s when compare IdealResultSet and ResultSet", operator, null);
                return false;
            }

            // 利用反射调用IdealResultSet的equals函数
            try
            {
                return (boolean) input1.getClass().getMethod("equals", Result.class, boolean.class).invoke(input1, input2, isSetType);
            }
            catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e)
            {
                WpLog.recordLog(LogLevelConstant.ERROR, WpLog.getExceptionInfo(e));
                return result;
            }
        }
        else if (dataType1.matches(".*ResultImpl") && dataType2.matches(".*ResultImpl"))
        {
            // 对比的两个结果集都是真实查询得到的
        	if (!operator.equals(SignConstant.EQUAL) &&
            		!operator.equals(SignConstant.ALL_ARE) &&
            		!operator.equals(SignConstant.CONTAIN))
            {
                WpLog.recordLog(LogLevelConstant.ERROR,
                        "Unsupport relation operator %s when compare IdealResultSet and ResultSet", operator);
                return false;
            }
        	if(operator.equals(SignConstant.EQUAL))
            {
            	return equals((Result) input1, (Result) input2, isSetType, false, false);
            }
            else if(operator.equals(SignConstant.ALL_ARE))
            {
            	return equals((Result) input1, (Result) input2, true, true, false);
            }
            else
            {
            	return equals((Result) input1, (Result) input2, true, false, true);
            }
        }
        else if (!dataType1.equals(dataType2))
        {
            return false;
        }

        if (operator.equals(SignConstant.EQUAL))
        {
            result = input1.equals(input2);
        }
        else if (operator.equals(SignConstant.GT_STR))
        {
            result = greaterThan(input1, input2);
        }
        else if (operator.equals(SignConstant.GTOE))
        {
            result = equalOrGreaterThan(input1, input2);
        }
        else if (operator.equals(SignConstant.LT_STR))
        {
            result = lessThan(input1, input2);
        }
        else if (operator.equals(SignConstant.LTOE))
        {
            result = equalOrLessThan(input1, input2);
        }
        else if (operator.equals(SignConstant.NON_EQUAL))
        {
            result = !input1.equals(input2);
        }
        else
        {
            WpLog.recordLog(LogLevelConstant.ERROR, "Unsupport relation operator %s", operator);
            return false;
        }
        return result;
    }

    /**
     * 支持两个数组的对比 支持的数据类型有：Boolean[]，Character[]，Integer[]，Long[]，Float[]，
     * Double[]，String[]，BigDecimal[] input1与input2数据类型必须相同（不相同则返回false）
     * 
     * @param input1 作对比的两个元素之一
     * @param input2 作对比的两个元素之一
     * @param operator 比较运算符，可为：>，>=，<，<=，==
     * @return 比较结果。input1与input2中元素按序相等则返回true，否则返回false
     */
    public static <T> boolean verify(T[] input1, String operator, T[] input2)
    {
        if (input1.length != input2.length)
            return false;
        for (int i = 0; i < input1.length; i++)
        {
            boolean result = verify(input1[i], operator, input2[i], false);
            if (result == false)
                return false;
        }
        return true;
    }
    
    /**
     * 支持两个数组的对比 支持的数据类型有：Boolean[]，Character[]，Integer[]，Long[]，Float[]，
     * Double[]，String[]，BigDecimal[] input1[]与input2数据类型必须相同（不相同则返回false）
     * 
     * @param input1[] 作对比的两个元素之一
     * @param input2 作对比的两个元素之一
     * @param operator 为ALL_ARE
     * @return 比较结果。input1与input2中元素按序相等则返回true，否则返回false
     */
    public static <T> boolean verify(T[] input1, String operator, T input2)
    {
        for (int i = 0; i < input1.length; i++)
        {
            boolean result = verify(input1[i], SignConstant.EQUAL, input2, false);
            if (result == false)
                return false;
        }
        return true;
    }

    /**
     * 支持两个列表的对比 支持的数据类型有：List<Boolean>，List<Character>，List<Integer>，List<Long>， List
     * <Float>，List<Double>，List<String>，List <BigDecimal>
     * input1与input2数据类型必须相同（不相同则返回false）
     * 
     * @param input1 作对比的两个元素之一
     * @param input2 作对比的两个元素之一
     * @param operator 比较运算符，可为：>，>=，<，<=，==
     * @return 比较结果。input1与input2中元素按序相等则返回true，否则返回false
     */
    public static <T> boolean verify(List<T> input1, String operator, List<T> input2)
    {
        if (input1.size() != input2.size())
            return false;
        for (int i = 0; i < input1.size(); i++)
        {
            boolean result = verify(input1.get(i), operator, input2.get(i), false);
            if (result == false)
                return false;
        }
        return true;
    }

    /**
     * “>”比较的实现 支持的数据类型有：Integer，Long，Float，Double，BigDecimal
     * 
     * @param input1 左元素
     * @param input2 右元素
     * @return 比较的结果
     */
    private static <T> boolean greaterThan(T input1, T input2)
    {
        boolean result = false;
        String dataType = input1.getClass().getName();
        if (dataType.equals(DataTypeConstant.INT_FULL))
        {
            result = new Integer(input1.toString()) > new Integer(input2.toString()) ? true : false;
        }
        else if (dataType.equals(DataTypeConstant.LONG_FULL))
        {
            result = new Long(input1.toString()) > new Long(input2.toString()) ? true : false;
        }
        else if (dataType.equals(DataTypeConstant.FLOAT_FULL))
        {
            result = new Float(input1.toString()) > new Float(input2.toString()) ? true : false;
        }
        else if (dataType.equals(DataTypeConstant.DOUBLE_FULL))
        {
            result = new Double(input1.toString()) > new Double(input2.toString()) ? true : false;
        }
        else if (dataType.equals(DataTypeConstant.DECIMAL_FULL))
        {
            result = new BigDecimal(input1.toString()).subtract(new BigDecimal(input2.toString())).doubleValue() > 0 ? true : false;
        }
        return result;
    }

    /**
     * “>=”比较的实现 支持的数据类型有：Integer，Long，Float，Double，BigDecimal
     * 
     * @param input1 左元素
     * @param input2 右元素
     * @return 比较的结果
     */
    private static <T> boolean equalOrGreaterThan(T input1, T input2)
    {
        boolean result = false;
        String dataType = input1.getClass().getName();
        if (dataType.equals(DataTypeConstant.INT_FULL))
        {
            result = new Integer(input1.toString()) >= new Integer(input2.toString()) ? true : false;
        }
        else if (dataType.equals(DataTypeConstant.LONG_FULL))
        {
            result = new Long(input1.toString()) >= new Long(input2.toString()) ? true : false;
        }
        else if (dataType.equals(DataTypeConstant.FLOAT_FULL))
        {
            result = new Float(input1.toString()) >= new Float(input2.toString()) ? true : false;
        }
        else if (dataType.equals(DataTypeConstant.DOUBLE_FULL))
        {
            result = new Double(input1.toString()) >= new Double(input2.toString()) ? true : false;
        }
        else if (dataType.equals(DataTypeConstant.DECIMAL_FULL))
        {
            result = new BigDecimal(input1.toString()).subtract(new BigDecimal(input2.toString())).doubleValue() >= 0 ? true : false;
        }
        return result;
    }

    /**
     * “<”比较的实现 支持的数据类型有：Integer，Long，Float，Double，BigDecimal
     * 
     * @param input1 左元素
     * @param input2 右元素
     * @return 比较的结果
     */
    private static <T> boolean lessThan(T input1, T input2)
    {
        boolean result = false;
        String dataType = input1.getClass().getName();
        if (dataType.equals(DataTypeConstant.INT_FULL))
        {
            result = new Integer(input1.toString()) < new Integer(input2.toString()) ? true : false;
        }
        else if (dataType.equals(DataTypeConstant.LONG_FULL))
        {
            result = new Long(input1.toString()) < new Long(input2.toString()) ? true : false;
        }
        else if (dataType.equals(DataTypeConstant.FLOAT_FULL))
        {
            result = new Float(input1.toString()) < new Float(input2.toString()) ? true : false;
        }
        else if (dataType.equals(DataTypeConstant.DOUBLE_FULL))
        {
            result = new Double(input1.toString()) < new Double(input2.toString()) ? true : false;
        }
        else if (dataType.equals(DataTypeConstant.DECIMAL_FULL))
        {
            result = new BigDecimal(input1.toString()).subtract(new BigDecimal(input2.toString())).doubleValue() < 0 ? true : false;
        }
        return result;
    }

    /**
     * “<=”比较的实现 支持的数据类型有：Integer，Long，Float，Double，BigDecimal
     * 
     * @param input1 左元素
     * @param input2 右元素
     * @return 比较的结果
     */
    private static <T> boolean equalOrLessThan(T input1, T input2)
    {
        boolean result = false;
        String dataType = input1.getClass().getName();
        if (dataType.equals(DataTypeConstant.INT_FULL))
        {
            result = new Integer(input1.toString()) <= new Integer(input2.toString()) ? true : false;
        }
        else if (dataType.equals(DataTypeConstant.LONG_FULL))
        {
            result = new Long(input1.toString()) <= new Long(input2.toString()) ? true : false;
        }
        else if (dataType.equals(DataTypeConstant.FLOAT_FULL))
        {
            result = new Float(input1.toString()) <= new Float(input2.toString()) ? true : false;
        }
        else if (dataType.equals(DataTypeConstant.DOUBLE_FULL))
        {
            result = new Double(input1.toString()) <= new Double(input2.toString()) ? true : false;
        }
        else if (dataType.equals(DataTypeConstant.DECIMAL_FULL))
        {
            result = new BigDecimal(input1.toString()).subtract(new BigDecimal(input2.toString())).doubleValue() <= 0 ? true : false;
        }
        return result;
    }

    /**
     * 对比两个真实结果集是否相等
     * 
     * @param result1
     * @param result2
     * @param isSetType True means result set's row order is not matter
     * @return
     */
    private static boolean equals(Result result1, Result result2, boolean isSetType, boolean isAllAre, boolean isContain)
    {
        // 大小不一致则不相等
    	if(result1.getColumnNames().length != result2.getColumnNames().length)
    		return false;
    	if(!isAllAre && !isContain)
    	{
    		if (result1.getRowCount() != result2.getRowCount())
    			return false;
    	}
    	else
    	{
    		if(isContain)
    			if (result1.getRowCount() < result2.getRowCount())
        			return false;
    	}
        // Compare strictly
        Object[][] realData1 = result1.getRowsByIndex();
        Object[][] realData2 = result2.getRowsByIndex();
        if (!isSetType)
        {
            for (int i = 0; i < realData1.length; i++)
            {
                for (int j = 0; j < realData1[0].length; j++)
                {
                    if (realData1[i][j] == null || realData2[i][j] == null)
                    {
                        if (realData1[i][j] != realData2[i][j])
                            return false;
                        continue;
                    }
                    if (!realData1[i][j].equals(realData2[i][j]))
                        return false;
                }
            }
            return true;
        }
        // Compare with set type, violent method
        if(isContain)
        {
          //用來處理結果集中包含多個相同元素，通過compareSet可以跳過另一個結果集中已經被比較的元素。
            Set<Integer> comparedSet = new HashSet<>();
            for (int j = 0; j < realData2.length; j++)
            {
                boolean findOneRowMatch = false;
                for (int i = 0; i < realData1.length; i++)
                {
                    if (comparedSet.contains(j))
                        continue;
                    if (equalsOneRow(realData1[i], realData2[j]))
                    {
                        findOneRowMatch = true;               
                        comparedSet.add(j);
                        break;
                    }
                } 
                if (!findOneRowMatch)
                    return false;
            } 
        }
        else
        {
            //用來處理結果集中包含多個相同元素，通過compareSet可以跳過另一個結果集中已經被比較的元素。
            Set<Integer> comparedSet = new HashSet<>();
            for (int i = 0; i < realData1.length; i++)
            {
                boolean findOneRowMatch = false;
                for (int j = 0; j < realData2.length; j++)
                {
                    if(!isAllAre)
                    {
                        if (comparedSet.contains(j))
                            continue;
                    } 
                    if (equalsOneRow(realData1[i], realData2[j]))
                    {
                        findOneRowMatch = true;
                        if(!isAllAre)
                        {
                            comparedSet.add(j);
                        }
                        break;
                    }
                } 
                if (!findOneRowMatch)
                    return false;
            }
        }
            
        
        
        
        return true;
    }

    /**
     * Return the compare result of two rows
     * 
     * @param row1
     * @param row2
     * @return
     */
    private static boolean equalsOneRow(Object[] row1, Object[] row2)
    {
        if (row1.length != row2.length)
            return false;
        for (int i = 0; i < row1.length; i++)
        {
            System.out.println(row1[i]);
            System.out.println(row2[i]);
            if(row1[i] == null && row2[i] == null)
            {
                continue;
            }
            if(row1[i] == null || row2[i] == null)
            {
                return false;
            }
            if (!row1[i].equals(row2[i]))
                return false;
        }
        return true;
    }
}
