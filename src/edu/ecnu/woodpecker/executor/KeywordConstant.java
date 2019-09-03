package edu.ecnu.woodpecker.executor;

import java.util.HashMap;
import java.util.Map;

import edu.ecnu.woodpecker.constant.SignConstant;

/**
 * The class contains keyword constant and the map between keyword and handle class name
 * Attention: when add new keyword, must add here
 */
public class KeywordConstant
{
    // Keyword constant
    public static final String GET_CONNECTION = "get_conn";
    public static final String GET_STATEMENT = "get_stat";
    public static final String GET_PREPARED_STATEMENT = "get_pstat";
    public static final String GET_CALLABLE_STATEMENT = "get_cstat";
    public static final String SQL = "sql";
    public static final String PSQL = "psql";
    public static final String CSQL = "csql";
    public static final String TRANSACTION = "tx";
    public static final String VERIFY = "verify";
    public static final String SLEEP = "sleep";
    public static final String STRESS_TEST = "st";
    public static final String IMPORT_DBI = "import_dbi";
    public static final String CLEAR_DBI = "clear_dbi";
    public static final String IMPORT_IRS = "import_irs";
    public static final String SYSTEM_TEST = "sys";
    public static final String DEFINE_PROCEDURE = "def_proc";
    public static final String CONTAIN = "contain";
    public static final String ERROR = "error";
    public static final String CALCULATE = "CAL"; // upper case in .mr file
    public static final String INDEX_OF = "index_of";
    public static final String GENERATE_RANDOM_DATA = "grd";
    public static final String CPU_EXCEPTION = "cpu";
    public static final String MEMORY_EXCEPTION = "mem";
    public static final String DISK_EXCEPTION = "disk";
    public static final String NETWORK_EXCEPTION = "net";
    public static final String PARALL = "parall";

    // All keyword class must locate in this directory
    public static final String CLASS_PREFIX = "edu.ecnu.woodpecker.executor.keyword";

    /**
     * map key is keyword, value is corresponding class name
     */
    public static Map<String, String> keywordClassMap = new HashMap<>(30);

    // manual add when you need to add new keyword
    static
    {
        keywordClassMap.put(GET_CONNECTION, CLASS_PREFIX + SignConstant.DOT_CHAR + "ConnectionGetter");
        keywordClassMap.put(GET_STATEMENT, CLASS_PREFIX + SignConstant.DOT_CHAR + "StatementGetter");
        keywordClassMap.put(GET_PREPARED_STATEMENT, CLASS_PREFIX + SignConstant.DOT_CHAR + "PreparedStatementGetter");
        keywordClassMap.put(GET_CALLABLE_STATEMENT, CLASS_PREFIX + SignConstant.DOT_CHAR + "CallableStatementGetter");

        keywordClassMap.put(SQL, CLASS_PREFIX + SignConstant.DOT_CHAR + "SQLProcessor");
        keywordClassMap.put(PSQL, CLASS_PREFIX + SignConstant.DOT_CHAR + "PreparedSQLProcessor");
        keywordClassMap.put(CSQL, CLASS_PREFIX + SignConstant.DOT_CHAR + "CallableSQLProcessor");

        keywordClassMap.put(TRANSACTION, CLASS_PREFIX + SignConstant.DOT_CHAR + "TransactionProcessor");
        keywordClassMap.put(VERIFY, CLASS_PREFIX + SignConstant.DOT_CHAR + "VerifyProcessor");
        keywordClassMap.put(SLEEP, CLASS_PREFIX + SignConstant.DOT_CHAR + "SleepProcessor");
        keywordClassMap.put(ERROR, CLASS_PREFIX + SignConstant.DOT_CHAR + "ErrorProcessor");
        keywordClassMap.put(STRESS_TEST, CLASS_PREFIX + SignConstant.DOT_CHAR + "StressTestProcessor");

        keywordClassMap.put(IMPORT_DBI, CLASS_PREFIX + SignConstant.DOT_CHAR + "DBIImporter");
        keywordClassMap.put(CLEAR_DBI, CLASS_PREFIX + SignConstant.DOT_CHAR + "DBICleaner");
        keywordClassMap.put(IMPORT_IRS, CLASS_PREFIX + SignConstant.DOT_CHAR + "IdealResultSetImporter");
        keywordClassMap.put(SYSTEM_TEST, CLASS_PREFIX + SignConstant.DOT_CHAR + "SystemTestProcessor");

        keywordClassMap.put(DEFINE_PROCEDURE, CLASS_PREFIX + SignConstant.DOT_CHAR + "ProcedureDefiner");
        keywordClassMap.put(CONTAIN, CLASS_PREFIX + SignConstant.DOT_CHAR + "ContainProcessor");
        keywordClassMap.put(CALCULATE, CLASS_PREFIX + SignConstant.DOT_CHAR + "CalculateProcessor");
        keywordClassMap.put(INDEX_OF, CLASS_PREFIX + SignConstant.DOT_CHAR + "IndexOfProcessor");

        keywordClassMap.put(GENERATE_RANDOM_DATA, CLASS_PREFIX + SignConstant.DOT_CHAR + "RandomDataGenerator");
        
        keywordClassMap.put(CPU_EXCEPTION, CLASS_PREFIX + SignConstant.DOT_CHAR + "CPUExceptionProcessor");
        keywordClassMap.put(MEMORY_EXCEPTION, CLASS_PREFIX + SignConstant.DOT_CHAR + "MemoryExceptionProcessor");
        keywordClassMap.put(DISK_EXCEPTION, CLASS_PREFIX + SignConstant.DOT_CHAR + "DiskExceptionProcessor");
        keywordClassMap.put(NETWORK_EXCEPTION, CLASS_PREFIX + SignConstant.DOT_CHAR + "NetworkExceptionProcessor");
        
        keywordClassMap.put(PARALL, CLASS_PREFIX + SignConstant.DOT_CHAR + "ParallelProcessor");
    }
}
