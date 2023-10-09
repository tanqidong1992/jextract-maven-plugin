package io.github.coffeelibs.maven.jextract;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static io.github.coffeelibs.maven.jextract.DumpIncludeMojo.dumpIncludes;

@SuppressWarnings({"unused", "MismatchedReadAndWriteOfArray"})
@Mojo(name = "generate-sources", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true, requiresOnline = true)
public class GenerateSourcesMojo extends AbstractMojo {

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
	 *     <dt>--target-package</dt>
	 *     <dd>target package for specified header files</dd>
	 * </dl>
	 */
	@Parameter(property = "jextract.targetPackage", required = true)
	private String targetPackage;

	/**
	 * <dl>
	 *     <dt>--header-class-name</dt>
	 *     <dd>name of the header class</dd>
	 * </dl>
	 */
	@Parameter(property = "jextract.headerClassName", required = false)
	private String headerClassName;

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
	 * <dl>
	 *     <dt>--include-function</dt>
	 *     <dd>name of function to include</dd>
	 * </dl>
	 */
	@Parameter(property = "jextract.includeFunctions", required = false)
	private String[] includeFunctions;

	/**
	 * <dl>
	 *     <dt>--include-constant</dt>
	 *     <dd>name of macro or enum constant to include</dd>
	 * </dl>
	 */
	@Parameter(property = "jextract.includeConstants", required = false)
	private String[] includeConstants;

	/**
	 * <dl>
	 *     <dt>--include-struct</dt>
	 *     <dd>name of struct definition to include</dd>
	 * </dl>
	 */
	@Parameter(property = "jextract.includeStructs", required = false)
	private String[] includeStructs;

	/**
	 * <dl>
	 *     <dt>--include-typedef</dt>
	 *     <dd>name of type definition to include</dd>
	 * </dl>
	 */
	@Parameter(property = "jextract.includeTypedefs", required = false)
	private String[] includeTypedefs;

	/**
	 * <dl>
	 *     <dt>--include-union</dt>
	 *     <dd>name of union definition to include</dd>
	 * </dl>
	 */
	@Parameter(property = "jextract.includeUnions", required = false)
	private String[] includeUnions;

	/**
	 * <dl>
	 *     <dt>--include-var</dt>
	 *     <dd>name of global variable to include</dd>
	 * </dl>
	 */
	@Parameter(property = "jextract.includeVars", required = false)
	private String[] includeVars;

	/**
	 * <dl>
	 *     <dt>--output</dt>
	 *     <dd>specify the directory to place generated files</dd>
	 * </dl>
	 */
	@Parameter(property = "jextract.outputDirectory", defaultValue = "${project.build.directory}/generated-sources/jextract", required = true)
	private File outputDirectory;

	/**
	 * working directory, which might contain <code>compile_flags.txt</code> to specify additional clang compiler options.
	 */
	@Parameter(property = "jextract.workingDirectory", defaultValue = "${project.basedir}", required = true)
	private File workingDirectory;


	public void execute() throws MojoFailureException {
		try {
			getLog().debug("Create dir " + outputDirectory);
			Files.createDirectories(outputDirectory.toPath());
		} catch (IOException e) {
			throw new MojoFailureException("Failed to create dir " + outputDirectory.getAbsolutePath(), e);
		}
		Path originIncludeFile= null;
		try {
			originIncludeFile = Files.createTempFile("jextract.includes.origin",".txt");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		dumpIncludes(executable,originIncludeFile.toFile(),headerSearchPaths,headerFile,cPreprocessorMacros,workingDirectory,getLog());
		Map<String, String[]> filters=new HashMap<>();
		filters.put(DumpIncludesParser.TYPE_FUNCTION,includeFunctions);
		filters.put(DumpIncludesParser.TYPE_CONSTANT,includeConstants);
		filters.put(DumpIncludesParser.TYPE_STRUCT,includeStructs);
		filters.put(DumpIncludesParser.TYPE_TYPEDEF,includeTypedefs);
		filters.put(DumpIncludesParser.TYPE_UNION,includeUnions);
		filters.put(DumpIncludesParser.TYPE_VAR,includeVars);
		List<DumpIncludesParser.Include> includes=DumpIncludesParser.parse(originIncludeFile.toFile(),filters);
		Path filteredIncludeFile= null;
		try {
			filteredIncludeFile = Files.createTempFile("jextract.includes.filtered",".txt");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		try {
			DumpIncludesParser.toFile(includes,filteredIncludeFile.toFile());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		List<String> args = new ArrayList<>();
		args.add(executable);
		args.add("--source");
		if (headerClassName != null) {
			args.add("--header-class-name");
			args.add(headerClassName);
		}
		args.add("--output");
		args.add(outputDirectory.getAbsolutePath());
		args.add("--target-package");
		args.add(targetPackage);
		Arrays.stream(headerSearchPaths).forEach(str -> {
			args.add("-I");
			args.add(str);
		});
		Arrays.stream(cPreprocessorMacros).forEach(str -> {
			args.add("-D");
			args.add(str);
		});
		args.add("@"+filteredIncludeFile.toFile().getAbsolutePath());
		args.add(headerFile);

		getLog().info("Running: " + String.join(" ", args));

		ProcessBuilder command = new ProcessBuilder(args);
		command.directory(workingDirectory);
		try (var stdout = new LoggingOutputStream(getLog()::info, StandardCharsets.UTF_8);
			 var stderr = new LoggingOutputStream(getLog()::warn, StandardCharsets.UTF_8)) {
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

		project.addCompileSourceRoot(outputDirectory.toString());
	}
}
