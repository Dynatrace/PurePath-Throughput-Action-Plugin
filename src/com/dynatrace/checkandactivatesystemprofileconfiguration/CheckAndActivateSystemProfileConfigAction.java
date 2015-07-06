package com.dynatrace.checkandactivatesystemprofileconfiguration;


import com.dynatrace.diagnostics.pdk.*;
import com.dynatrace.diagnostics.plugin.actionhelper.ActionData;
import com.dynatrace.diagnostics.plugin.actionhelper.ActionHelper;
import com.dynatrace.diagnostics.plugin.actionhelper.HelperUtils;
import com.dynatrace.diagnostics.automation.rest.sdk.*;
import com.dynatrace.diagnostics.automation.rest.sdk.entity.BaseConfiguration;
import com.dynatrace.diagnostics.pdk.Violation.TriggerValue;

import java.util.logging.Logger;
import java.util.ArrayList;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.*;
import javax.net.ssl.*;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.*;
import javax.xml.xpath.XPathVariableResolver;
import javax.xml.namespace.QName;
import org.apache.http.client.ClientProtocolException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;


public class CheckAndActivateSystemProfileConfigAction implements Action {

	private static final Logger log = Logger.getLogger(CheckAndActivateSystemProfileConfigAction.class.getName());
	private String levelOne;
	private String levelTwo;
	private String levelThree;
	private String levelFour;
	private String levelFive;
	private String username;
	private String password;
	private String httpmethod;
	private URLConnection connection;
	private String urlprotocol;
	private int urlport;
	private String dynaTraceURL;
	private String activeConfiguration;
	private URL activeConfUrl;
	private String sysProfileConfURL;
	private NodeList xpathNodeList;	

	@Override
	public Status setup(ActionEnvironment env) throws Exception {
		
		//SET VARIABLES
		levelOne = env.getConfigString("levelOne");
		levelTwo = env.getConfigString("levelTwo");
		levelThree = env.getConfigString("levelThree");
		levelFour = env.getConfigString("levelFour");
		levelFive = env.getConfigString("levelFive");
		username = env.getConfigString("username");
		password = env.getConfigPassword("password");
		
		return new Status(Status.StatusCode.Success);
	}


