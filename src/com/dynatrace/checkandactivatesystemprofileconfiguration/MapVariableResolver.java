package com.dynatrace.checkandactivatesystemprofileconfiguration;

import com.dynatrace.diagnostics.pdk.*;

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
import javax.xml.namespace.QName;

import org.apache.http.client.ClientProtocolException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;


public class MapVariableResolver implements XPathVariableResolver {
  // local store of variable name -> variable value mappings
  Map<String, String> variableMappings = new HashMap<String, String>();


  // a way of setting new variable mappings 
  public void setVariable(String key, String value)  {
    variableMappings.put(key, value);
  }

  
  // override this method in XPathVariableResolver to 
  // be used during evaluation of the XPath expression      
  @Override
  public Object resolveVariable(QName varName) {
    // if using namespaces, there's more to do here
    String key = varName.getLocalPart();
    return variableMappings.get(key);
  }
  
}