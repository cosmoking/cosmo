package org.cosmo.common.template;

public class StringBinding extends Binding.ValueBinding
{

	public StringBinding (String stringBinding)
	{
		super(stringBinding);
		_name = _name.substring(1, _name.length() -1 );;
	}

	public Object getValue (Page page, BindingSrc bindingSrc, Object context, Options options)
	  throws Exception
	{
		return _name;
	}

	public String name ()
	{
		return _name;
	}

}