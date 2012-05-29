package org.cosmo.common.template;

public class BindBinding extends Binding
{
	public static final BindBinding End = new BindBinding();


		// marks the bound of the case segments
	public int _segmentStartIdx;
	public int _segmentEndIdx;

		// marks the begin of the binding index of the above segments
	public int _bindingStartIdx;
	public int _bindingEndIdx;

		// flag of new case segment
	public boolean _newSegment;


	public BindBinding ()
	{
		_newSegment = true;
	}


		// records each Segment and BindingIndex
	public void recordSegementAndBindingIndex (int segIdx, int bindingIdx)
	{
		if (_newSegment) {
			_bindingStartIdx = bindingIdx;
			_bindingEndIdx = bindingIdx;
			_segmentStartIdx = segIdx;
			_segmentEndIdx = segIdx;
			_newSegment = false;
		}
		else {
			_segmentEndIdx = segIdx;
			_bindingEndIdx = bindingIdx;
		}
	}


	@Override
	public Object applyValue (Page page, BindingSrc bindingSrc, Content container, Object context, Options options)
	  throws Exception
	{
		/*	HACK - when eval by case() we don't want to append any content since we just need the result as a boolean.
		 *  the indication at this level is that if contentContaine is null then it's called by casebinding otherwise
		 *  it's a regular call which content needs to be appended.
		 */
		if (container == null) {
				// when different we know there is content hence return true
			return _segmentStartIdx != _segmentEndIdx;
		}
		else {
			page.append(_segmentStartIdx, _segmentEndIdx, _bindingStartIdx, context, container, bindingSrc);
		}
		return null;
	}


	public Object getValue (Page page, BindingSrc bindingSrc, Object context, Options options) throws Exception
	{
		throw new IllegalArgumentException("Not Supported");
	}


	public String name ()
	{
		throw new IllegalArgumentException("Not Supported");
	}



}