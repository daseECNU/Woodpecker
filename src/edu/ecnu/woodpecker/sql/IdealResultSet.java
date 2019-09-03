package edu.ecnu.woodpecker.sql;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.jsp.jstl.sql.Result;

import edu.ecnu.woodpecker.constant.DataType;
import edu.ecnu.woodpecker.constant.DataValueConstant;
import edu.ecnu.woodpecker.constant.FileConstant;
import edu.ecnu.woodpecker.constant.LogLevelConstant;
import edu.ecnu.woodpecker.controller.TestController;
import edu.ecnu.woodpecker.log.WpLog;

/**
 * 自定义的返回结果集数据类型 主要功能：提供该数据类型的支持，支持正确结果集的导入
 */
public class IdealResultSet
{

    /**
     * 返回结果集每个属性的数据类型
     */
    private DataType[] dataTypes = null;

    /**
     * 以字符串类型存储理想结果集
     */
    private String[][] data = null;

    public IdealResultSet()
    {}

    public void setDataTypes(DataType[] dataTypes)
    {
        this.dataTypes = dataTypes;
    }

    public void setData(List<ArrayList<String>> data)
    {
        int rowSize = data.size();
        int columnSize = data.get(0).size();
        this.data = new String[rowSize][columnSize];
        for (int i = 0; i < rowSize; i++)
            data.get(i).toArray(this.data[i]);
    }

    public String[][] getData()
    {
        return data;
    }

    /**
     * 与一个数据库操作返回结果集进行比较 默认数据库操作返回结果集的schema与自己相同，对此不作过多的判断
     * 
     * @param result 返回结果集，比较对象
     * @param isSetType True means result set's row order is not matter
     * @return 完全相同返回true，否则返回false
     */
    public boolean equals(Result result, boolean isSetType)
    {
        WpLog.recordLog(LogLevelConstant.INFO, "compare query result set with ideal result set");
        WpLog.recordQueryAndIdealResultSet(LogLevelConstant.INFO, result, this);
        int rowCount = result.getRowCount();
        int columnCount = result.getColumnNames().length;
        if (rowCount != data.length || columnCount != dataTypes.length)
        {
            // The sizes of two result set are different
            return false;
        }

        Object[][] realResultSetData = result.getRowsByIndex();
        // Compare strictly
        if (!isSetType)
        {
            for (int i = 0; i < rowCount; i++)
            {
                for (int j = 0; j < columnCount; j++)
                {
                    if (!equalsOneElement(realResultSetData[i][j], i, j))
                        return false;
                }
            }
            return true;
        }
        // Compare with set type, violent method
        Set<Integer> comparedSet = new HashSet<>();
        for (int i = 0; i < rowCount; i++)
        {
            boolean findOneRowMatch = false;
            for (int j = 0; j < rowCount; j++)
            {
                if (comparedSet.contains(j))
                    continue;
                if (equalsOneRow(i, realResultSetData[j]))
                {
                    findOneRowMatch = true;
                    comparedSet.add(j);
                    break;
                }
            }
            if (!findOneRowMatch)
                return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < data.length; i++)
            stringBuilder.append(FileConstant.TAB_CHAR).append(Arrays.toString(data[i])).append(FileConstant.LINUX_LINE_FEED);
        return stringBuilder.toString();
    }
    
    /**
     * Get the data according to specified row and column
     * @param row
     * @param column
     * @return
     */
    public Object getDataByIndex(int row, int column)
    {
        return data[row][column];
    }
    
    /**
     * 从文件中导入正确的结果集
     * 
     * @param file 结果集存放的文件
     * @param idealResultSets
     * @param positions 
     */
    public static void importIRS(File file, IdealResultSet[] idealResultSets, int[] positions)
    {
        WpLog.recordLog(LogLevelConstant.INFO, "Start to import ideal result set");
        for (int i = 0; i < idealResultSets.length; i++)
            importOneResult(file, idealResultSets[i], positions[i]);
    }
    
