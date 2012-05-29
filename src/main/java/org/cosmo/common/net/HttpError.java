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
package org.cosmo.common.net;

import static org.jboss.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;


public class HttpError extends Exception
{
	public HttpResponseStatus _httpStatus;
	public String _message;

		// TODO, define on httpStatus that tells where error is thrown and which error
	public HttpError (HttpResponseStatus httpStatus, String message)
	{
		_httpStatus = httpStatus;
		_message = message;
	}

	public HttpError (HttpResponseStatus httpStatus)
	{
		this (httpStatus, httpStatus.getReasonPhrase());
	}


	public HttpError (String message)
	{
		this (INTERNAL_SERVER_ERROR, message);
	}
}
