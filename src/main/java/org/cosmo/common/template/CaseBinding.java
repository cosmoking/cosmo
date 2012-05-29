package org.cosmo.common.template;

import java.util.ArrayList;
import java.util.Vector;

import org.cosmo.common.net.StringTokens;
import org.cosmo.common.util.Quad;

public class CaseBinding extends Binding
{
	public static final Quad<Integer, Integer, Integer, Integer> EmptySegmentBoundary = new Quad(Integer.MAX_VALUE, 0, 0, 0); //
	public static final CaseBinding NoOp = new CaseBinding();
	public static final CaseBinding End = new CaseBinding();

		// set start idx, seg end idx, binding start, end idx
	public Vector<Quad<Integer, Integer, Integer, Integer>> _caseSegmentBoundaries;

		// this is for one line expr in which we have "list of bindings" insteand of segments
	public Binding[] _caseShortformBindings;


		// marks the bound of the case segments
	public int _segmentStartIdx;
	public int _segmentEndIdx;


		// marks the begin of the binding index of the above segments
	public int _bindingStartIdx;
	public int _bindingEndIdx;

		// flag of new case segment
	public boolean _newSegment;

		// tracks the depth of case statements, allow recursive of case statements
		// ie (when depth == 1) then track the case segments. (see bindingbindingSrc.java)
	public int _depth;


	public String _bindingExprStr;
	public Binding _bindingExpr;



	public CaseBinding ()
	{
		_newSegment = true;
	}


	public void setBindingExpr (String bindingExpr)
	{
		_bindingExprStr = bindingExpr;
	}

	public void setShortformBindings (StringTokens args, Page page, Class<? extends BindingSrc> bindingSrc, Options options)
	  throws Exception
	{
		ArrayList<Binding> strs = new ArrayList();
		while (args.hasNext()) {
			Binding binding = Parser.parseBinding(args.next(), page, bindingSrc, options);
			strs.add(binding);
		}
		_caseShortformBindings = strs.toArray(new Binding[]{});
	}

	public boolean isEnclosedExpr () {
		return  _caseSegmentBoundaries != null;
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

		// create new boundary
	public void markCaseSegmentBoundary ()
	{
		if (_caseSegmentBoundaries == null) {
			_caseSegmentBoundaries = new Vector();
		}
		Quad boundary = new Quad(_segmentStartIdx, _segmentEndIdx, _bindingStartIdx, _bindingEndIdx);
		_caseSegmentBoundaries.add(boundary);
		_newSegment = true;

	}

	@Override
	public Object applyValue (Page page, BindingSrc bindingSrc, Content container, Object context, Options options)
	  throws Exception
	{
		if (_bindingExpr == null) {
			_bindingExpr = Parser.parseBinding(_bindingExprStr, page, bindingSrc.getClass(), options);
		}
		Object result =  bindingSrc.applyValue(_bindingExpr, page, null, context, options);


		int index = Integer.MAX_VALUE;
		if (result instanceof Boolean) {
			index = ((Boolean)result).booleanValue() ? 0 : 1;
		}
		else if (result instanceof Number) {
			index = ((Number)result).intValue();
		}
		else if (result == null || result == "") {
				// this is when virtual binding is being eval but no binding is provided. so false
			index = 1;
		}
		else {
				// otherwise there is some content so true
			index = 0;
			//throw new IllegalArgumentException(New.str("Case result [", result, "] from binding [", _bindingExpr.name(), "] on page [", page._name, "] is not an integer or boolean"));
		}

			  // segments based
		if (_caseSegmentBoundaries != null) {
			Quad<Integer, Integer, Integer, Integer> segmentBoundary = index < _caseSegmentBoundaries.size() ? _caseSegmentBoundaries.get(index) : EmptySegmentBoundary;
			page.append(segmentBoundary._t1, segmentBoundary._t2, segmentBoundary._t3, context, container, bindingSrc);
		}
			  // segment string based
		else if (_caseShortformBindings != null) {
			if (index < _caseShortformBindings.length) {
				bindingSrc.applyValue(_caseShortformBindings[index], page, container, context, options);
			}
			else {
				container.pushBinding("");
			}
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