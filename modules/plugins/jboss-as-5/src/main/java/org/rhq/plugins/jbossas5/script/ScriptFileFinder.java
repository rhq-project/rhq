package org.rhq.plugins.jbossas5.script;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.rhq.core.system.SystemInfo;

/**
 * A class that finds all script files beneath a specified set of directories on
 * the local filesystem. The code will follow symbolic links on *NUX platforms
 * but protects against circularity.
 * 
 * @author Ian Springer
 * @author Jay Shaughnessy
 */
public class ScriptFileFinder {
	private final Log log = LogFactory.getLog(this.getClass());

	private SystemInfo systemInfo;
	private List<File> scanDirectories;

	public ScriptFileFinder(SystemInfo systemInfo, File... scanDirectories) {
		this.scanDirectories = Arrays.asList(scanDirectories);
		this.systemInfo = systemInfo;
	}

	protected List<File> getScanDirectories() {
		return this.scanDirectories;
	}

	@NotNull
	public List<File> findScriptFiles() {
		List<File> scriptFiles = new ArrayList<File>();

		for (File scanDir : this.scanDirectories) {
			if (!scanDir.isAbsolute()) {
				log.warn("The specified scan directory (" + scanDir
						+ ") is not absolute.");
				continue;
			}

			if (!scanDir.exists()) {
				log.warn("The specified scan directory (" + scanDir
						+ ") does not exist.");
				continue;
			}

			if (!scanDir.isDirectory()) {
				log.warn("The specified scan directory (" + scanDir
						+ ") is not a directory.");
				continue;
			}

			findScriptFiles(scanDir, new ArrayList<File>(), scriptFiles);
		}

		return scriptFiles;
	}

	protected void findScriptFiles(File scanDir, List<File> excludeDirs,
			List<File> scriptFiles) {
		if (!scanDir.isDirectory()) {
			return;
		}

		File canonicalScanDir = null;

		try {
			canonicalScanDir = scanDir.getCanonicalFile();
		} catch (IOException e) {
			// if we aren't allowed to reach the canonical path, skip this
			// directory
			return;
		}

		// if we've seen this directory before (or it's explicitly excluded by
		// the caller) skip this directory.
		// this protects against following cyclic symbolic links.
		if (excludeDirs.contains(canonicalScanDir)) {
			return;
		}

		// add to the directories we've seen
		excludeDirs.add(canonicalScanDir);

		scriptFiles.addAll(Arrays.asList(scanDir
				.listFiles(new ScriptFileFilter())));

		File[] subDirs = scanDir.listFiles(new DirectoryFilter());
		for (File subDir : subDirs) {
			findScriptFiles(subDir, excludeDirs, scriptFiles); // recurse
		}
	}

	protected class ScriptFileFilter implements FileFilter {
		public boolean accept(File file) {
			if (file.isDirectory()) {
				return false;
			}

			String path = file.getPath();
			int dotIndex = path.lastIndexOf(".");
			String extension = (dotIndex != -1) ? path.substring(dotIndex + 1)
					: "";
			switch (ScriptFileFinder.this.systemInfo.getOperatingSystemType()) {
			case WINDOWS: {
				for (String scriptFileExtension : getWindowsScriptFileExtensions()) {
					if (extension.equalsIgnoreCase(scriptFileExtension)) // ignore
					// case
					// on
					// Windows
					{
						return true;
					}
				}

				break;
			}

			default: // UNIX
			{
				for (String scriptFileExtension : getUnixScriptFileExtensions()) {
					if (extension.equals(scriptFileExtension)) {
						return true;
					}
				}
			}
			}

			return false;
		}

		protected List<String> getWindowsScriptFileExtensions() {
			return Arrays.asList("bat", "cmd");
		}

		protected List<String> getUnixScriptFileExtensions() {
			return Arrays.asList("sh", "pl");
		}
	}

	protected static class DirectoryFilter implements FileFilter {
		public boolean accept(File file) {
			return (file.isDirectory());
		}
	}
}
