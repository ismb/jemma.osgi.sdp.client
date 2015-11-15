package it.ismb.pert.jemma.osgi.sdp.uploader.sorriso.conf;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Class storing the "uploads" configuration for future access. See uploads_configuration.json for an example of configuration
 * */
public class UploadsConfiguration {

    Map<String, ConfDevice> confdevices = new HashMap<String, ConfDevice>();

    public UploadsConfiguration(JsonObject jsonObject) {
	JsonArray jsonuploads = jsonObject.getAsJsonArray("uploads");
	for (JsonElement jsonupload : jsonuploads) {
	    ConfDevice toadd = this.parseDevice(jsonupload.getAsJsonObject());
	    String duid = jsonupload.getAsJsonObject().get("dal.device.UID").getAsString();
	    this.confdevices.put(duid, toadd);
	}
    }

    private ConfDevice parseDevice(JsonObject asJsonObject) {
	String duid = asJsonObject.get("dal.device.UID").getAsString();
	ConfDevice ret = new ConfDevice(duid);

	JsonArray jsonuploads = asJsonObject.getAsJsonArray("uploads");
	for (JsonElement jsonupload : jsonuploads) {

	    ConfUpload u = this.parseUpload(jsonupload.getAsJsonObject());
	    ret.addUpload(u);
	}

	return ret;
    }

    private ConfUpload parseUpload(JsonObject jsonupload) {
	ConfUpload ret = new ConfUpload();
	ret.functionuid = jsonupload.get("dal.function.UID").getAsString();
	ret.operation = jsonupload.get("operation").getAsString();
	ret.sdp_stream = jsonupload.get("sdp_stream").getAsString();
	JsonArray jsondalparameters = jsonupload.get("dal_parameters").getAsJsonArray();

	for (JsonElement entryjsonparams : jsondalparameters) {
	    String key = entryjsonparams.getAsJsonObject().get("dalparam").getAsString();
	    String value = entryjsonparams.getAsJsonObject().get("sdpparam").getAsString();
	    // dal param name goes as key, sdp param name goes as value
	    ret.dal_parameters.put(key, value);
	}
	
	JsonArray jsonfixedparameters = jsonupload.get("fixed_parameters").getAsJsonArray();

	for (JsonElement entryjsonparams : jsonfixedparameters) {
	    Set<Entry<String, JsonElement>> set = entryjsonparams.getAsJsonObject().entrySet();
	    for (Entry<String, JsonElement> myset: set) {
		String key = myset.getKey();
		String value = myset.getValue().getAsString();
		ret.fixed_parameters.put(key, value);
	    }
	}	

	return ret;
    }

    public String toString() {
	String ret = "";

	for (ConfDevice u : this.confdevices.values()) {
	    ret += u.toString() + "\n";
	}

	return ret;
    }

    public boolean hasConfigurationsFor(String uid) {
	return this.confdevices.containsKey(uid);
    }

    public ConfDevice getDeviceConf(String uid) {
	return this.confdevices.get(uid);
    }

}
