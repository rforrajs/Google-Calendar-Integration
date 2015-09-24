// start of class
package com.social.controller;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets.Details;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.calendar.Calendar.Events;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;

@Controller
@RequestMapping("/connect")
public class GoogleConnectController {

	private final static Log logger = LogFactory.getLog(GoogleConnectController.class);
	private static final String APPLICATION_NAME = "";
	private static HttpTransport httpTransport;
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	private static com.google.api.services.calendar.Calendar client;

	GoogleClientSecrets clientSecrets;
	GoogleAuthorizationCodeFlow flow;	
	Credential credential;

	private String clientId="464094426701-mdnqfuio4qo4fevu29evjd87ra7a5ge8.apps.googleusercontent.com";
	private String clientSecret="rq-e5lGS5nB71g5RL66l-P6_";
	private String redirectURI="http://localhost:8080/TestGoogleConnection/connect/google.do";
	private Set<Event> events=new HashSet<Event>();

	public void setEvents(Set<Event> events) {
		this.events = events;
	}

	@RequestMapping(value="/google.do", method=RequestMethod.GET)
	public RedirectView googleConnectionStatus(HttpServletRequest request) throws Exception  {
		return new RedirectView(authorize());
	}

	@RequestMapping(value="/google.do", method=RequestMethod.GET,  params="code" )
	public ModelAndView oauth2Callback(@RequestParam(value="code") String code, ModelAndView mv) {
		try {
			TokenResponse response = flow.newTokenRequest(code).setRedirectUri(redirectURI).execute();
			credential=flow.createAndStoreCredential(response, "userID");
			client = new com.google.api.services.calendar.Calendar.Builder(httpTransport, JSON_FACTORY, credential).
					setApplicationName(APPLICATION_NAME).build();

			Events events=client.events();
			com.google.api.services.calendar.model.Events eventList=events.list("primary").execute();
			mv.addObject("events", eventList.getItems());			
		} catch (Exception e) {
			logger.warn("Exception while handling OAuth2 callback (" + e.getMessage() + ")."
					+ " Redirecting to google connection status page.");
		}
		mv.setViewName("eventList");
		return mv;
	}

	public Set<Event> getEvents() throws IOException{
		return this.events;
	}

	private String  authorize() throws Exception {
		AuthorizationCodeRequestUrl authorizationUrl;
		if(flow==null){
			Details web=new Details();
			web.setClientId(clientId);
			web.setClientSecret(clientSecret);
			clientSecrets = new GoogleClientSecrets().setWeb(web);
			httpTransport = GoogleNetHttpTransport.newTrustedTransport();
			flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets,
					Collections.singleton(CalendarScopes.CALENDAR)).build();
		}
		authorizationUrl = flow.newAuthorizationUrl().setRedirectUri(redirectURI);
		return authorizationUrl.build();
	}
}
