package com.openshift.jenkins.plugins.pipeline.model;

import hudson.Launcher;
import hudson.model.TaskListener;

import java.util.List;
import java.util.Map;

import org.jboss.dmr.ModelNode;

import com.openshift.jenkins.plugins.pipeline.MessageConstants;
import com.openshift.restclient.IClient;

public interface IOpenShiftDeleterJsonYaml extends IOpenShiftApiObjHandler {

	final static String DISPLAY_NAME = "Delete OpenShift Resource(s) from JSON/YAML";
	
	String getJsonyaml();
		
    default String getJsonyaml(Map<String,String> overrides) {
		return getOverride(getJsonyaml(), overrides);
    }
    
	default boolean coreLogic(Launcher launcher, TaskListener listener, Map<String,String> overrides) {
		boolean chatty = Boolean.parseBoolean(getVerbose(overrides));
    	listener.getLogger().println(String.format(MessageConstants.START_DELETE_OBJS, DISPLAY_NAME, getNamespace(overrides)));
    	updateApiTypes(chatty, listener, overrides);
    	
    	ModelNode resources = this.hydrateJsonYaml(getJsonyaml(overrides), chatty ? listener : null);
    	if (resources == null) {
    		return false;
    	}
    	    	
    	// get oc client 
    	IClient client = this.getClient(listener, DISPLAY_NAME, overrides);
    	
    	if (client != null) {
	    	//cycle through json and POST to appropriate resource
	    	String kind = resources.get("kind").asString();
	    	// rc[0] will be successful deletes, rc[1] will be failed deletes
	    	int[] rc = new int[2];
	    	if (kind.equalsIgnoreCase("List")) {
	    		List<ModelNode> list = resources.get("items").asList();
	    		for (ModelNode node : list) {
	    			String path = node.get("kind").asString();
	    			String name = node.get("metadata").get("name").asString();
					
	    			rc = deleteAPIObjs(client, listener, getNamespace(overrides), path, name, null);
	
	    		}
	    	} else {
	    		String path = kind;
	    		String name = resources.get("metadata").get("name").asString();
	    		
    			rc = deleteAPIObjs(client, listener, getNamespace(overrides), path, name, null);
	    		
	    	}
	
	    	if (rc[1] > 0) {
	    		listener.getLogger().println(String.format(MessageConstants.EXIT_DELETE_BAD, DISPLAY_NAME, rc[0], rc[1]));
				return false;
	    	} else {
	    		listener.getLogger().println(String.format(MessageConstants.EXIT_DELETE_GOOD, DISPLAY_NAME, rc[0]));
	    		return true;
	    	}
    	}
		return false;
	}
    
	
}
