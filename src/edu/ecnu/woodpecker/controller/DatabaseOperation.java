package edu.ecnu.woodpecker.controller;

import org.apache.commons.cli.CommandLine;

public interface DatabaseOperation
{
    public void initialize(String configFilePath);
    
    public void enter(CommandLine line);
}