	@Override
	public Status execute(ActionEnvironment env) throws Exception {
		
		log.finer("Entering execute method");
			
		try {
			
			String profileName = env.getSystemProfileName();
			//String profileName = "HostMonitoring";  //testing
			log.finer("profileName :" + profileName);
		
			//Login to dynatrace server
			log.finer("Entering username/password setup");
			String userpass = username + ":" + password;
			String basicAuth = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(userpass.getBytes());
		
			disableCertificateValidation();
			
			//Build URL
			//Request URL ->  http://[SERVERURL]/rest/management/profiles/[SYSTEMPROFILE]/configurations 
			log.finer("Entering URL Setup");
			RESTEndpoint endPoint = new RESTEndpoint(username, password, env.getHost().getAddress());
			sysProfileConfURL = "/rest/management/profiles/" + profileName + "/configurations";
			log.finer("sysProfileConfURL: " + sysProfileConfURL);
			
			String tempString = endPoint.getAddress().toString();
			String[] tempArray = tempString.split("://");
			String protocol = tempArray[0];
			tempArray = tempArray[1].split(":");
			String serverURL = tempArray[0];
			tempString = tempArray[1];
			int port = Integer.parseInt(tempString);
			
			activeConfUrl = new URL(protocol, serverURL, port, sysProfileConfURL);		
			log.finer("Executing URL: " + activeConfUrl.toString());
			
			//Grab XML file with active/inactive system profile configurations
			log.finer("Entering XML file grab");
			connection = activeConfUrl.openConnection();
			connection.setRequestProperty("Authorization", basicAuth);
			connection.setConnectTimeout(50000);

			InputStream responseIS = connection.getInputStream();	
			DocumentBuilderFactory xmlFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = xmlFactory.newDocumentBuilder();
			Document xmlDoc = docBuilder.parse(responseIS);
			XPathFactory xpathFact = XPathFactory.newInstance();
			XPath xpath = xpathFact.newXPath();
			
			//read XML and return current active configuration
			log.finer("Entering XML file read");
			xpathNodeList = (NodeList) xpath.evaluate("configurations/configuration[contains(@isactive, 'true')]", xmlDoc, XPathConstants.NODESET);
			tempString = xpathNodeList.item(0).getAttributes().getNamedItem("id").toString();
			activeConfiguration = tempString.replaceAll("\"","").replaceAll("id=","");
			log.info("Active Configuration: " + activeConfiguration);
			
			//Determine current capture level
			log.finer("Entering switch statement");
			int levelSwitch = 0;
			if (activeConfiguration.equals(levelOne))
				levelSwitch = 1;
			if (activeConfiguration.equals(levelTwo))
				levelSwitch = 2;
			if (activeConfiguration.equals(levelThree))
				levelSwitch = 3;
			if (activeConfiguration.equals(levelFour))
				levelSwitch = 4;
			if (activeConfiguration.equals(levelFive))
				levelSwitch = 5;
			log.finer("levelSwitch: " + levelSwitch);
			
			
			//Incident info
			Collection<Incident> incidents = env.getIncidents();
			log.finer("incidents" + incidents);
			
			for (Incident incident : incidents) {
				HashMap<String,String> incidentStringSubMap = this.getSubMap(log, incident);		
				String incidentName = incidentStringSubMap.get("INCIDENT_NAME"); 
				log.finer("incidentStringSubMap: " + incidentStringSubMap.toString());		
				
				switch (levelSwitch)
				{
					case 1: 					
						log.finer("Case 1");						
						if (incidentName.contains(">")){
							endPoint.activateConfiguration(profileName, levelTwo);
							log.info("Configuration " + levelTwo + " has been activated on " + profileName);
						}
					break;
				
				
					case 2:					
						log.finer("Case 2");
						if (incidentName.contains(">")) {
							endPoint.activateConfiguration(profileName, levelThree);
							log.info("Configuration " + levelThree + " has been activated on " + profileName);
						}
						
						else if (incidentName.contains("<")){
							endPoint.activateConfiguration(profileName, levelOne);
							log.info("Configuration " + levelOne + " has been activated on " + profileName);
						}							
					break;
				
				
					case 3: 				
						log.finer("Case 3");
						if (incidentName.contains(">")) {
							endPoint.activateConfiguration(profileName, levelFour);
							log.info("Configuration " + levelFour + " has been activated on " + profileName);
						}
						
						else if (incidentName.contains("<")){
							endPoint.activateConfiguration(profileName, levelTwo);
							log.info("Configuration " + levelTwo + " has been activated on " + profileName);
						}
					break;
					
					
					case 4: 				
						log.finer("Case 4");
						if (incidentName.contains(">")) {
							endPoint.activateConfiguration(profileName, levelFive);
							log.info("Configuration " + levelFive + " has been activated on " + profileName);
						}
						
						else if (incidentName.contains("<")){
							endPoint.activateConfiguration(profileName, levelThree);
							log.info("Configuration " + levelThree + " has been activated on " + profileName);
						}
					break;
					
					
					case 5: 				
						log.finer("Case 5");
						if (incidentName.contains("<")){
							endPoint.activateConfiguration(profileName, levelFour);
							log.info("Configuration " + levelFour + " has been activated on " + profileName);
						}
					break;
				}
			}
		
			
			
			
		} catch (ClientProtocolException e) {
			 log.info("ClientProtocolException: " + e);
			 return new Status(Status.StatusCode.ErrorInternal);

		} catch (IOException e) {
			log.finer("IOException: " + e);
			return new Status(Status.StatusCode.ErrorInternal);

		} catch (Exception e){
			log.finer("Exception: " + e);
			return new Status(Status.StatusCode.ErrorInternal);
		}	
		
		return new Status(Status.StatusCode.Success);
	}

	@Override
	public void teardown(ActionEnvironment env) throws Exception {
		// TODO
	}
	
	public static void disableCertificateValidation() {
		
		log.finer("Entering disableCertificateValidation method");  
		
		//Create a trust manager that does not validate certificate chains
		  TrustManager[] trustAllCerts = new TrustManager[] { 
		    new X509TrustManager() {
		      public X509Certificate[] getAcceptedIssuers() { 
		        return new X509Certificate[0]; 
		      }
		      public void checkClientTrusted(X509Certificate[] certs, String authType) {}
		      public void checkServerTrusted(X509Certificate[] certs, String authType) {}
		  }};

		  //Ignore differences between given hostname and certificate hostname
		  HostnameVerifier hv = new HostnameVerifier() {
		    public boolean verify(String hostname, SSLSession session) { return true; }
		  };

		  //Install the all-trusting trust manager
		  try {
		    SSLContext sc = SSLContext.getInstance("SSL");
		    sc.init(null, trustAllCerts, new SecureRandom());
		    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		    HttpsURLConnection.setDefaultHostnameVerifier(hv);
		  } catch (Exception e) {}
		  
		  log.finer("Leaving disableCertificateValidation method");
	}
	
	
	//SUBSTITUTE STRINGS
	private HashMap<String, String> getSubMap(Logger log, Incident incident){
		
		log.finer("enter getSubMap Method");
		
		String message = incident.getMessage();
			log.info("Incident " + message + " triggered.");
				for (Violation violation : incident.getViolations()) {
					log.finer("Measure " + violation.getViolatedMeasure().getName() + " violoated threshold.");
					incident.getViolations();
				}	
		
		
		//CREATE SUBMAP
		HashMap<String, String> subMap = new HashMap<String, String>();
				
		//ADD TO SUB MAP
		subMap.put("INCIDENT_MESSAGE", incident.getMessage()); 
		subMap.put("INCIDENT_SEVERITY", incident.getSeverity().toString()); 
		subMap.put("INCIDENT_NAME", incident.getIncidentRule().getName()); 	
			
		log.finer("subMap: " + subMap);
		log.finer("exit getSubMap Method");
		return subMap;
	}
}
