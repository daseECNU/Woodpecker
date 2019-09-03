package edu.ecnu.woodpecker.executor.keyword;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.ecnu.woodpecker.constant.SignConstant;
import edu.ecnu.woodpecker.executor.Executor;
import edu.ecnu.woodpecker.performancetest.Branch;
import edu.ecnu.woodpecker.performancetest.Column;
import edu.ecnu.woodpecker.performancetest.Delete;
import edu.ecnu.woodpecker.performancetest.Dispatcher;
import edu.ecnu.woodpecker.performancetest.Distribution;
import edu.ecnu.woodpecker.performancetest.Filter;
import edu.ecnu.woodpecker.performancetest.Insert;
import edu.ecnu.woodpecker.performancetest.Multiple;
import edu.ecnu.woodpecker.performancetest.NormalDistribution;
import edu.ecnu.woodpecker.performancetest.PrimaryKey;
import edu.ecnu.woodpecker.performancetest.Replace;
import edu.ecnu.woodpecker.performancetest.SQL;
import edu.ecnu.woodpecker.performancetest.Select;
import edu.ecnu.woodpecker.performancetest.SelectForUpdate;
import edu.ecnu.woodpecker.performancetest.Table;
import edu.ecnu.woodpecker.performancetest.Transaction;
import edu.ecnu.woodpecker.performancetest.TransactionBlock;
import edu.ecnu.woodpecker.performancetest.UniformDistribution;
import edu.ecnu.woodpecker.performancetest.UniqueDistribution;
import edu.ecnu.woodpecker.performancetest.Update;
import edu.ecnu.woodpecker.performancetest.Workload;
import edu.ecnu.woodpecker.performancetest.ZIPFDistribution;
import edu.ecnu.woodpecker.util.Util;

public class STKeywordProcessRealize
{
    public STKeywordProcessRealize()
    {
    }

    /**
     * stList包括所有st关键字语句
     */
    public static List<String> stList = new ArrayList<>();

    /**
     * index表示当前执行到的语句位置
     */
    public static int index = 0;

    /**
     * indexSkipLength表示txn，multiple，或branch块的长度
     */
    public static int indexSkipLength = 0;

    /**
     * txnNum表示事务的数量
     */
    public static int txnNum = 0;

    /**
     * tableList存储定义的表模式
     */
    public static List<Table> tableList = new ArrayList<>();

    /**
     * importTableList表示需要导入数据的表模式
     */
    public static List<Table> importTableList = new ArrayList<>();

    /**
     * clearTableList表示需要清空的表模式
     */
    public static List<Table> clearTableList = new ArrayList<>();

    /**
     * transactionList存储定义的事务
     */
    public static List<Transaction> transactionList = new ArrayList<>();

    /**
     * txbList存储事务块
     */
    public static List<TransactionBlock> txbList = new ArrayList<>();

    /**
     * workloadList存储workload
     */
    public static List<Workload> workloadList = new ArrayList<>();

    /**
     * 是否有txn关键字
     */
    public static boolean txn_flag = false;

    /**
     * tableMap中key为表名，value为表名对应的table类
     * tableColMap中key为表名，value为一个map，其中key为列名，value为对应的列类
     */
    public static Map<String, Table> tableMap = new HashMap<>();
    public static Map<String, Map<String, Column>> tableColMap = new HashMap<>();

