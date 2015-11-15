package it.ismb.pert.jemma.osgi.sdp.uploader.sorriso;

import it.ismb.pert.jemma.osgi.sdp.uploader.sorriso.conf.FileUtils;
import it.ismb.pert.jemma.osgi.sdp.uploader.sorriso.conf.SmartDataNetConfiguration;
import it.ismb.pert.jemma.osgi.sdp.uploader.sorriso.internal.DALDevicesManager;
import it.ismb.pert.jemma.osgi.sdp.uploader.sorriso.internal.DataManager;
import it.ismb.pert.jemma.osgi.sdp.uploader.sorriso.internal.SDPQueueUploader;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * OSGi activator: just starts the three threads running the real implementation..
 * */
public class Activator implements BundleActivator {

    private static final Logger LOG = LoggerFactory.getLogger(Activator.class);

    private static BundleContext context;

    private ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
    private DALDevicesManager t1;
    private DataManager t2;
    private SDPQueueUploader t3;

    static BundleContext getContext() {
	return context;
    }


    public void start(BundleContext bundleContext) throws Exception {
	Activator.context = bundleContext;

	SmartDataNetConfiguration cfg = FileUtils.getSDPConfiguration();

	executor.execute(this.t1 = new DALDevicesManager(10));
	executor.execute(this.t2 = new DataManager(cfg));
	executor.execute(this.t3 = new SDPQueueUploader(cfg));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext bundleContext) throws Exception {
	this.t1.terminate();
	this.t2.terminate();
	this.t3.terminate();
	Activator.context = null;
	LOG.info("Wait 10 seconds for closing all thread in the thread pool");
	executor.awaitTermination(10, TimeUnit.SECONDS);
    }
}
