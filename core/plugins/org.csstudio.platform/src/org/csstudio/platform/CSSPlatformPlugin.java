/* 
 * Copyright (c) 2006 Stiftung Deutsches Elektronen-Synchroton, 
 * Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY.
 *
 * THIS SOFTWARE IS PROVIDED UNDER THIS LICENSE ON AN "../AS IS" BASIS. 
 * WITHOUT WARRANTY OF ANY KIND, EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED 
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR PARTICULAR PURPOSE AND 
 * NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE 
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR 
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE. SHOULD THE SOFTWARE PROVE DEFECTIVE 
 * IN ANY RESPECT, THE USER ASSUMES THE COST OF ANY NECESSARY SERVICING, REPAIR OR 
 * CORRECTION. THIS DISCLAIMER OF WARRANTY CONSTITUTES AN ESSENTIAL PART OF THIS LICENSE. 
 * NO USE OF ANY SOFTWARE IS AUTHORIZED HEREUNDER EXCEPT UNDER THIS DISCLAIMER.
 * DESY HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, 
 * OR MODIFICATIONS.
 * THE FULL LICENSE SPECIFYING FOR THE SOFTWARE THE REDISTRIBUTION, MODIFICATION, 
 * USAGE AND OTHER RIGHTS AND OBLIGATIONS IS INCLUDED WITH THE DISTRIBUTION OF THIS 
 * PROJECT IN THE FILE LICENSE.HTML. IF THE LICENSE IS NOT INCLUDED YOU MAY FIND A COPY 
 * AT HTTP://WWW.DESY.DE/LEGAL/LICENSE.HTM
 */
package org.csstudio.platform;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Collection;

import org.csstudio.platform.internal.PluginCustomizationExporter;
import org.csstudio.platform.logging.CentralLogger;
import org.osgi.framework.BundleContext;

/**
 * The activator for the platform plugin.
 * 
 * @author Alexander Will, Sven Wende
 */
public class CSSPlatformPlugin extends AbstractCssPlugin {
	/**
	 * The shared instance of this _plugin class.
	 */
	private static CSSPlatformPlugin _plugin;

	/**
	 * This plugin's ID.
	 */
	public static final String ID = "org.csstudio.platform"; //$NON-NLS-1$

	/**
	 * Extension point ID for the <b>controlSystemItemFactories</b> extension
	 * point.
	 */
	public static final String EXTPOINT_CONTROL_SYSTEM_ITEM_FACTORIES = ID
			+ ".controlSystemItemFactories"; //$NON-NLS-1$

	/**
	 * Standard constructor.
	 */
	public CSSPlatformPlugin() {
		_plugin = this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void doStart(final BundleContext context) throws Exception {
		applySystemPropertyDefaults();
	}

	/**
	 * Applies the default values for system properties set up in the CSS
	 * preferences.
	 */
	private void applySystemPropertyDefaults() {
		Collection<SystemPropertyPreferenceEntry> properties =
			SystemPropertyPreferenceEntry.loadFromPreferences();
		for (SystemPropertyPreferenceEntry entry : properties) {
			// the preferences are for defaults, so they are applied only if
			// the system property is not already set to some other value
			if (System.getProperty(entry.getKey()) == null) {
				System.setProperty(entry.getKey(), entry.getValue());
				CentralLogger.getInstance().debug(this,
						"Setting system property: " + entry);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void doStop(final BundleContext context) throws Exception {
		// do nothing specific
	}

	/**
	 * Returns the shared instance.
	 * 
	 * @return Return the shared instance.
	 */
	public static CSSPlatformPlugin getDefault() {
		return _plugin;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final String getPluginId() {
		return ID;
	}
	
	/**
	 * Exports the current preferences into a file that can be used as a plugin
	 * customization file.  The exported file can for example be used as an
	 * argument to Eclipse's -pluginCustomization command line switch.
	 * 
	 * @param file the filename.
	 */
	public final void exportPluginCustomization(String file) {
		try {
			OutputStream os = new FileOutputStream(file);
			PluginCustomizationExporter.exportTo(os);
			os.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

}