    /**
     * ST入口执行函数
     */
    public static void execute()
    {
        for (index = 0; index < stList.size(); index++)
        {
            index = index + indexSkipLength;
            indexSkipLength = 0;
            if (index >= stList.size())
            {
                try
                {
                    Executor.index = index;
                    if (Executor.getMIdresult(index).equals("outofindex"))
                        return;
                    Executor.assignStatement();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                break;
            }
            function(index, getKeyword(index));
        }
    }

    /**
     * 根据index在stList列表中找到当前index下的关键字
     * 
     * @param index
     * @return
     */
    public static String getKeyword(int index)
    {
        String keyword = null;
        if (stList.get(index).indexOf("[") != -1)
        {
            keyword = stList.get(index).substring(0,
                    stList.get(index).indexOf(SignConstant.LBRACKET));
        }
        else
        {
            keyword = stList.get(index);
        }
        return keyword;
    }

    @SuppressWarnings("unchecked")
    public static <T> T function(int index, String keyword)
    {
        switch (keyword)
        {
        case "table":
            table(index, stList.get(index));
            break;
        case "column":
            column(stList.get(index));
            break;
        case "import_tbl":
            import_tbl(stList.get(index));
            break;
        case "clear_tbl":
            clear_tbl(stList.get(index));
            break;
        case "select":
            return (T) select(stList.get(index));
            
        case "select_for_update":
            return (T) SelectForUpdate(stList.get(index));
            
        case "delete":
            return (T) delete(stList.get(index));

        case "insert":
            return (T) insert(stList.get(index));

        case "replace":
            return (T) replace(stList.get(index));

        case "update":
            return (T) update(stList.get(index));

        case "txn":
            txn(index, stList.get(index));
            break;
        case "multiple":
            return (T) multiple(index, stList.get(index));

        case "branch":
            return (T) branch(index, stList.get(index));

        case "txn_loading":
            txnLoading(stList.get(index));
            break;
        default:
            break;
        }
        return null;
    }

    /**
     * 定义表模式
     * 
     * @param index
     * @param keywords
     */
    public static void table(int index, String keywords)
    {
        String[] parts = Util.removeBlankElement(keywords.split("\\[|;|]"));
        String[] column = parts[3].toString().split(",");

        Map<String, Column> columnMap = new HashMap<>();// 包括列名和对应列

        List<Column> columns = new ArrayList<>();

        for (int i = 0; i < column.length; i++)
        {
            String[] col_name_type = column[i].split(" ");
            if (col_name_type[1].contains("Decimal"))
            {
                col_name_type[1] = col_name_type[1].replace(':', ',');
            }
            Column col = new Column(col_name_type[0], col_name_type[1]);
            columns.add(col);
            columnMap.put(col_name_type[0], col);
        }

        List<String> pkColumnNames = new ArrayList<String>();
        boolean autoIncrement = true;
        int flag = 1;
        flag = parts[4].indexOf("auto_increment");
        if (flag == -1)
        {
            // 非主键自增
            autoIncrement = false;
            String[] pk = parts[4].toString().split("\\(|\\)");
            String[] pkcol = pk[1].split(" ");
            for (int i = 0; i < pkcol.length; i++)
            {
                pkColumnNames.add(pkcol[i]);
            }
        }
        else
        {
            autoIncrement = true;
            String[] pk = parts[4].toString().split("\\(|,");
            String[] pkcol = pk[1].split(" ");
            for (int i = 0; i < pkcol.length; i++)
            {
                pkColumnNames.add(pkcol[i]);
            }
        }

        PrimaryKey pKey = new PrimaryKey(pkColumnNames, autoIncrement);

        Table table = new Table(parts[1], Long.parseLong(parts[2]), columns, pKey, null, null);
        tableMap.put(parts[1], table);
        tableList.add(table);

        tableColMap.put(parts[1], columnMap);
    }

    /**
     * 处理整个事务。从txn开始到txn_loading结束
     * 
     * @param index
     * @param keywords
     */
    public static void txn(int index, String keywords)
    {
        txn_flag = true;
        String[] parts = Util.removeBlankElement(keywords.split("\\[|]"));
        String txn_name = null;
        int txnBegin = index;
        float txn_ratio = Float.parseFloat(parts[1]);
        txbList = new ArrayList<>();

        while (!getKeyword(index).equals("txn_loading"))
        {
            index++;
            if (getKeyword(index).equals("end_txn"))
            {
                txnNum++;
                txn_name = "txn" + txnNum;
                Transaction txn = new Transaction(txn_name, txn_ratio, txbList);
                transactionList.add(txn);
                txbList = new ArrayList<>();
                index++;
            }
            txbList.add(function(index, getKeyword(index)));
            index = index + indexSkipLength;
            indexSkipLength = 0;
        }
        // 事务长度
        indexSkipLength = index - txnBegin;
    }

    /**
     * 处理multiple分支，从multiple关键字开始，到end_multiple关键字结束
     * 
     * @param index
     * @param keywords
     * @return
     */
    public static Multiple multiple(int index, String keywords)
    {
        String[] parts = Util.removeBlankElement(keywords.split("\\[|;|]"));
        ArrayList<SQL> sqlList = new ArrayList<>();
        int multiplebegin = index;
        while (!getKeyword(index + 1).equals("end_multiple"))
        {
            index++;
            sqlList.add(function(index, getKeyword(index)));
        }
        // 跳过end_multiple
        indexSkipLength = index + 1 - multiplebegin;
        Multiple multiple = new Multiple(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
                sqlList);
        return multiple;
    }

    /**
     * 处理branch分支，从branch关键字开始，到end_branch关键字结束
     * 
     * @param index
     * @param keywords
     * @return
     */
    public static Branch branch(int index, String keywords)
    {
        String[] parts = Util.removeBlankElement(keywords.split("\\[|;|]"));
        Float[] branchRatios = new Float[]
        { Float.parseFloat(parts[1]), Float.parseFloat(parts[2]) };
        int branchNum = parts.length - 1;
        int branchBegin = index;
        List<ArrayList<SQL>> branchBlockList = new ArrayList<>();
        while (!getKeyword(index).equals("end_branch"))
        {
            for (int i = 0; i < branchNum; i++)
            {
                ArrayList<SQL> sqlList = new ArrayList<>();
                do
                {
                    index++;
                    sqlList.add(function(index, getKeyword(index)));
                } while (!(getKeyword(index + 1).equals("branch_delimiter")
                        || getKeyword(index + 1).equals("end_branch")));
                index++;// 跳过delimiter_branch或end_branch
                branchBlockList.add(sqlList);
                sqlList = new ArrayList<>();
            }
        }
        indexSkipLength = index - branchBegin;
        Branch branch = new Branch(branchRatios, branchBlockList);
        return branch;
    }

    /**
     * select函数将select关键字封装成select类
     * 
     * @param keywords
     * @return
     */
    public static Select select(String keywords)
    {
        List<Column> filterColumnList = new ArrayList<>();
        List<String> relationalOperator = new ArrayList<>();
        List<String> logicalOperator = new ArrayList<>();
        boolean is_prepared = false;
        List<String> selectedColumnList = new ArrayList<>();
        String[] parts = Util.removeBlankElement(keywords.split("\\[|;|]"));
        if (keywords.indexOf("filter") != -1)// 包含filter
        {
            // 处理filter条件
            String[] filter = parts[5].split("\\(|,|\\)");
            for (int i = 1; i < filter.length; i++)
            {
                if (i % 2 == 1)// 处理类似“c1 =”的情况
                {
                    String[] col_op = filter[i].split(" ");
                    // Map map=tableColMap.get(parts[1]);
                    // Column column=(Column)map.get("c1");
                    filterColumnList.add((Column) tableColMap.get(parts[1]).get(col_op[0]));
                    relationalOperator.add(col_op[1]);
                }
                if (i % 2 == 0)// 处理类似逻辑运算符的情况
                {
                    if (filter[i].equals("&"))
                    {
                        logicalOperator.add("and");
                    }
                    if (filter[i].equals("|"))
                    {
                        logicalOperator.add("OR");
                    }
                }
            }
        }
        Filter filter = new Filter(filterColumnList, relationalOperator, logicalOperator);
        Table table = tableMap.get(parts[1]);
        is_prepared = Boolean.parseBoolean(parts[2]);
        Distribution distribution = distribution(parts[3]);
        String[] selectCon = parts[4].substring(1, parts[4].length() - 1).split(",");
        for (int i = 0; i < selectCon.length; i++)
        {
            if (selectCon[i].equals("*"))
            {
                selectedColumnList.add("*");
                break;
            }
            selectedColumnList.add(selectCon[i]);
        }

        String append = null;
        if (keywords.indexOf("append") != -1)
        {
            String[] temp = parts[parts.length - 1].split("\"|'", -1);
            if(temp.length > 1)
            {
                append = temp[1].toString();
            }
            else
            {
                append = temp[0].toString();
            }
        }
        Select select = new Select(table, is_prepared, distribution, selectedColumnList, filter,
                append);
        return select;
    }

    /**
     * SelectForUpdate函数将SelectForUpdate关键字封装成SelectForUpdate类
     * 
     * @param keywords
     * @return
     */
    public static SelectForUpdate SelectForUpdate(String keywords)
    {
        List<Column> filterColumnList = new ArrayList<>();
        List<String> relationalOperator = new ArrayList<>();
        List<String> logicalOperator = new ArrayList<>();
        boolean is_prepared = false;
        List<String> selectedColumnList = new ArrayList<>();
        String[] parts = Util.removeBlankElement(keywords.split("\\[|;|]"));
        if (keywords.indexOf("filter") != -1)// 包含filter
        {
            // 处理filter条件
            String[] filter = parts[5].split("\\(|,|\\)");
            for (int i = 1; i < filter.length; i++)
            {
                if (i % 2 == 1)// 处理类似“c1 =”的情况
                {
                    String[] col_op = filter[i].split(" ");
                    // Map map=tableColMap.get(parts[1]);
                    // Column column=(Column)map.get("c1");
                    filterColumnList.add((Column) tableColMap.get(parts[1]).get(col_op[0]));
                    relationalOperator.add(col_op[1]);
                }
                if (i % 2 == 0)// 处理类似逻辑运算符的情况
                {
                    if (filter[i].equals("&"))
                    {
                        logicalOperator.add("and");
                    }
                    if (filter[i].equals("|"))
                    {
                        logicalOperator.add("OR");
                    }
                }
            }
        }
        Filter filter = new Filter(filterColumnList, relationalOperator, logicalOperator);
        Table table = tableMap.get(parts[1]);
        is_prepared = Boolean.parseBoolean(parts[2]);
        Distribution distribution = distribution(parts[3]);
        String[] selectCon = parts[4].substring(1, parts[4].length() - 1).split(",");
        for (int i = 0; i < selectCon.length; i++)
        {
            if (selectCon[i].equals("*"))
            {
                selectedColumnList.add("*");
                break;
            }
            selectedColumnList.add(selectCon[i]);
        }

        String append = null;
        if (keywords.indexOf("append") != -1)
        {
            String[] temp = parts[parts.length - 1].split("\"|'", -1);
            if(temp.length > 1)
            {
                append = temp[1].toString();
            }
            else
            {
                append = temp[0].toString();
            }
        }
        SelectForUpdate selectForUpdate = new SelectForUpdate(table, is_prepared, distribution, selectedColumnList, filter,append);
        return selectForUpdate;
    }
    
    /**
     * insert函数将insert关键字封装成insert类
     * 
     * @param keywords
     * @return
     */
    public static Insert insert(String keywords)
    {
        boolean is_prepared = false;
        String[] parts = Util.removeBlankElement(keywords.split("\\[|;|]"));

        Table table = tableMap.get(parts[1]);
        is_prepared = Boolean.parseBoolean(parts[2]);
        Distribution distribution = distribution(parts[3]);
        Insert insert = new Insert(table, is_prepared, distribution);
        return insert;
    }

    /**
     * replace函数将replace关键字封装成replace类
     * 
     * @param keywords
     * @return
     */
    public static Replace replace(String keywords)
    {
        boolean is_prepared = false;
        String[] parts = Util.removeBlankElement(keywords.split("\\[|;|]"));

        Table table = tableMap.get(parts[1]);
        is_prepared = Boolean.parseBoolean(parts[2]);
        Distribution distribution = distribution(parts[3]);
        Replace replace = new Replace(table, is_prepared, distribution);
        return replace;
    }

    /**
     * delete函数将delete关键字封装成delete类
     * 
     * @param keywords
     * @return
     */
    public static Delete delete(String keywords)
    {
        List<Column> filterColumnList = new ArrayList<>();
        List<String> relationalOperator = new ArrayList<>();
        List<String> logicalOperator = new ArrayList<>();
        boolean is_prepared = false;
        String[] parts = Util.removeBlankElement(keywords.split("\\[|;|]"));
        // if(parts.length == 5)//包含filter
        if (keywords.indexOf("filter") != -1)// 包含filter
        {
            // 处理filter条件
            String[] filter = parts[4].split("\\(|,|\\)");
            for (int i = 1; i < filter.length; i++)
            {
                if (i % 2 == 1)// 处理类似“c1 =”的情况
                {
                    String[] col_op = filter[i].split(" ");
                    filterColumnList.add((Column) tableColMap.get(parts[1]).get(col_op[0]));
                    relationalOperator.add(col_op[1]);
                }
                if (i % 2 == 0)// 处理类似逻辑运算符的情况
                {
                    if (filter[i].equals("&"))
                    {
                        logicalOperator.add("and");
                    }
                    if (filter[i].equals("|"))
                    {
                        logicalOperator.add("OR");
                    }
                }
            }
        }
        Table table = tableMap.get(parts[1]);
        Distribution distribution = distribution(parts[3]);
        Filter filter = new Filter(filterColumnList, relationalOperator, logicalOperator);
        is_prepared = Boolean.parseBoolean(parts[2]);
        Delete delete = new Delete(table, is_prepared, distribution, filter);
        return delete;
    }

    /**
     * update函数将update关键字封装成update类
     * 
     * @param keywords
     * @return
     */
    public static Update update(String keywords)
    {
        List<Column> filterColumnList = new ArrayList<>();
        List<String> relationalOperator = new ArrayList<>();
        List<String> logicalOperator = new ArrayList<>();
        boolean is_prepared = false;
        String[] parts = Util.removeBlankElement(keywords.split("\\[|;|]"));
        // if(parts.length == 5)//包含filter
        if (keywords.indexOf("filter") != -1)// 包含filter
        {
            // 处理filter条件
            String[] filter = parts[parts.length - 1].split("\\(|,|\\)");
            for (int i = 1; i < filter.length; i++)
            {
                if (i % 2 == 1)// 处理类似“c1 =”的情况
                {
                    String[] col_op = filter[i].split(" ");
                    filterColumnList.add((Column) tableColMap.get(parts[1]).get(col_op[0]));
                    relationalOperator.add(col_op[1]);
                }
                if (i % 2 == 0)// 处理类似逻辑运算符的情况
                {
                    if (filter[i].equals("&"))
                    {
                        logicalOperator.add("and");
                    }
                    if (filter[i].equals("|"))
                    {
                        logicalOperator.add("OR");
                    }
                }
            }
        }
        Table table = tableMap.get(parts[1]);
        Distribution distribution = distribution(parts[3]);
        Filter filter = new Filter(filterColumnList, relationalOperator, logicalOperator);
        is_prepared = Boolean.parseBoolean(parts[2]);

        String[] updateCondition = parts[4].split(",");
        List<Column> updatedColumnList = new ArrayList<>();
        List<String> updateOperatorList = new ArrayList<>();
        List<String> updatedValue = new ArrayList<>();
        for (int i = 0; i < updateCondition.length; i++)
        {
            String[] upd = updateCondition[i].split(" ");
            if (upd.length == 1)
            {
                updatedColumnList.add((Column) tableColMap.get(parts[1]).get(upd[0]));
                updateOperatorList.add("=");
                updatedValue.add("");
            }
            if (upd.length == 2)
            {
                updatedColumnList.add((Column) tableColMap.get(parts[1]).get(upd[0]));
                updateOperatorList.add(upd[1]);
                updatedValue.add("");
            }
            if (upd.length == 3)
            {
                updatedColumnList.add((Column) tableColMap.get(parts[1]).get(upd[0]));
                updateOperatorList.add(upd[1]);
                String[] temp = upd[2].split("\"|'", -1);
                if (temp.length > 1)
                    updatedValue.add(temp[1]);
                else
                    updatedValue.add(temp[0]);
            }
        }
        Update update = new Update(table, is_prepared, distribution, updatedColumnList,
                updateOperatorList, updatedValue, filter);
        return update;
    }

    /**
     * 对表中的某列设置数据特征
     * @param keywords
     */
    public static void column(String keywords)
    {
        String[] parts = Util.removeBlankElement(keywords.split("\\[|;|]"));
        Column col = tableColMap.get(parts[1]).get(parts[2]);
        col.setNullRatio(Float.parseFloat(parts[3]));
        col.setCardinality(Integer.parseInt(parts[4]));
        if(col.getDataType().equals("char")||col.getDataType().equals("varchar"))
        {
            col.setMinLength(Integer.parseInt(parts[5]));
            col.setMaxLength(Integer.parseInt(parts[6]));
        }
        else 
        {
            col.setMinValue(Double.parseDouble(parts[5]));
            col.setMaxValue(Double.parseDouble(parts[6]));
        }
        
//        List<Column> columnList1 = new ArrayList<>();
//        columnList1 = tableMap.get(parts[1]).getColumnList();
//        for(int i = 0; i < columnList1.size(); i++)
//            if(columnList1.get(i).equals(col))
//                System.out.println(columnList1.get(i).getMaxValue());
       
    }

    /**
     * 根据表模式，建立表并插入数据
     * 
     * @param keywords
     */
    public static void import_tbl(String keywords)
    {
        String[] parts = Util.removeBlankElement(keywords.split("\\[|;|]"));
        for (int i = 1; i <= parts.length - 1; i++)
        {
            importTableList.add(tableMap.get(parts[i]));
        }
    }

    /**
     * 根据表模式，清空表
     * 
     * @param keywords
     */
    public static void clear_tbl(String keywords)
    {
        String[] parts = Util.removeBlankElement(keywords.split("\\[|;|]"));
        for (int i = 1; i <= parts.length - 1; i++)
        {
            clearTableList.add(tableMap.get(parts[i]));
        }
        Dispatcher.clearTables(clearTableList);
    }

    /**
     * 开始执行压力测试，调用压力测试主程序
     * 
     * @param keywords
     */
    public static void txnLoading(String keywords)
    {
        String[] parts = Util.removeBlankElement(keywords.split("\\[|;|]"));
        int threadNumber = Integer.parseInt(parts[1]);
        int threadRunTimes = Integer.parseInt(parts[2]);
        int loadMachineNumber = Integer.parseInt(parts[3]);
        if (txn_flag)
        {
            Workload workload = new Workload(transactionList, threadNumber, threadRunTimes,
                    loadMachineNumber);
            workloadList.add(workload);
        }
        txn_flag = false;
        // Dispatcher dispatcher = new Dispatcher(tableList, workloadList);
        // dispatcher.execute();
        // Dispatcher.execute(tableList, workloadList);
        Dispatcher.execute(importTableList, workloadList);
    }

    /**
     * 处理数据的分布
     * 
     * @param dis
     * @return
     */
    public static Distribution distribution(String dis)
    {
        Distribution distribution = null;
        String[] distri = dis.split("\\(|,|\\)");
        switch (distri[0])
        {
        case "unique":
            distribution = new UniqueDistribution(Integer.parseInt(distri[1]),
                    Integer.parseInt(distri[2]));
            break;
        case "uniform":
            distribution = new UniformDistribution(Long.parseLong(distri[1]),
                    Long.parseLong(distri[2]));
            break;
        // case "poisson":
        // distribution = new PoissonDistribution(Integer.parseInt(distri[1]),
        // Integer.parseInt(distri[2]));
        // break;
        case "normal":
            distribution = new NormalDistribution(Integer.parseInt(distri[1]),
                    Integer.parseInt(distri[2]), Integer.parseInt(distri[3]));
            break;
        case "zipfian":
            distribution = new ZIPFDistribution(Integer.parseInt(distri[1]),
                    Integer.parseInt(distri[2]), Integer.parseInt(distri[3]),
                    Integer.parseInt(distri[4]));
            break;
        default:
            break;
        }
        return distribution;
    }
}
