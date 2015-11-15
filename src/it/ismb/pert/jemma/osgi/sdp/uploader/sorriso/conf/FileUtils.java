package it.ismb.pert.jemma.osgi.sdp.uploader.sorriso.conf;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;


/**
 * Misc classes used to load configurations
 * */
public class FileUtils {

    private static final Logger LOG = LoggerFactory.getLogger(FileUtils.class);

    public static SmartDataNetConfiguration getSDPConfiguration() throws Exception {

	SmartDataNetConfiguration cfg = new SmartDataNetConfiguration();
	String path = new File("").getAbsolutePath();
	path = path + "/Data_Configuration/SmartDataNet.ini";
	InputStream input = new FileInputStream(path);
	Properties props = new Properties();
	props.load(input);
	if (props.get("user") != null)
	    cfg.user = props.get("user").toString();
	if (props.get("password") != null)
	    cfg.password = props.get("password").toString();
	if (props.get("applicationname") != null)
	    cfg.application = props.get("applicationname").toString();
	if (props.get("refreshtime") != null)
	    cfg.refreshTime = Integer.valueOf(props.get("refreshtime").toString());
	if (props.get("url") != null)
	    cfg.url = props.getProperty("url").toString();
	return cfg;
    }
    
    public UploadsConfiguration load_uploads_configuration() throws IOException {
	return this.load_uploads_configuration("/Data_Configuration/uploads_configuration.json");
    }

    public UploadsConfiguration load_uploads_configuration(String filename) throws IOException {
	Gson gson = new Gson();
	String path = new File("").getAbsolutePath();
	path = path + filename;
	File file = new File(path);
	FileInputStream fis = new FileInputStream(file);
	byte[] data = new byte[(int) file.length()];
	fis.read(data);
	fis.close();
	String json = new String(data, "UTF-8");
	JsonElement uploads = new JsonParser().parse(json);
	UploadsConfiguration conf = new UploadsConfiguration(uploads.getAsJsonObject());

	return conf;
    }
}
