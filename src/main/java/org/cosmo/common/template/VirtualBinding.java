package org.cosmo.common.template;

public class VirtualBinding extends Binding.ValueBinding implements Binding.AllowOverride
{
	public VirtualBinding (String fieldName)
	  throws Exception
	{
		super(fieldName);
		_name =  _name.substring(1, _name.length());
	}

	public Object getValue (Page page, BindingSrc bindingSrc, Object context, Options options)
	  throws Exception
	{
			// this means caller did not pass the arg - hence return empty string;
		return "";
	}

	public String name ()
	{
		return _name;
	}
}