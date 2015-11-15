package it.ismb.pert.jemma.osgi.sdp.uploader.sorriso.internal;

import it.ismb.pert.jemma.osgi.sdp.uploader.sorriso.conf.ConfDevice;
import it.ismb.pert.jemma.osgi.sdp.uploader.sorriso.conf.ConfUpload;
import it.ismb.pert.jemma.osgi.sdp.uploader.sorriso.conf.FileUtils;
import it.ismb.pert.jemma.osgi.sdp.uploader.sorriso.conf.SmartDataNetConfiguration;
import it.ismb.pert.jemma.osgi.sdp.uploader.sorriso.conf.UploadsConfiguration;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.dal.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * This is the brain of the component: it matches available DAL devices with configuration and querty/uploads data, if any
 * */
public class DataManager implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(DataManager.class);

    // RT this clss copies locally the SmartDataNetConfiguration cfg
    public DataManager(SmartDataNetConfiguration cfg) throws IOException {
	this.application_name = cfg.application;
	this.refreshTime = cfg.refreshTime;
	this.url = cfg.url;
	this.user = cfg.user;
	this.password = cfg.password;
	// RT are this threads used for data access ?
	executor = Executors.newFixedThreadPool(4);
    }

    private String application_name;
    private int refreshTime;
    private String url;
    String user;
    String password;
    ExecutorService executor;
    private UploadsConfiguration uploads_configurations;
    private boolean flagCondition;

    private void init() {
	FileUtils f = new FileUtils();
	try {
	    this.uploads_configurations = f.load_uploads_configuration();
	} catch (IOException e) {
	    LOG.warn("IOException", e);
	}

    }

    @Override
    public void run() {

	// RT it seems this thread dies on exceptions.

	this.init();

	this.flagCondition = true;

	while (flagCondition && !Thread.currentThread().isInterrupted()) {

	    LOG.debug("#########  DataDumper starting new cycle ######### ");

	    List<Map<String, Object>> devices = DALDevicesManager.getDevices();

	    for (Map devicemap : devices) {
		String uid = devicemap.get("dal.device.UID").toString();
		LOG.debug("considering device: " + uid);
		if (this.uploads_configurations.hasConfigurationsFor(uid)) {
		    LOG.debug("we have upload configurations for device: " + uid);
		    this.serveDevice(this.uploads_configurations.getDeviceConf(uid));
		}
	    }

	    try {
		LOG.debug("#########  DataDumper closing cycle ######### ");
		Thread.sleep(refreshTime * 1000);
	    } catch (InterruptedException e) {
		LOG.warn("InterruptedException", e);
	    }

	}

    }

    private void serveDevice(ConfDevice deviceConf) {

	// we iterate for each upload entry in the configuration
	for (ConfUpload up : deviceConf.uploads) {
	    LOG.debug("calling " + deviceConf.deviceuid + ":" + up.functionuid + " " + up.operation);
	    Object dalresult = null;
	    dalresult = this.executeMethod(up.operation, deviceConf.deviceuid + ":" + up.functionuid);

	    if (dalresult != null) {
		Gson gson = new Gson();
		String jsonstring = gson.toJson(dalresult);
		LOG.debug("result OK:" + jsonstring);
		JsonObject jsonresult = new JsonParser().parse(jsonstring).getAsJsonObject();
		// valid result here: {"level":4.0000,"unit":"W","timestamp":1447582960910}
		this.publishData(up, jsonresult);

	    }

	}

    }

    private void publishData(ConfUpload up, JsonObject jsonresult) {
	LOG.debug("#### About to queue data");
	LOG.debug("ConfUpload");
	LOG.debug(up.toString());
	LOG.debug("JsonObject");
	LOG.debug(jsonresult.toString());

	JsonObject ret = new JsonObject();

	String stream = up.sdp_stream;
	String sensor = this.application_name;
	String myttimestamp = this.nowDate();

	ret.addProperty("stream", stream);
	ret.addProperty("sensor", sensor);

	JsonObject values = new JsonObject();
	JsonObject components = new JsonObject();

	for (Entry<String, JsonElement> a : jsonresult.entrySet()) {
	    String dalkey = a.getKey();
	    String dalvalue = a.getValue().getAsString();
	    String sdpkey = "";

	    if (up.dal_parameters.containsKey(dalkey)) {
		sdpkey = up.dal_parameters.get(dalkey);
		
		//XXX Hack: converting all values to integer to avoid compatibility problems (this should be improved - it has just been done because SDP does not accept float values if the stream is integer)
		int mydalvalue = (int) Float.parseFloat(dalvalue);
		components.addProperty(sdpkey, mydalvalue + "");
		
	    } else {
		LOG.debug("Not adding parameter [dalkey="+dalkey+"] because not in configuration");
	    }
	}
	
	//adding fixed parameters
	for ( String key : up.fixed_parameters.keySet()) {
	    String value = up.fixed_parameters.get(key);
	    components.addProperty(key, value);
	}

	values.addProperty("time", myttimestamp);
	values.add("components", (JsonElement) components);

	ret.add("values", (JsonElement) values);

	LOG.debug("About to push data:");
	LOG.debug(ret.toString());

	SDPQueueUploader.pushData(String.valueOf(ret));

    }

    private Object executeMethod(String method, String functionUID) {

	String filterString = null;
	try {
	    filterString = "(" + Function.SERVICE_UID + "=" + URLDecoder.decode(functionUID, "UTF-8") + ")";
	} catch (UnsupportedEncodingException e1) {
	    LOG.warn("executeMethod failed");
	}

	// Getting BundleContext
	BundleContext ctx = FrameworkUtil.getBundle(DataManager.class).getBundleContext();

	if (ctx == null) {
	    // Error getting bundlecontext
	    LOG.warn("executeMethod failed");
	    return null;
	}

	ServiceReference[] functionReferences = null;
	try {
	    functionReferences = (ServiceReference[]) ctx.getServiceReferences(Function.class.getName(), filterString);
	} catch (InvalidSyntaxException e1) {
	    LOG.warn("executeMethod failed");
	}

	if (null == functionReferences || functionReferences.length == 0) {
	    LOG.warn("No service reference found with function UID: {}", functionUID);
	    return null;
	} else {
	    LOG.debug("function found");
	}
	Object f = ctx.getService(functionReferences[0]);
	Class<? extends Object> clazz = f.getClass();
	Object o = null;
	Method meth = null;
	try {
	    // TODO: find a method for using method with paramters
	    Class[] ptypes = null;
	    meth = clazz.getDeclaredMethod(method, ptypes);
	    if (meth != null) {
		Object[] paramValues = null;
		o = meth.invoke(clazz.cast(f), paramValues);
		return o;
	    } else {
		LOG.warn("executeMethod failed");
	    }
	} catch (Exception e) {
	    LOG.warn("executeMethod failed");
	}
	return null;
    }

    private String nowDate() {
	SimpleDateFormat sdf_data = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
	return sdf_data.format(new Date());
    }

    public void terminate() {
	this.flagCondition = false;

    }
}
