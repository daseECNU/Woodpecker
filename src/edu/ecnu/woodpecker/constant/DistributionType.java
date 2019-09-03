package edu.ecnu.woodpecker.constant;

import java.util.NoSuchElementException;
import java.util.stream.Stream;

public enum DistributionType {
	UNIQUE("UNIQUE"), UNIFORM("UNIFORM"), NORMAL("NORMAL"), ZIPF("ZIPF");
	public String value = null;
	
	private DistributionType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
	
	 /**
     * Returns an enum constant of the specified value
     * 
     * @param value
     * @return
     * @throws NoSuchElementException
     */
    public static DistributionType of(String value) throws NoSuchElementException
    {
        Stream<DistributionType> stream = Stream.of(DistributionType.class.getEnumConstants());
        return stream.filter(ele -> ele.getValue().equals(value)).findAny().get();
    }
}
