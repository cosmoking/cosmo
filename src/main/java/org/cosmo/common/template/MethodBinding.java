package org.cosmo.common.template;

import java.lang.reflect.Method;

public class MethodBinding extends Binding.ValueBinding implements Binding.AllowOverride
{
	String _arg;
	Method _method;
	boolean _hasContextArg;

	public MethodBinding (String methodName)
	{
		super(methodName);
		_name = _name.substring(0, _name.indexOf("("));
	}

	private void initMethods (BindingSrc bindingSrc, Object context)
	  throws Exception
	{
		// lazy init - move this to constructor until context class is available at page parsing time
		if (_method == null) {
			Class contextClass = context == null ? Void.class  : context.getClass();
			try {
				_method = bindingSrc.getClass().getMethod(_name, contextClass, Content.class);
				_hasContextArg = true;
			}
			catch (NoSuchMethodException e1) {
				try {
					_method = bindingSrc.getClass().getMethod(_name, Object.class, Content.class);
					_hasContextArg = true;
				}
				catch (NoSuchMethodException e2) {
					try {
						_method = bindingSrc.getClass().getMethod(_name, Content.class);
						_hasContextArg = false;
					}
					catch (NoSuchMethodException e3) {
						NoSuchMethodException e = new NoSuchMethodException(e1.getMessage() + " AND " + e2.getMessage() + " AND " + e3.getMessage());
						e.fillInStackTrace();
						throw e;
					}
				}
			}
		}
	}

	@Override
	public Object applyValue (Page page, BindingSrc bindingSrc, Content container, Object context, Options options)
	  throws Exception
	{
		initMethods(bindingSrc, context);
		if (container == null) {
			return _hasContextArg
				? _method.invoke(bindingSrc, context, (Object)null)
				: _method.invoke(bindingSrc, (Object)null);
		}
		else {
			Object result = _hasContextArg
				? _method.invoke(bindingSrc, context, container)
				: _method.invoke(bindingSrc, container);
			container.pushBinding(result == null ? "" : result);
			return result;
		}
	}

	public Object getValue (Page page, BindingSrc bindingSrc, Object context, Options options) throws Exception
	{
		throw new IllegalArgumentException("Not Supported");
	}

	public String name ()
	{
		return _name;
	}

}