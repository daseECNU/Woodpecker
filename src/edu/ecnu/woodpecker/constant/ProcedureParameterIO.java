package edu.ecnu.woodpecker.constant;

import java.util.NoSuchElementException;
import java.util.stream.Stream;

/**
 * The IO type of parameter in store procedure
 *
 */
public enum ProcedureParameterIO
{
    IN(SQLConstant.IN), OUT(SQLConstant.OUT), IN_OUT(SQLConstant.IN_OUT);
    
    private String lowerDesc = null;
    
    private ProcedureParameterIO(String lowerDesc)
    {
        this.lowerDesc = lowerDesc;
    }
    
    public String getLowerDesc()
    {
        return lowerDesc;
    }
    
    /**
     * Returns an enum constant of the specified value
     * 
     * @param lowerDesc Include "in", "out", "inout"
     * @return
     * @throws NoSuchElementException
     */
    public static ProcedureParameterIO of(String lowerDesc) throws NoSuchElementException
    {
        Stream<ProcedureParameterIO> stream = Stream.of(ProcedureParameterIO.class.getEnumConstants());
        return stream.filter(ele -> ele.getLowerDesc().equals(lowerDesc)).findAny().get();
    }
}
