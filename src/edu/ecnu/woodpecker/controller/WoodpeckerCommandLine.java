package edu.ecnu.woodpecker.controller;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import edu.ecnu.woodpecker.constant.CLIParameterConstant;
import edu.ecnu.woodpecker.constant.LogLevelConstant;
import edu.ecnu.woodpecker.log.WpLog;

/**
 * Woodpecker command line class
 * 
 */
public class WoodpeckerCommandLine
{
    private static Options options = null;
    private static CommandLineParser parser = null; 
    private static CommandLine commandLine = null;
    
    public static void setOption(String[] args)
    {
        // use Apache Commons CLI to parse parameters
        options = new Options();
        options.addOption(CLIParameterConstant.HELP, CLIParameterConstant.HELP_DESC);
        options.addOption(CLIParameterConstant.EDIT_MODE, CLIParameterConstant.EDIT_MODE_DESC);
        options.addOption(CLIParameterConstant.COMPILE_CEDAR, CLIParameterConstant.COMPILE_CEDAR_DESC);
        options.addOption(CLIParameterConstant.COMPILE_CEDAR_ONLY, CLIParameterConstant.COMPILE_CEDAR_ONLY_DESC);
        options.addOption(CLIParameterConstant.DEPLOY, CLIParameterConstant.DEPLOY_DESC);
        options.addOption(CLIParameterConstant.DEPLOY_ONLY, CLIParameterConstant.DEPLOY_ONLY_DESC);
        options.addOption(CLIParameterConstant.CLOSE_MYSQL, CLIParameterConstant.CLOSE_MYSQL_DESC);
        options.addOption(CLIParameterConstant.DISABLE_RESTART, CLIParameterConstant.DISABLE_RESTART_DESC);
        options.addOption(CLIParameterConstant.MERGE, CLIParameterConstant.MERGE_DESC);
        options.addOption(CLIParameterConstant.KILL_CEDAR, CLIParameterConstant.KILL_CEDAR_DESC);
        options.addOption(CLIParameterConstant.REELECT, CLIParameterConstant.REELECT_DESC);
        options.addOption(CLIParameterConstant.GET_OBI_ROLE, CLIParameterConstant.GET_OBI_ROLE_DESC);
        options.addOption(CLIParameterConstant.SYNTAX_CHECK, true, CLIParameterConstant.SYNTAX_CHECK_DESC);
        // parse parameters
        parser = new DefaultParser();
        try
        {
            commandLine = parser.parse(options, args);
        }
        catch (ParseException e)
        {
            WpLog.recordLog(LogLevelConstant.ERROR, "parsing command line parameters failed, reason: %s", e.getMessage());
            System.exit(1);
        }
    }
    
    public static void printUsage()
    {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(CLIParameterConstant.USAGE, options);
    }
    
    public static CommandLine getCommandLine()
    {
        return commandLine;
    }
}
