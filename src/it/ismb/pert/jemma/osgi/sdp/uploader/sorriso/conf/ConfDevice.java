package it.ismb.pert.jemma.osgi.sdp.uploader.sorriso.conf;

import java.util.LinkedList;
import java.util.List;


/**
 * Simple clas holding configuration of a single device
 * */
public class ConfDevice {
	
	public String deviceuid;
	public List<ConfUpload> uploads;

	public ConfDevice(String deviceuid) {
	    this.deviceuid=deviceuid;
	    this.uploads = new LinkedList<ConfUpload>();
	}

	public String  toString(){
	    String ret="";
	    ret+= "[deviceuid=" + this.deviceuid + "]\n";
	    for (ConfUpload u: this.uploads) {
		ret += "\t" + u.toString() + "\n";
	    }
	    return ret;
	}

	public void addUpload(ConfUpload u) {
	    this.uploads.add(u);
	}
	
}