    /**
     * 不同结果集之间的分隔符为：一行“[-]+.*”；不同记录之间的分隔符为：“\n”或者“\r\n”； 不同元素之间的分隔符为：“|”
     * 
     * @param file
     * @param idealResultSet
     * @param position
     */
    private static void importOneResult(File file, IdealResultSet idealResultSet, int position)
    {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), FileConstant.UTF_8)))
        {
            WpLog.recordLog(LogLevelConstant.INFO, "Import one ideal result set");
            String inputLine = null;
            int index = 0; // To index the number of ideal result set
            List<ArrayList<String>> data = new ArrayList<ArrayList<String>>();
            ArrayList<String> rowValue = new ArrayList<String>();

            while ((inputLine = reader.readLine()) != null)
            {
                if (inputLine.matches("\\s*(--).*"))  // Split line
                {
                    if (index == position)
                    {
                        // Import an ideal result set
                        idealResultSet.setData(data);
                        WpLog.recordIdealResultSet(LogLevelConstant.INFO, idealResultSet);
                        break;
                    }
                    index++;
                    continue;
                }
                if (index == position)
                {
                    // Get an row of an ideal result set
                    rowValue.addAll(Arrays.asList(importOneLine(inputLine.split("\\|"))));
                    data.add(rowValue);
                    rowValue = new ArrayList<String>();
                }
            }
        }
        catch (IOException e)
        {
            WpLog.recordLog(LogLevelConstant.ERROR, WpLog.getExceptionInfo(e));
        }
    }

    /**
     * 每次导入一行数据，每列的String都会trim，对于字符串会将两边的单引号去除
     * 
     * @param arrays 用'|'划分每行得到的数组
     * @return
     */
    private static String[] importOneLine(String[] arrays)
    {
        WpLog.recordLog(LogLevelConstant.INFO, "Import one line into ideal result set");
        List<String> list = new ArrayList<String>();
        int countSingleQuotation = 0;
        StringBuilder strColumnValue = new StringBuilder();
        for (String ele : arrays)
        {
            // 空行
            if (ele.matches("^\\s*$"))
                continue;

            if (ele.matches("^\\s*'.*'\\s*$") && !ele.trim().endsWith("\\'"))
            {
                // 一条完整的字符串
                ele = ele.replace("\\'", "'");
                ele = ele.replace("\\\\", "\\");
                list.add(ele.trim().substring(1, ele.trim().length() - 1));
            }
            else if (ele.matches("^\\s*'.*$"))
            {
                // 一条字符串开头，防止遇到字符串中有空格刚好被分割，所以不trim
                countSingleQuotation++;
                ele = ele.replace("\\'", "'");
                ele = ele.replace("\\\\", "\\");
                strColumnValue.append(ele);
            }
            else if (ele.matches("^.*\'\\s*$"))
            {
                // 一条字符串结束
                countSingleQuotation--;
                ele = ele.replace("\\'", "'");
                ele = ele.replace("\\\\", "\\");
                strColumnValue.append("|" + ele);
                String tmp = strColumnValue.toString().trim();
                // 去除字符串两边的单引号
                list.add(tmp.substring(1, tmp.length() - 1).trim());
                strColumnValue.delete(0, strColumnValue.length());
            }
            else if (countSingleQuotation != 0)
            {
                // 字符串的一个组成部分
                ele = ele.replace("\\'", "'");
                ele = ele.replace("\\\\", "\\");
                strColumnValue.append("|" + ele);
            }
            else
            {
                // 非字符串
                ele = ele.trim();
                // 如果是大写NULL就转为小写
                if(ele.equals(DataValueConstant.NULL_UPPER))
                    list.add(ele.toLowerCase());
                else
                    list.add(ele);
            }
        }
        return list.toArray(new String[0]);
    }
    
    /**
     * 
     * @param row The row index of ideal result set
     * @param realResultSetDataRow The row of real result set
     * @return
     */
    private boolean equalsOneRow(int row, Object[] realResultSetDataRow)
    {
        int columnCount = realResultSetDataRow.length;
        for (int column = 0; column < columnCount; column++)
        {
            if (!equalsOneElement(realResultSetDataRow[column], row, column))
                return false;
        }
        return true;
    }
    
    /**
     * Return the compare result of one element of real data with ideal result set's
     * specified element
     * 
     * @param realDataElement
     * @param row  The row position of ideal result set
     * @param column  The column position of ideal result set
     * @return false if exist non-equal element
     */
    private boolean equalsOneElement(Object realDataElement, int row, int column)
    {
        // 如有不相等则返回false
        boolean equal = true;
        switch (dataTypes[column])
        {
        case INT:
            if (data[row][column].equals(DataValueConstant.NULL_LOWER))
                equal = realDataElement == null ? true : false;
            else
            {
                if (TestController.getDatabase() == DbmsBrand.CEDAR)
                {
                    // 为某破数据库单独加实现逻辑，他的int返回的时候用long表示
                    equal = realDataElement == null ? false : Long.parseLong(data[row][column]) == Long.parseLong(realDataElement.toString());
                    break;
                }
                equal = realDataElement == null ? false : Integer.parseInt(data[row][column]) == Integer.parseInt(realDataElement.toString());
            }
            break;
        case LONG:
            if (data[row][column].equals(DataValueConstant.NULL_LOWER))
                equal = realDataElement == null ? true : false;
            else
            {
                equal = realDataElement == null ? false : Long.parseLong(data[row][column]) == Long.parseLong(realDataElement.toString());
            }
            break;
        case FLOAT:
            if (data[row][column].equals(DataValueConstant.NULL_LOWER))
                equal = realDataElement == null ? true : false;
            else
            {
                equal = realDataElement == null ? false : Float.parseFloat(data[row][column]) == Float.parseFloat(realDataElement.toString());
            }
            break;
        case DOUBLE:
            if (data[row][column].equals(DataValueConstant.NULL_LOWER))
                equal = realDataElement == null ? true : false;
            else
            {
                equal = realDataElement == null ? false : Double.parseDouble(data[row][column]) == Double.parseDouble(realDataElement.toString());
            }
            break;
        case STRING:
            if (data[row][column].equals(DataValueConstant.NULL_LOWER))
                equal = realDataElement == null ? true : false;
            else
                equal = realDataElement == null ? false : data[row][column].equals((String) realDataElement);
            break;
        case DECIMAL:
            if (data[row][column].equals(DataValueConstant.NULL_LOWER))
                equal = realDataElement == null ? true : false;
            else
            {
                equal = realDataElement == null ? false
                        : new BigDecimal(data[row][column]).compareTo(new BigDecimal(realDataElement.toString())) == 0 ? true : false;
            }
            break;
        case BOOLEAN:
            if (data[row][column].equals(DataValueConstant.NULL_LOWER))
                equal = realDataElement == null ? true : false;
            else
            {
                if (realDataElement.toString().matches("(\\p{Digit})+"))
                {
                    int real = Integer.parseInt(realDataElement.toString());
                    boolean ideal = Boolean.parseBoolean(data[row][column]);
                    equal = (real == 1 && ideal) || (real == 0 && !ideal) ? true : false;
                }
                else
                {
                    equal = Boolean.parseBoolean(data[row][column]) == Boolean.parseBoolean(realDataElement.toString()) ? true
                            : false;
                }
            }
            break;
        case TIMESTAMP:
            if (data[row][column].equals(DataValueConstant.NULL_LOWER))
                equal = realDataElement == null ? true : false;
            else
            {
                equal = realDataElement == null ? false
                        : Timestamp.valueOf(data[row][column]).compareTo((Timestamp) realDataElement) == 0 ? true : false;
            }
            break;
        default:
            WpLog.recordLog(LogLevelConstant.ERROR, "Unsupported data type: %s", dataTypes[column].getShortName());
            equal = false;
        }
        return equal;
    }
}
