package edu.ecnu.woodpecker.constant;

/**
 * Woodpecker config file constant
 */
public final class ConfigConstant
{
    // Woodpecker.conf config item
    public static final String RETRY_COUNT = "retry_count";
    public static final String DEFAULT_OR_NOT = "default_or_not";
    public static final String CASE_GROUP_NAME = "case_group_name";
    // Value of config item
    public static final String UNIFORM = "uniform";
    public static final String NORMAL = "normal";
    // Database Manager System
    public static final String MYSQL = "mysql";
    public static final String CEDAR = "cedar";
    public static final String CEDAR_DATASOURCE = "cedar_data_source";
    public static final String VOLTDB = "voltdb";
    public static final String POSTGRESQL = "postgresql";
    public final static String SQL_SERVER = "sqlserver";
    // Cluster role
    public static final String MASTER_LOWER = "master";
    public static final String MASTER_UPPER = "MASTER";
    public static final String SLAVE_LOWER = "slave";
    public static final String SLAVE_UPPER = "SLAVE";
    // CEDAR_compile.conf config item
    public static final String DATABASE_USER = "database_user";
    public static final String DATABASE_PASSWORD = "database_password";
    public static final String SRC_PATH = "src_path";
    public static final String MAKE_PATH = "make_path";
    public static final String CORE = "core";
    public static final String COMPILE_TOOLS = "compile_tools";
    public static final String CONNECTION_PORT = "connection_port";
    // Cluster config file config item
    public static final String DEPLOY_PATH = "deploy_path";
    public static final String CEDAR_PORT = "cedar_port";
    public static final String LOG_PATH = "log_path";
    // default database username and password
    public static final String ADMIN_NAME = "admin";
    public static final String ADMIN_PASSWORD = "admin";
    public static final String ROOT_NAME = "root";
    public static final String ROOT_PASSWORD = "root";
}
