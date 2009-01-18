package org.csstudio.dct.nameresolution;

import org.csstudio.dct.model.IRecord;

/**
 * Represents function that can be used in a record field. Functions are
 * identified by their name and can take an arbitrary number of parameters. The
 * final task of a function is to deliver a String value for the record field.
 * 
 * From a design point of view field functions are a typical strategy pattern.
 * 
 * @author Sven Wende
 * 
 */
public interface IFieldFunction {
	/**
	 * Evaluates the function to a String.
	 * 
	 * @param name
	 *            the function name
	 * @param parameters
	 *            the parameters
	 * @param record
	 *            the record which contains the field with this function
	 * @param fieldName
	 *            the name of the field that contains this function
	 *            
	 * @return the final String 
	 */
	String evaluate(String name, String[] parameters, IRecord record, String fieldName) throws Exception;
}
