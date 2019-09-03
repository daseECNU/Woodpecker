package edu.ecnu.woodpecker.sql;

import edu.ecnu.woodpecker.constant.SQLConstant;

public enum TransactionOperator
{
    START(SQLConstant.START),COMMIT(SQLConstant.COMMIT),ROLLBACK(SQLConstant.ROLLBACK);
    
    private String operator = null;
    
    private TransactionOperator(String operator)
    {
        this.operator = operator;
    }
    
    public String getOperator()
    {
        return operator;
    }
    
    public String toString()
    {
        return operator;
    }
}
