/*
 * Copyright (c) 2014, IETR/INSA of Rennes
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *   * Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   * Neither the name of the IETR/INSA of Rennes nor the names of its
 *     contributors may be used to endorse or promote products derived from this
 *     software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package net.sf.orcc.util

import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.FileReader
import java.io.FileWriter
import java.io.InputStream
import java.io.InputStreamReader
import java.io.PrintStream
import java.io.Reader
import java.io.StringReader
import java.util.Collections
import java.util.jar.JarEntry
import java.util.jar.JarFile
import org.eclipse.core.runtime.Assert
import org.eclipse.core.runtime.FileLocator
import org.osgi.framework.FrameworkUtil

import static net.sf.orcc.util.Result.*

/**
 * Utility class to manipulate files. It brings everything needed to extract files
 * from a jar plugin to the filesystem, check if 2 files are identical, read/write
 * files, etc.
 */
class FilesManager {

	// TODO: In a future version, an classical java enum could be
	// used here. This is possible only with Xtend 2.4
	public static val OS_WINDOWS = 1
	public static val OS_LINUX = 2
	public static val OS_MACOS = 3
	public static val OS_UNKNOWN = 4

	/**
	 * <p>Copy the file or the folder at given <em>path</em> to the given
	 * <em>target folder</em>.</p>
	 * 
	 * <p>It is important to understand that the resource (file or folder) at the given path
	 * will be copiend <b>into</b> the target folder. For example,
	 * <code>extract("/path/to/file.txt", "/home/johndoe")</code> will copy <em>file.txt</em>
	 * into <em>/home/johndoe</em>, and <code>extract("/path/to/MyFolder", "/home/johndoe")</code>
	 * will create <em>MyFolder</em> directory in <em>/home/johndoe</em> and copy all files
	 * from the source folder into it.
	 * </p>
	 * 
	 * @param path The path of the source (folder or file) to copy
	 * @param targetFolder The directory where to copy the source element
	 * @return A Result object counting exactly how many files have been really
	 * 		written, and how many haven't because they were already up-to-date
	 * @throws FileNotFoundException If not resource have been found at the given path
	 */
	def static extract(String path, String targetFolder) {
		val targetF = new File(targetFolder)
		val url = path.url
		if(url == null) {
			throw new FileNotFoundException(path)
		}
		if (url.protocol.equals("jar")) {
			val splittedURL = url.file.split("!")
			val jar = new JarFile(splittedURL.head.substring(5))
			jarExtract(jar, splittedURL.last, targetF)
		} else {
			fsExtract(new File(path.url.toURI), targetF)
		}
	}

	/**
	 * Copy the given <i>source</i> file to the given <em>targetFile</em>
	 * path.
	 * 
	 * @param source
	 * 			An existing File instance
	 * @param targetFolder
	 * 			The target folder to copy the file
	 */
	private def static Result fsExtract(File source, File targetFolder) {
		if (!source.exists) {
			throw new FileNotFoundException(source.path)
		}
		val target = new File(targetFolder, source.name)
		if (source.file)
			source.fsFileExtract(target)
		else if (source.directory)
			source.fsDirectoryExtract(target)
	}

	/**
	 * Copy the file at the given <i>source</i> path to the path
	 * encapsulated in the given <em>targetFile</em>. It is called
	 * 'extract' because in most case, this method is used to extract
	 * files from a plugin in classpath to the filesystem.
	 * 
	 * @param source
	 * 			The source path of an existing file
	 * @param targetFile
	 * 			The target path of a writable file
	 */
	private def static fsFileExtract(File source, File targetFile) {
		val reader = new FileReader(source)
		if (reader.isContentEqual(targetFile)) {
			return CACHED
		}

		val writer = new FileWriter(targetFile)
		var int c
		while ((c = reader.read) != -1) {
			writer.append(c as char)
		}
		reader.close
		writer.close

		return OK
	}

