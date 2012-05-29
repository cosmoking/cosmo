package org.cosmo.common.template;

public class ConstantBinding extends Binding.ValueBinding
{
	Object _constant;

	public ConstantBinding (String constantStr)
	{
		super(constantStr);
		if (Character.isDigit(_name.charAt(0))) {
			_constant = Integer.parseInt(_name);
		}
		else {
			if ("null".equals(_name)) {
				_constant = null;
			}
			else {
				_constant = Boolean.parseBoolean(_name) ? Boolean.TRUE : Boolean.FALSE;
			}

		}
	}

	public Object getValue (Page page, BindingSrc bindingSrc, Object context, Options options)
	  throws Exception
	{
		return _constant;
	}

	public String name ()
	{
		return _name;
	}

}