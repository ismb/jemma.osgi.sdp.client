package it.ismb.pert.jemma.osgi.sdp.uploader.sorriso.internal;

import java.io.IOException;

import it.ismb.pert.jemma.osgi.sdp.uploader.sorriso.conf.FileUtils;
import it.ismb.pert.jemma.osgi.sdp.uploader.sorriso.conf.UploadsConfiguration;

public class TestMain {

    public static void main(String[] args) {
	FileUtils f = new FileUtils();
	try {
	    UploadsConfiguration a = f.load_uploads_configuration("/configuration-example/uploads_configuration.json");
	    System.out.println(a);
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	
	
    }

}