	/**
	 * Copy the given <i>source</i> directory and its content into
	 * the given <em>targetFolder</em> directory.
	 * 
	 * @param source
	 * 			The source path of an existing file
	 * @param targetFolder
	 * 			Path to the folder where source will be copied
	 */
	private def static fsDirectoryExtract(File source, File targetFolder) {
		Assert.isTrue(source.directory)
		if (!targetFolder.exists)
			Assert.isTrue(targetFolder.mkdirs)
		else
			Assert.isTrue(targetFolder.directory)

		val result = EMPTY_RESULT
		for (file : source.listFiles) {
			result.merge(
				file.fsExtract(targetFolder)
			)
		}
		return result
	}

	/**
	 * Starting point for extraction of a file/folder resource from a jar. 
	 */
	private def static jarExtract(JarFile jar, String path, File targetFolder) {
		val updatedPath = if (path.startsWith("/")) {
				path.substring(1)
			} else {
				path
			}

		val entry = jar.getJarEntry(updatedPath)
		// Remove the last char if it is '/'
		val name =
			if(entry.name.endsWith("/"))
				entry.name.substring(0, entry.name.length - 1)
			else
				entry.name
		val fileName =
			if(name.lastIndexOf("/") != -1)
				name.substring(name.lastIndexOf("/"))
			else
				name

		if (entry.directory) {
			jarDirectoryExtract(jar, entry, new File(targetFolder, fileName))
		} else {
			val entries = Collections::list(jar.entries).filter[name.startsWith(updatedPath)]
			if (entries.size > 1) {
				jarDirectoryExtract(jar, entry, new File(targetFolder, fileName))
			} else {
				jarFileExtract(jar, entry, new File(targetFolder, fileName))
			}
		}
	}

	/**
	 * Extract all files in the given <em>entry</em> from the given <em>jar</em> into
	 * the <em>target folder</em>.
	 */
	private def static jarDirectoryExtract(JarFile jar, JarEntry entry, File targetFolder) {
		val prefix = entry.name
		val entries = Collections::list(jar.entries).filter[name.startsWith(prefix)]
		val result = EMPTY_RESULT
		for (e : entries) {
			result.merge(
				jarFileExtract(jar, e, new File(targetFolder, e.name.substring(prefix.length)))
			)
		}
		return result
	}

	/**
	 * Extract the file <em>entry</em> from the given <em>jar</em> into the <em>target
	 * file</em>.
	 */
	private def static jarFileExtract(JarFile jar, JarEntry entry, File targetFile) {
		targetFile.parentFile.mkdirs
		if (entry.directory) {
			targetFile.mkdir
			return EMPTY_RESULT
		}
		val is = jar.getInputStream(entry)

		if (is.isContentEqual(targetFile)) {
			return CACHED
		}

		val os = new FileOutputStream(targetFile)

		val byte[] buffer = newByteArrayOfSize(512)
		var readLen = 0
		while ((readLen = is.read(buffer)) != -1) {
			os.write(buffer, 0, readLen)
		}
		is.close
		os.close

		return OK
	}

