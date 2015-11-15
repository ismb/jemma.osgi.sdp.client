package it.ismb.pert.jemma.osgi.sdp.uploader.sorriso.internal;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.dal.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Periodically reads the DAL and get available devices and stores their identifiers
 * */
public class DALDevicesManager implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(DALDevicesManager.class);

    public DALDevicesManager(int refreshTime) {
	devices = new LinkedList<Map<String, Object>>();
	this.refreshTime = refreshTime * 1000;
    }

    private static List<Map<String, Object>> devices;

    private int refreshTime;

    private boolean flagCondition;

    @Override
    public void run() {

	// RT: this to kill the thread in case of exception
	this.flagCondition = true;

	while (flagCondition && !Thread.currentThread().isInterrupted()) {
	    LOG.debug("RunnableDevicesResources starting new cycle");
	    try {
		BundleContext ctx = FrameworkUtil.getBundle(DALDevicesManager.class).getBundleContext();
		ServiceReference[] deviceReferences = null;
		try {
		    // RT it seems that this gets a reference to all DAL devices endpoints, if any - otherwise, we sleep.
		    deviceReferences = (ServiceReference[]) ctx.getServiceReferences(Device.class.getName(), null);
		    if (deviceReferences == null) {
			LOG.debug("No device found retry after: " + this.refreshTime);
			Thread.sleep(this.refreshTime);
		    } else {

			List<Map<String, Object>> tempDevices = new LinkedList<Map<String, Object>>();

			for (ServiceReference deviceReference : deviceReferences) {

			    LOG.debug("RunnableDevicesResources considering device: " + deviceReference.toString());
			    Map<String, Object> propMap = new HashMap<String, Object>();

			    // RT seemingly we iterate on all device properties.
			    // XXX RT why cannot be use iterator instead of using the index ?
			    for (int j = 0; j < deviceReference.getPropertyKeys().length; j++) {

				// RT all property keys except "objectClass" are considered
				if (!deviceReference.getPropertyKeys()[j].equals("objectClass")) {

				    // RT seemingly this adds all properties of the device to the local propMap
				    propMap.put(deviceReference.getPropertyKeys()[j], deviceReference.getProperty(deviceReference.getPropertyKeys()[j]));

				    // XXX RT I dont's understand this "ungetService"
				    ctx.ungetService(deviceReference);

				    LOG.debug("\t\t [k=" + deviceReference.getPropertyKeys()[j] + "] [v=" + deviceReference.getProperty(deviceReference.getPropertyKeys()[j]) + "]");
				}
			    }

			    // RT the map is added to the overall list
			    tempDevices.add(propMap);
			}

			synchronized (devices) {
			    this.devices = tempDevices;
			}

			// every this.refreshtime milliseconds: in case some new device appears, it will be re-loaded at the next refresh
			Thread.sleep(refreshTime);

		    }
		} catch (InvalidSyntaxException e) {
		    LOG.warn("InvalidSyntaxException", e);
		} catch (InterruptedException e) {
		    LOG.warn("InterruptedException", e);
		}
	    } catch (Exception e) {
		flagCondition = false;
	    }
	}
    }

    public static List<Map<String, Object>> getDevices() {

	// update avery this.refreshtime milliseconds, always contains the list of key-value of all properties for all devices registered to DAL
	synchronized (devices) {
	    return devices;
	}
    }

    public void terminate() {
	this.flagCondition = false;

    }

}
