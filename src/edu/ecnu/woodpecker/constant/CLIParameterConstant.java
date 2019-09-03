package edu.ecnu.woodpecker.constant;

/**
 * 命令行参数项的常量类
 * 
 */
public final class CLIParameterConstant
{
    public final static String HELP = "help";
    public final static String HELP_DESC = "print Woodpecker usage and stop running";
    public final static String USAGE = "java -jar Woodpecker.jar [ options ]";
    
    public final static String EDIT_MODE = "edit_mode";
    public final static String EDIT_MODE_DESC = "CEDAR: never compile, deploy and stop CEDAR except restart, its priority is great than -compile_cedar and -deploy";
   
    public final static String COMPILE_CEDAR = "compile_cedar";
    public final static String COMPILE_CEDAR_DESC = "CEDAR: compile CEDAR before testing";
    public final static String COMPILE_CEDAR_ONLY = "compile_cedar_only";
    public final static String COMPILE_CEDAR_ONLY_DESC = "CEDAR: compile CEDAR, then stop Woodpecker if not exists -deploy_only";
    
    public final static String CLOSE_MYSQL = "close_mysql";
    public final static String CLOSE_MYSQL_DESC = "MySQL: close MySQL after testing or failure";
    
    public final static String DEPLOY = "deploy";
    public final static String DEPLOY_DESC = "deploy DBMS before testing";
    public final static String DEPLOY_ONLY = "deploy_only";
    public final static String DEPLOY_ONLY_DESC = "CEDAR: deploy CEDAR, then stop Woodpecker";
    
    public final static String DISABLE_RESTART = "disable_restart";
    public final static String DISABLE_RESTART_DESC = "CEDAR: disable automatic restart when case fail";
    
    // 小工具命令
    public final static String MERGE = "merge";
    public final static String MERGE_DESC = "CEDAR: use merge command in CEDAR";
    
    public final static String KILL_CEDAR = "kill_cedar";
    public final static String KILL_CEDAR_DESC = "CEDAR: kill all CEDAR servers";
    
    public final static String REELECT = "reelect";
    public final static String REELECT_DESC = "CEDAR: reelect master in CEDAR";
    
    public final static String GET_OBI_ROLE = "get_obi_role";
    public final static String GET_OBI_ROLE_DESC = "CEDAR: get RootServer IP of master";
    
    public final static String SYNTAX_CHECK = "syntax_check";
    public final static String SYNTAX_CHECK_DESC = "check syntax of case file";
}
