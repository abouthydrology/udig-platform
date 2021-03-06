/*
 *    uDig - User Friendly Desktop Internet GIS client
 *    http://udig.refractions.net
 *    (C) 2012, Refractions Research Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the Refractions BSD
 * License v1.0 (http://udig.refractions.net/files/bsd3-v10.html).
 */
package net.refractions.udig.catalog.tests.ui;

import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.URL;

import net.refractions.udig.catalog.IService;
import net.refractions.udig.catalog.internal.wms.WMSServiceImpl;

public class WMSCatalogImportTest extends CatalogImportTest {

	@Override
	Object getContext() throws MalformedURLException {
		return new URL("http://demo.opengeo.org/geoserver/wms?Service=WMS&Version=1.1.1&Request=GetCapabilities"); //$NON-NLS-1$
	}
	
	@Override
	void assertServiceType(IService service) {
		assertTrue(service instanceof WMSServiceImpl);
	}
}

