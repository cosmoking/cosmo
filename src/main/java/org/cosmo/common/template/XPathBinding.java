package org.cosmo.common.template;

import ariba.util.fieldvalue.FieldValue;

public class XPathBinding extends Binding.ValueBinding implements Binding.AllowOverride
{

	public XPathBinding (String xpath)
	{
		super(xpath);
	}

    public Object getValue (Page page, BindingSrc bindingSrc, Object context, Options options)
      throws Exception
    {
    	return FieldValue.getFieldValue(bindingSrc, _name);
    }


	public String name ()
	{
		return _name;
	}
}