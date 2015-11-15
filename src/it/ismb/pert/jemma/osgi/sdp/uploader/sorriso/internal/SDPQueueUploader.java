package it.ismb.pert.jemma.osgi.sdp.uploader.sorriso.internal;

import it.ismb.pert.jemma.osgi.sdp.uploader.sorriso.conf.SmartDataNetConfiguration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * All requests are queued in this class and periodically uploaded to sdp
 */
public class SDPQueueUploader implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(SDPQueueUploader.class);

    public SDPQueueUploader(SmartDataNetConfiguration cfg) {
	this.cfg = cfg;
	queue = new LinkedBlockingQueue<String>();
    }

    SmartDataNetConfiguration cfg;
    private boolean flagCOndition;
    private static BlockingQueue<String> queue;

    @Override
    public void run() {
	this.flagCOndition = true;
	while (flagCOndition && !Thread.currentThread().isInterrupted()) {
	    try {
		if (!queue.isEmpty()) {
		    String authString = cfg.user.trim() + ":" + cfg.password.trim();
		    byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
		    String authStringEnc = new String(authEncBytes);

		    try {
			URL uri = new URL(cfg.url);
			URLConnection connection = uri.openConnection();
			connection.setDoOutput(true);
			connection.setRequestProperty("Authorization", "Basic " + authStringEnc);
			connection.setRequestProperty("Content-Type", "application/json");
			String tosend = queue.take().toString();
			LOG.debug("popping from queue: " + tosend);
			OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
			wr.write(tosend);
			wr.flush();
			BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String line;
			while ((line = rd.readLine()) != null) {
			    LOG.debug(line);
			}
			wr.close();
			rd.close();
			Thread.sleep(250);
		    } catch (Exception e) {
			// F**k for some reason it fails. I don't know if it's a internet problem or an other correlated to LinkedQueueBlocked.
			// Anyway: Houston, we have a problem
			LOG.warn("Exception", e);
		    }
		} else {
		    // Queue is empty try again after 30s
		    try {
			Thread.sleep(30000);
		    } catch (Exception e) {
			LOG.warn("Exception", e);
		    }
		}
	    } catch (Exception e) {
		flagCOndition = false;
	    }
	}
    }

    public static void pushData(String data) {
	LOG.debug("SDPLoader is queuing: " + data);
	try {
	    queue.put(data);
	} catch (Exception e) {
	    LOG.error("Exception", e);
	}
    }

    public void terminate() {
	this.flagCOndition = false;

    }

}
