package org.cosmo.common.template;

import java.lang.reflect.Field;

import org.cosmo.common.util.New;

public class FieldBinding extends Binding.ValueBinding implements Binding.AllowOverride
{
	Field _field;

	public FieldBinding (String fieldName, Class bindingSrcClass)
	  throws Exception
	{
		super(fieldName);
		try {
			_field = bindingSrcClass.getField(_name.toString());
		}
		catch (Exception e) {
			Page page = Page.byName(bindingSrcClass.getName());
			throw new IllegalArgumentException(New.str("Binding [", _name, "] not exist for page [", page._name, "] bindingSrc [", bindingSrcClass, "]"));
		}
	}

	public Object getValue (Page page, BindingSrc bindingSrc, Object context, Options options)
	  throws Exception
	{
		return _field.get(bindingSrc);
	}

	public String name ()
	{
		return _name;
	}
}