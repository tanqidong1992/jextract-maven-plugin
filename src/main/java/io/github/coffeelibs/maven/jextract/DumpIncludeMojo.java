package io.github.coffeelibs.maven.jextract;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings({"unused", "MismatchedReadAndWriteOfArray"})
@Mojo(name = "dump-includes", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true, requiresOnline = true)
public class DumpIncludeMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}")
	private MavenProject project;

	/**
	 * Path to the <code>jextract</code> binary.
	 */
	@Parameter(property = "jextract.executable", required = true)
	private String executable;

	/**
	 * <dl>
	 *     <dt>--include-macro</dt>
	 *     <dd>name of constant macro to include</dd>
	 * </dl>
	 */
	@Parameter(property = "jextract.headerFile", required = true)
	private String headerFile;

	/**
	 * <dl>
	 *     <dt>-I</dt>
	 *     <dd>specify include files path</dd>
	 * </dl>
	 */
	@Parameter(property = "jextract.headerSearchPaths", required = false)
	private String[] headerSearchPaths;

	/**
	 * <dl>
	 *     <dt>-D</dt>
	 *     <dd>C preprocessor macro</dd>
	 * </dl>
	 */
	@Parameter(property = "jextract.cPreprocessorMacros", required = false)
	private String[] cPreprocessorMacros;

	/**
	 * working directory, which might contain <code>compile_flags.txt</code> to specify additional clang compiler options.
	 */
	@Parameter(property = "jextract.workingDirectory", defaultValue = "${project.basedir}", required = true)
	private File workingDirectory;


	@Parameter(property = "jextract.dumpIncludes", defaultValue = "${project.build.directory}/jextract.includes.txt", required = true)
	private File jextractIncludes;

	public void execute() throws MojoFailureException {

		dumpIncludes(executable,jextractIncludes,headerSearchPaths,headerFile,cPreprocessorMacros,workingDirectory,getLog());

	}

	public static void dumpIncludes(String executable,
									File jextractIncludes,
									String[] headerSearchPaths,
									String headerFile,
									String[] cPreprocessorMacros,
									File workingDirectory,Log log) throws MojoFailureException {

		List<String> args = new ArrayList<>();
		args.add(executable);
		args.add("--dump-includes");
		args.add(jextractIncludes.getAbsolutePath());
		Arrays.stream(headerSearchPaths).forEach(str -> {
			args.add("-I");
			args.add(str);
		});
		Arrays.stream(cPreprocessorMacros).forEach(str -> {
			args.add("-D");
			args.add(str);
		});
		args.add(headerFile);

		log.info("Running: " + String.join(" ", args));

		ProcessBuilder command = new ProcessBuilder(args);
		command.directory(workingDirectory);
		try (var stdout = new LoggingOutputStream(log::info, StandardCharsets.UTF_8);
			 var stderr = new LoggingOutputStream(log::warn, StandardCharsets.UTF_8)) {
			Process process = command.start();
			process.getInputStream().transferTo(stdout);
			process.getErrorStream().transferTo(stderr);
			int result = process.waitFor();
			if (result != 0) {
				throw new MojoFailureException("jextract returned error code " + result);
			}
		} catch (IOException e) {
			throw new MojoFailureException("Invoking jextract failed", e);
		} catch (InterruptedException e) {
			throw new MojoFailureException("Interrupted while waiting for jextract", e);
		}
	}
}
