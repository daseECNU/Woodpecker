#!/bin/bash

JAR_PATH=lib
BIN_PATH=bin
SRC_PATH=src
TESTCASE_PATH=test_case
IDEALRESULTSET_PATH=ideal_result_set
DBINSTANCE_PATH=database_instance
CONFIG_PATH=config
MIDDLERESULT_PATH=middle_result


echo "build classpath"  
# java文件列表目录  
SRC_FILE_LIST_PATH=src/sources.list  
  
#生成所有的java文件列表  
rm -f $SRC_PATH/sources.list
find $SRC_PATH/ -name *.java > $SRC_FILE_LIST_PATH
  
#删除旧的编译文件 生成bin目录  
rm -rf $BIN_PATH/
mkdir $BIN_PATH/

#生成依赖jar包列表  
for file in  ${JAR_PATH}/*.jar;  
do
jarfile=${jarfile}:${file}
done
#echo "jarfile = "$jarfile

# #生成案例文件列表
# for file in  ${TESTCASE_PATH}/*.*;
# do
# casefile=${casefile}:${file}
# done

# #生成理想结果集文件列表
# for file in ${IDEALRESULTSET_PATH}/*.*;
# do
# irsfile=${irsfile}:${file}
# done

# #生成数据库实例文件列表
# for file in ${DBINSTANCE_PATH}/*.*;
# do
# dbifile=${dbifile}:${file}
# done

# #生成配置文件列表
# for file in ${CONFIG_PATH}/*.*;
# do
# configfile=${configfile}:${file}
# done

# #生成中间结果集路径
# for file in ${MIDDLERESULT_PATH}/*.*;
# do
# midresult=${midresult}:${file}
# done


#编译
echo "compile Woodpecker"
javac -d $BIN_PATH/ -cp $jarfile @$SRC_FILE_LIST_PATH
#echo $jarfile$casefile$irsfile$dbifile$configfile
  
#运行
echo "run Woodpecker"
java -cp $BIN_PATH$jarfile edu.ecnu.woodpecker.controller.TestController
#echo $BIN_PATH$jarfile$casefile$irsfile$dbifile$configfile
