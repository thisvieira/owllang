/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package org.aitools.programd.server.servlet;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletOutputStream;

import org.aitools.programd.Core;
import org.aitools.programd.server.ServletRequestTransactionEnvelope;
import org.aitools.programd.server.ServletRequestResponderManagerRegistry;
import org.aitools.programd.util.DeveloperError;
import org.aitools.programd.util.UserError;

/**
 * <p>
 * This is the chat servlet used to broker a conversation from a client. 
 * </p>
 * <ol>
 * <li>Determining the type of client requesting a bot response (via
 * User-Agent)</li>
 * <li>Obtaining a bot response from the Graphmaster</li>
 * <li>Forwarding the bot response to the appropriate Responder</li>
 * </ol>
 * 
 * @author Karsten Oster Lundqvist
 * @author <a href="mailto:k.o.lundqvist@rdg.ac.uk">Karsten Oster Lundqvist</a>
 */




public class AjaxD extends HttpServlet
{
    /**
	 * 
	 */

	/** The string &quot;{@value}&quot;. */
    private static final String CORE = "core";
    
    /** The string &quot;{@value}&quot;. */
    private static final String RESPONDER_REGISTRY = "responder-registry";

    /** The Core to use. */
    private Core core;

    /** The ServletRequestResponderManagerRegistry to use. */
    private ServletRequestResponderManagerRegistry responderRegistry;
    
    /** An empty string. */
    private static final String EMPTY_STRING = "";

    /** The connect string. */
    private static String connectString="CONNECT";

    /** The inactivity string. */
    private static String inactivityString;

    /** The name of the text parameter in a request (&quot;text&quot;). */
    private static final String TEXT_PARAM = "text";

    /** The name of the userid parameter in a request (&quot;userid&quot;). */
    private static final String USERID_PARAM = "userid";

    /** The name of the botid parameter in a request (&quot;botid&quot;). */
    private static final String BOTID_PARAM = "botid";

    /**
     * The name of the response encoding parameter in a request
     * (&quot;response_encoding&quot;).
     */
    private static final String RESPONSE_ENCODING_PARAM = "response_encoding";

    /** The string &quot;{@value}&quot; (for character encoding conversion). */
    private static final String ENC_8859_1 = "8859_1";

    /** The string &quot;{@value}&quot; (for character encoding conversion). */
    private static final String ENC_UTF8 = "utf-8";

    /** The string &quot;{@value}&quot;. */
    private static final String TEMPLATE = "template";

    /**
     * @see javax.servlet.GenericServlet#init()
     */
    public void init()
    {
        this.core = (Core) this.getServletContext().getAttribute(CORE);
        this.responderRegistry = (ServletRequestResponderManagerRegistry) this.getServletContext().getAttribute(RESPONDER_REGISTRY);
    }

    /**
     * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
     */
    public void init(ServletConfig config)
    {
        this.core = (Core) config.getServletContext().getAttribute(CORE);
        this.responderRegistry = (ServletRequestResponderManagerRegistry) config.getServletContext().getAttribute(RESPONDER_REGISTRY);
    }

    /**
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response)
    {
    	String referer=request.getHeader("X-Referer");
    	if(referer!=null)
        if (!referer.equals("http://athene:8001/chatter.html"))
        {
            return;
        }
        try
        {
        	String userRequest = request.getParameter(TEXT_PARAM);
        	String userid = request.getParameter(USERID_PARAM);
        	String botid = request.getParameter(BOTID_PARAM);
        	String responseEncoding = request.getParameter(RESPONSE_ENCODING_PARAM);

            // If no text parameter then we assume a new connection.
            if (userRequest == null)
            {
                userRequest = connectString;
            }
            // Check for blank request.
            else if (userRequest.equals(EMPTY_STRING))
            {
                userRequest = inactivityString;
            }
            // Convert to UTF-8.
            else
            {
                try
                {
                    userRequest = new String(userRequest.getBytes(ENC_8859_1), ENC_UTF8);
                }
                catch (UnsupportedEncodingException e)
                {
                    throw new DeveloperError("Encodings are not properly supported!", e);
                }
            }

            // If no response encoding specified, use UTF-8.
            if (responseEncoding == null)
            {
                responseEncoding = ENC_UTF8;
            }

            // Check for no userid.
            if (userid == null)
            {
                userid = request.getRemoteHost();
            }

            // Check for no bot id.
            if (botid == null)
            {
                botid = this.core.getBots().getABot().getID();
            }

            // Look for a named template.
            String templateName = request.getParameter(TEMPLATE);
            if (templateName == null)
            {
                templateName = EMPTY_STRING;
            }

            ServletOutputStream serviceOutputStream;
            try
            {
            	serviceOutputStream = response.getOutputStream();
            }
            catch (IOException e)
            {
                throw new DeveloperError("Error getting service response output stream.", e);
            }

            String botResponse = this.core.getResponse(userRequest, userid, botid);

            try
            {
                serviceOutputStream.write(botResponse.getBytes(responseEncoding));
            }
            catch (UnsupportedEncodingException e0)
            {
                throw new UserError("UTF-8 encoding is not supported on your platform!", e0);
            }
            catch (IOException e1)
            {
                throw new DeveloperError("Error writing to service output stream.", e1);
            }
            try
            {
                serviceOutputStream.flush();
            }
            catch (IOException e)
            {
                throw new DeveloperError("Error flushing service output stream.", e);
            }
            try
            {
                serviceOutputStream.close();
            }
            catch (IOException e)
            {
                throw new DeveloperError("Error closing service output stream.", e);
            }
        }
        catch (Throwable e)
        {
            this.core.fail(e);
        }
    }

    /**
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response)
    {
        doGet(request, response);
    }
}