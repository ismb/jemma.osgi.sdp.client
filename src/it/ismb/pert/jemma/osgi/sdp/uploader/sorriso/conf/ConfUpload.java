package it.ismb.pert.jemma.osgi.sdp.uploader.sorriso.conf;

import java.util.HashMap;
import java.util.Map;


/**
 * Simple class holding the configuration of a simple upload.
 * */
public class ConfUpload {
	
	public Map<String, String> dal_parameters;
	public Map<String, String> fixed_parameters;
	
	public ConfUpload(){
	    this.dal_parameters = new HashMap<String,String>();
	    this.fixed_parameters = new HashMap<String,String>();
	}

	public String functionuid;
	public String operation;
	public String sdp_stream;

	
	public String toString(){
	    String ret = "";
	    ret += "[functionuid="+functionuid+"] ";
	    ret += "[operation="+operation+"] ";
	    ret += "[sdp_stream="+sdp_stream+"]\n";
	    ret += "dal_params:\n";
	    for (String key : this.dal_parameters.keySet()) {
		String val = this.dal_parameters.get(key);
		ret += "\t\t" + key + "->" + val + "\n";
	    }
	    ret += "fixed_params:\n";
	    for (String key : this.fixed_parameters.keySet()) {
		String val = this.fixed_parameters.get(key);
		ret += "\t\t" + key + "->" + val + "\n";
	    }
	    
	    return ret;
	}
	
}