	/**
	 * Search on the file system for a file or folder corresponding to the
	 * given path. If not found, search on the current classpath. If this method
	 * returns an URL, it always represents an existing file.
	 * 
	 * @param path
	 * 			A path
	 * @return
	 * 			An URL for an existing file, or null
	 */
	def static getUrl(String path) {
		val sanitizedPath = path.sanitize

		val file = new File(sanitizedPath)
		if (file.exists)
			return file.toURI.toURL

		// Search in all reachable bundles for the given path resource
		val bundle = FrameworkUtil::getBundle(FilesManager)
		val url =
			if(bundle != null) {
				val bundles = bundle.bundleContext.bundles
				bundles
					// Search only in Orcc plugins
					.filter[symbolicName.startsWith("net.sf.orcc")]
					// We want an URL to the resource
					.map[getEntry(path)]
					// We keep the first URL not null (we found the resource)
					.findFirst[it != null]
			}
			// Fallback, we are not in a bundle context (maybe unit tests execution?),
			// we use the default ClassLoader method. The problem with this method is
			// that it is not possible to locate resources in other jar archives (even
			// if they are in the classpath)
			else {
				FilesManager.getResource(path)
			}

		if (#["bundle", "bundleresource", "bundleentry"].contains(url?.protocol?.toLowerCase))
			FileLocator.resolve(url)
		else
			url
	}

	/**
	 * Check if given a CharSequence have exactly the same content
	 * than file b.
	 */
	static def isContentEqual(CharSequence a, File b) {
		new StringReader(a.toString).isContentEqual(b)
	}

	/**
	 * Check if given a InputStream have exactly the same content
	 * than file b.
	 */
	static def isContentEqual(InputStream a, File b) {
		new InputStreamReader(a).isContentEqual(b)
	}

	/**
	 * Check if given files have exactly the same content
	 */
	static def isContentEqual(Reader readerA, File b) {
		if(!b.exists) return false
		val readerB = new FileReader(b)

		var byteA = 0;
		var byteB = 0
		do {
			byteA = readerA.read
			byteB = readerB.read
		} while (byteA == byteB && byteA != -1)
		readerA.close
		readerB.close

		return byteA == -1
	}

	/**
	 * Write <em>content</em> to target file <em>path</em>, if necessary. This method
	 * will not write the content to the target file if this one already have exactly
	 * the same content.
	 */
	static def writeFile(CharSequence content, String path) {
		val target = new File(path)

		if (content.isContentEqual(target)) {
			return CACHED
		}

		if (!target.parentFile.exists) {
			target.parentFile.mkdirs
		}
		val ps = new PrintStream(new FileOutputStream(target))
		ps.print(content)
		ps.close
		return OK
	}

	/**
	 * Read the file at the given <em>path</em> and returns its content
	 * as a String.
	 * 
	 * @param path
	 * 			The path of the file to read
	 * @returns
	 * 			The content of the file
	 * @throws FileNotFoundException
	 * 			If the file doesn't exists
	 */
	static def readFile(String path) {
		val contentBuilder = new StringBuilder

		val url = path.url
		if(url == null) {
			throw new FileNotFoundException(path)
		}

		val reader =
			if (url.protocol.equals("jar")) {
				val splittedURL = url.file.split("!")
				val jar = new JarFile(splittedURL.head.substring(5))
				val entryPath = splittedURL.last
				val updatedPath = if (entryPath.startsWith("/")) {
						entryPath.substring(1)
					} else {
						path
					}
				val is = jar.getInputStream(jar.getEntry(updatedPath))

				new InputStreamReader(is)
			} else {
				new FileReader(new File(url.toURI))
			}

		var int c
		while ((c = reader.read) != -1) {
			contentBuilder.append(c as char)
		}

		reader.close
		contentBuilder.toString
	}

	/**
	 * Transform the given path to a valid filesystem one.
	 * 
	 * <ul>
	 * <li>It replaces first '~' by the home directory of the current user.</li>
	 * </ul>
	 */
	static def sanitize(String path) {
		// We use the following construction because Xtend infer '~' as a String instead of a char
		// path.substring(0,1).equals('~')
		if (!path.nullOrEmpty && path.substring(0, 1).equals('~')) {
			val builder = new StringBuilder(System::getProperty("user.home"))
			builder.append(File.separatorChar).append(path.substring(1))
			return builder.toString()
		}

		return path
	}

	/**
	 * <p>Detect the current user operating system, and return one of the constants
	 * <em>OS_WINDOWS, OS_LINUX, OS_MACOS, OS_UNKNOWN</em> to indicate which system
	 * has been detected.</p>
	 * 
	 * <p>TODO: this method should be tested and fixed on Mac OS</p>
	 */
	static def getCurrentOS() {
		val systemname = System.getProperty("os.name").toLowerCase()
		if (systemname.startsWith("win")) {
			OS_WINDOWS
		} else if (systemname.equals("linux")) {
			OS_LINUX
		} else if (systemname.contains("mac")) {
			OS_MACOS
		} else {
			OS_UNKNOWN
		}
	}

	/**
	 * Delete the given d directory and all its content
	 */
	static def void recursiveDelete(File d) {
		for (e : d.listFiles) {
			if (e.file) {
				e.delete
			} else if (e.directory) {
				e.recursiveDelete
			}
		}
		d.delete
	}
}
