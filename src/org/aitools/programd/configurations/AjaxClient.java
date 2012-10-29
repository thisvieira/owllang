/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package org.aitools.programd.configurations;


import org.mortbay.http.HttpServer;
import org.mortbay.http.HttpContext;
import org.mortbay.http.handler.ResourceHandler;
import org.mortbay.http.SocketListener;

/**
 * An implementation of Program D combined with a web server.
 * 
 * @author <a href="mailto:noel@aitools.org">Noel Bush</a>
 */
public class AjaxClient
{
    private HttpServer server;

    

    /**
     * A WebClient without a console.
     * 
     * @param corePropertiesPath the path to the console properties file
     * @param webServerPropertiesPath the path to the web server properties file
     */
    public AjaxClient()
    {
        initialize();
    }

    private void initialize()
    {
    	try
    	{
    		server = new HttpServer();
    		SocketListener listener = new SocketListener();
    		listener.setPort(8081);
    		server.addListener(listener);

    		HttpContext context = new HttpContext();
    		context.setContextPath("/");
    		context.setResourceBase("./docroot/");
    		context.addHandler(new ResourceHandler());
    		server.addContext(context);

    		
    	} catch(Exception e)
    	{
			e.printStackTrace();
    		System.exit(-1);
    	}
    }

    /**
     * Starts the core and the http server.
     */
    public void run()
    {
    	try {
			server.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    private static void usage()
    {
        /*System.out.println("Usage: web-server -c <CORE_CONFIG> -w <WEBSERVER_CONFIG> [-n <CONSOLE_CONFIG>]");
        System.out.println("Start up a web server version of Program D using the specified config files.");
        System.out.println();
        System.out.println("  -c, --core-properties        the path to the core configuration (XML properties) file");
        System.out.println("  -w, --web-server-properties  the path to the web server configuration (XML properties) file");
        System.out.println("  -n, --console-properties     the path to the console configuration (XML properties) file");
        System.out.println();
        System.out.println("Report bugs to <programd@aitools.org>");*/
    }

    /**
     * Starts up the WebServer configuration. Required arguments are:
     * <ul>
     * <li><code>-c, --core-properties     the path to the core configuration (XML properties) file</code></li>
     * <li><code>-w, --web-server-properties  the path to the web server configuration (XML properties) file</code></li>
     * <li><code>-n, --console-properties  the path to the console configuration (XML properties) file</code></li>
     * </ul>
     * 
     * @param argv
     */
    public static void main(String[] argv)
    {
        /*int opt;
        LongOpt[] longopts = new LongOpt[3];
        longopts[0] = new LongOpt("core-properties", LongOpt.REQUIRED_ARGUMENT, null, 'c');
        longopts[1] = new LongOpt("web-server-properties", LongOpt.REQUIRED_ARGUMENT, null, 'w');
        longopts[2] = new LongOpt("console-properties", LongOpt.REQUIRED_ARGUMENT, null, 'n');

        Getopt getopt = new Getopt("web-server", argv, ":c:n:w:", longopts);

        while ((opt = getopt.getopt()) != -1)
        {
            switch (opt)
            {
                case 'c':
                    corePropertiesPath = getopt.getOptarg();
                    break;

                case 'n':
                    consolePropertiesPath = getopt.getOptarg();
                    break;

                case 'w':
                    webServerPropertiesPath = getopt.getOptarg();
                    break;
            }
        }

        if (corePropertiesPath == null)
        {
            System.err.println("You must specify a core properties path.");
            usage();
            System.exit(1);
        }

        if (webServerPropertiesPath == null)
        {
            System.err.println("You must specify a web server properties path.");
            usage();
            System.exit(1);
        }*/

        AjaxClient client;

        client = new AjaxClient();
       
        client.run();
    }
}