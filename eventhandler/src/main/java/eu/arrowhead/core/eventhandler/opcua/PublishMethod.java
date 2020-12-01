package eu.arrowhead.core.eventhandler.opcua;

import java.io.InputStream;
import java.net.URI;
import java.security.Principal;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.arrowhead.common.Utility;
import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.common.messages.PublishEvent;
import eu.arrowhead.core.eventhandler.EventHandlerResource;

public class PublishMethod extends AbstractMethodInvocationHandler {
	private static final Logger log = LoggerFactory.getLogger(PublishMethod.class.getName());

	public PublishMethod(UaMethodNode node) {
		super(node);
	}

	public static final Argument PublishEvent = new Argument("PublishEvent", Identifiers.String,
			ValueRanks.Scalar, null, new LocalizedText("OrchestrationInput"));

	@Override
	public Argument[] getInputArguments() {
		return new Argument[] { PublishEvent };
	}

	@Override
	public Argument[] getOutputArguments() {
		return new Argument[] { };
	}

	@Override
	protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException {
		log.debug("Invoking publish() method of Object '{}'", invocationContext.getObjectId());

		try {
			new EventHandlerResource().publishEvent(
					Utility.fromJson(inputValues[0].getValue().toString(), PublishEvent.class), new ContainerRequestContext() {
						
						@Override
						public void setSecurityContext(SecurityContext context) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setRequestUri(URI baseUri, URI requestUri) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setRequestUri(URI requestUri) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setProperty(String name, Object object) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setMethod(String method) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void setEntityStream(InputStream input) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void removeProperty(String name) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public boolean hasEntity() {
							// TODO Auto-generated method stub
							return false;
						}
						
						@Override
						public UriInfo getUriInfo() {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						public SecurityContext getSecurityContext() {
							return new SecurityContext() {
								
								@Override
								public boolean isUserInRole(String role) {
									// TODO Auto-generated method stub
									return false;
								}
								
								@Override
								public boolean isSecure() {
									return false;
								}
								
								@Override
								public Principal getUserPrincipal() {
									// TODO Auto-generated method stub
									return null;
								}
								
								@Override
								public String getAuthenticationScheme() {
									// TODO Auto-generated method stub
									return null;
								}
							};
						}
						
						@Override
						public Request getRequest() {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						public Collection<String> getPropertyNames() {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						public Object getProperty(String name) {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						public String getMethod() {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						public MediaType getMediaType() {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						public int getLength() {
							// TODO Auto-generated method stub
							return 0;
						}
						
						@Override
						public Locale getLanguage() {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						public MultivaluedMap<String, String> getHeaders() {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						public String getHeaderString(String name) {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						public InputStream getEntityStream() {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						public Date getDate() {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						public Map<String, Cookie> getCookies() {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						public List<MediaType> getAcceptableMediaTypes() {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						public List<Locale> getAcceptableLanguages() {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						public void abortWith(Response response) {
							// TODO Auto-generated method stub
							
						}
					});	
		} catch (DataNotFoundException e) {
			log.debug("Error publish: {}", e);
		}

		return new Variant[0];
	}
}
