package httpserver;

import java.io.DataOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

/**
 * An HTTPHandler is what all handlers used by your server descend from. <p>
 *
 * Extended classes have two options for determining their actions: they may
 * override the handle method (slightly harder), or use the addGet and addPost
 * methods in the constructor. See their descriptions for more information. <p>
 *
 * If you just want to send a static message to the client, regardless of
 * request, you can use a MessageHandler, instead of creating a new Handler.
 *
 * @see HTTPHandler#handle
 * @see HTTPHandler#addGET
 * @see HTTPHandler#addPOST
 * @see MessageHandler
 */
public abstract class HTTPHandler {
    private final HashMap<String, ArrayList<MethodWrapper>> routes = new HashMap<>();

    private Socket socket;
    private DataOutputStream writer;


    /**
     * Create an HTTPHandler. <p>
     *
     * When writing your own HTTPHandler, this is where you should add the
     * handler's internal routing, as well performing any setup tasks. Handlers
     * are multi-use, which means that only one of any kind of handler should be
     * created in an application (unless you have custom needs).
     *
     * @throws HTTPException  The exception typically comes from trying to add
     *                        a new method. In a standard configuration this will
     *                        keep the server from starting.
     */
    public HTTPHandler() throws HTTPException { }


    /**
     * Where the Handler handles the information given from the request and
     * based off of the paths specified in the Handler. <p>
     *
     * This can be overridden for more fine-grained handling. As is, it uses
     * the data behind the addGET, addPOST, and addDELETE methods for determining
     * the correct action to take. <p>
     *
     * If there is not exact match, the `*` path is used. If you don't have a `*`
     * catchall route, a 501 (Not implemented) is sent to the client.
     *
     * @param request     The incoming HTTPRequest.
     * @param response    The outgoing HTTPResponse, waiting to be filled by an
     *                    HTTPHandler.
     *
     * @see HTTPHandler#addGET
     * @see HTTPHandler#addPOST
     * @see HTTPHandler#addDELETE
     * @see HTTPResponse#NOT_A_METHOD_ERROR
     */
    public void handle(HTTPRequest request, HTTPResponse response) {
	String httpRequestType = request.getRequestType().toUpperCase();
	if (!routes.containsKey(httpRequestType)) {
	    response.message(404, "No " + httpRequestType + " routes exist.");
	    return;
	}

	MethodWrapper finalMethod = null;
	int bestFit = 0;
	for (MethodWrapper method : routes.get(httpRequestType)) {
	    if (method.matchesPerfectly(request.getSplitPath())) {
			finalMethod = method;
			break;
	    }

	    int testScore = method.howCorrect(request.getSplitPath());
	    if (testScore > bestFit) {
			finalMethod = method;
			bestFit = testScore;
			}
		}

		if (finalMethod == null) {
			response.message(501, HTTPResponse.NOT_A_METHOD_ERROR);
			return;
		}

		finalMethod.invoke(request, response, request.getSplitPath());
    }

    /**
     * Attach a method to a GET request at a path. <p>
     *
     * Methods are passed in as a String, and must be a member of the current
     * handler.<p>
     *
     * Path's should come in "/path/to/action" form. If the method requires
     * any parameters that aren't an HTTPResponse, HTTPRequest, or Map,
     * they should be included in the path, in the order they're
     * listed in the method header, in "{ClassName}" form. Example:
     * <code>/hello/{String}/{String}</code> is a good path. <p>
     *
     * Methods being passed in must accept an HTTPResponse as their first
     * parameter. Methods may optionally accept an HTTPRequest and a
     * Map&lt;String, String&gt; in that order (they may accept a Map but not an
     * HTTPRequest, but if they accept both the HTTPRequest must come first).
     *
     * Parameters following the above must be included in the java.lang library
     * and have a constructor that takes in a String.
     * Any other type of parameter will cause an exception to occur. <p>
     *
     * Additionally, primitives are not permited, because they're not classes in
     * the java.lang library. The three most common parameter types are String,
     * Integer, and Double.
     *
     * @param path        Path to match
     * @param methodName  Method belonging to the current class, in String form.
     * @throws HTTPException When you do bad things.
     *
     * @see HTTPHandler#addPOST
     * @see HTTPHandler#addDELETE
     * @see HTTPResponse
     * @see HTTPRequest
     */
    public void get(String path, Route route) throws HTTPException {
		addRoute(HTTPRequest.GET_REQUEST_TYPE, path, route);
    }

    /**
     * Attach a method to a POST request at a path. <p>
     *
     * For a more detailed explanation, see {@link HTTPHandler#addGET}.
     *
     * @param path         Path to match
     * @param methodName   Class and Method in class#method form.
     * @throws HTTPException When you do bad things.
     *
     * @see HTTPHandler#addGET
     * @see HTTPHandler#addDELETE
     */
    public void post(String path, Route route) throws HTTPException {
		addRoute(HTTPRequest.POST_REQUEST_TYPE, path, route);
    }

    /**
     * Attach a method to a DELETE request at a path. <p>
     *
     * For a more detailed explanation, see {@link HTTPHandler#addGET}.
     *
     * @param path        Path to match
     * @param methodName  Class and Method in class#method form.
     * @throws HTTPException when you do bad things.
     *
     * @see HTTPHandler#addGET
     * @see HTTPHandler#addPOST
     */
    public void delete(String path, Route route) throws HTTPException {
		addRoute(HTTPRequest.DELETE_REQUEST_TYPE, path, route);
    }

    /**
     * Add a method to a path in a map. <p>
     *
     * Methods are passed in using "methodName", meaning they must be a member of
     * the current handler.
     *
     * @param httpMethod    The HTTP method this route will match to.
     * @param path	    Path to match.
     * @param route	    The Route to be called at said path.
     *
     * @throws HTTPException  When you do bad things.
     */
    public void addRoute(String httpMethod, String path, Route route) throws HTTPException {
		httpMethod = httpMethod.toUpperCase();

		MethodWrapper method = new MethodWrapper(path, route);
    	if (!routes.containsKey(httpMethod)) {
		   routes.put(httpMethod, new ArrayList<>());
    	}

    	routes.get(httpMethod).add(method);
    }



    /******************************
      Generic getters and setters
     ******************************/

    public void setSocket(Socket socket) {
		this.socket = socket;
    }
    public Socket getSocket() {
		return socket;
    }

    public void setWriter(DataOutputStream writer) {
		this.writer = writer;
    }
    public DataOutputStream getWriter() {
		return writer;
    }
}