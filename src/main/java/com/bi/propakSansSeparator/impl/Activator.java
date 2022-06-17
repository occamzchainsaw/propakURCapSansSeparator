package com.bi.propakSansSeparator.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.bi.propakSansSeparator.pickups.PickUpProgramNodeService;
import com.ur.urcap.api.contribution.program.swing.SwingProgramNodeService;

/**
 * Hello world activator for the OSGi bundle URCAPS contribution
 *
 */
public class Activator implements BundleActivator {
	@Override
	public void start(BundleContext bundleContext) throws Exception {
		//System.out.println("Activator for ProPak Node Starting");
		bundleContext.registerService(SwingProgramNodeService.class, new ProPakProgramNodeService(), null);
		bundleContext.registerService(SwingProgramNodeService.class, new PickUpProgramNodeService(), null);
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		//System.out.println("Activator says Goodbye World!");
	}
}

