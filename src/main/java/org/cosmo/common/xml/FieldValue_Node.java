/*******************************************************************************
 * Copyright 2012 Jack Wang
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.cosmo.common.xml;

import ariba.util.core.Fmt;
import ariba.util.fieldvalue.*;

public class FieldValue_Node extends FieldValue_Object
{

    static {
        FieldValue.registerClassExtension(XML.class,
                new FieldValue_Node());

    	FieldValue.registerClassExtension(Node.class,
                               new FieldValue_Node());

        FieldValue.registerClassExtension(AttributeNode.class,
                			   new FieldValue_Node());

    }


    	// Straight copy from FieldValue - except reserve '#' as a prefix
        // to access FieldValue members of Node
    public void setFieldValuePrimitive (Object target, FieldPath fieldPath,
                                        Object value)
    {
    	if (fieldPath._fieldName.charAt(0) == '#') {
            FieldValueSetter setter = fieldPath._previousSetter;
            boolean isAccessorApplicable = (target.getClass() == setter.forClass())
                && setter.isApplicable(target);
            if (!isAccessorApplicable) {

            	String fieldName = fieldPath._fieldName.substring(1, fieldPath._fieldName.length());

                setter = (FieldValueSetter)getAccessor(target, fieldName, Setter);
                if (setter == null) {
                    String message = Fmt.S(
                        "Unable to locate setter method or " +
                        "field for: \"%s\" on target class: \"%s\"",
                        fieldPath._fieldName, target.getClass().getName());
                    throw new FieldValueException(message);
                }
                fieldPath._previousSetter = setter;
            }
            setter.setValue(target, value);
    	}
    	else {
    		((Node)target).set(fieldPath._fieldName, value);
    	}
    }


    public Object getFieldValuePrimitive (Object target, FieldPath fieldPath)
    {
    	if (fieldPath._fieldName.charAt(0) == '#') {
            FieldValueGetter getter = fieldPath._previousGetter;
            boolean isAccessorApplicable = (target.getClass() == getter.forClass())
                && getter.isApplicable(target);
            if (!isAccessorApplicable) {

               	String fieldName = fieldPath._fieldName.substring(1, fieldPath._fieldName.length());

                getter = (FieldValueGetter)getAccessor(target, fieldName, Getter);
                if (getter == null) {
                    String message = Fmt.S(
                        "Unable to locate getter method or " +
                        "field for: \"%s\" on target class: \"%s\"",
                        fieldPath._fieldName, target.getClass().getName());
                    throw new FieldValueException(message);
                }
                fieldPath._previousGetter = getter;
            }
            return getter.getValue(target);
    	}
    	else {
    		return ((Node)target).get(fieldPath._fieldName);
    	}
    }
}
