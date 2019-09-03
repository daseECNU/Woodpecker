package edu.ecnu.woodpecker.constant;

public class ShellConstant
{
    public final static String SCP = "scp -r user@ip:path .; ";
    public final static String DELETE = "rm -rf path ";
    public final static String MKDIR = "mkdir -p dirName ;";
    public final static String OPENDIR = "cd dirName ";
    public final static String EXECRESULT = " && echo exec_successful ||echo exec_unsuccessful ;";
    public final static String LS = "ls ;";
    public final static String FORDISK = "for disk in {1..8}; do ";
    public final static String LN = "ln -s path ;";
    public final static String DONE = "done";

}
