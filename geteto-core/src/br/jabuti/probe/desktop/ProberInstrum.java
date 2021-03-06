/*  Copyright 2003  Auri Marcelo Rizzo Vicenzi, Marcio Eduardo Delamaro, 			    Jose Carlos Maldonado

    This file is part of Jabuti.

    Jabuti is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as 
    published by the Free Software Foundation, either version 3 of the      
    License, or (at your option) any later version.

    Jabuti is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with Jabuti.  If not, see <http://www.gnu.org/licenses/>.
 */

package br.jabuti.probe.desktop;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipFile;

import org.aspectj.apache.bcel.classfile.JavaClass;
import org.aspectj.apache.bcel.classfile.Method;
import org.aspectj.apache.bcel.generic.ClassGen;
import org.aspectj.apache.bcel.generic.ConstantPoolGen;
import org.aspectj.apache.bcel.generic.InstructionHandle;
import org.aspectj.apache.bcel.generic.InstructionList;
import org.aspectj.apache.bcel.generic.MethodGen;
import org.aspectj.apache.bcel.util.ClassPath;

import br.jabuti.graph.datastructure.dug.CFG;
import br.jabuti.instrumenter.bytecode.bcel.ASMInstrumenter;
import br.jabuti.lookup.java.bytecode.Program;
import br.jabuti.project.JabutiProject;
import br.jabuti.util.ToolConstants;

/**
 * This is the class that implements the functionality of a JVM code
 * instrumenter. Using such object it is possible to insert JVM code in a given
 * JVM method.
 * 
 * @version: 0.00001
 * @author: Marcio Eduardo Delamaro
 */

public class ProberInstrum {
	// Classes da Jabuti adicionadas no jar
	static final private String[] ProberClasses = new String[] {
			"br.jabuti.probe.ProbedNode", "br.jabuti.probe.DefaultProber",
			"br.jabuti.probe.DefaultProberHook" };

	public static void usage() {
		System.out.println(ToolConstants.toolName + " v. "
				+ ToolConstants.toolVersion);
		System.out.println("\nProberInstrum usage:");
		System.out.println("-------------------\n");
		System.out
				.println("java probe.ProberInstrum [-d <DIR>] -p <PROJECT_NAME> | -jar <FILE_NAME> [-avoid \"package list\"] -o JAR_FILE_NAME\n");
		System.out
				.println("      [-d <DIR>]              Optional parameter. Specify the directory where the project");
		System.out
				.println("                              is located. If not specified, the current directory is assumed.");
		System.out
				.println("      -p <PROJECT_NAME>       Specify the name of the project to be used. The");
		System.out
				.println("                              project must be a valid project file (.jba) generated by");
		System.out
		        .println("      -avoid                  Only used with -jar option. It is null by default. Defines the list of packages that should not be considered during instrumentation. Each name is separeted by a space.");		
		System.out
				.println("      -jar <JAR_FILE_NAME>    Specifies the jar file containing the set of classes to be instrumented.");
		System.out
				.println("      -o <filename>           Specifies the name of the file to write the instrumented classes");
		System.out.println("\nCopyright (c) 2002-2005\n");
	}

	public static void main(String args[]) throws Throwable {
		String workDir = null;
		String projectName = null;
		JabutiProject project = null;
		String outName = null;
		String jarFile = null;
		String toAvoid = null;

		if (args.length > 0) {

			int i = 0;

			while ((i < args.length) && (args[i].startsWith("-"))) {
				// -CP: Class path
				if (("-d".equals(args[i])) && (i < args.length - 1)) {
					i++;
					workDir = args[i];
				} // -P: project name
				else if (("-p".equals(args[i])) && (i < args.length - 1)) {
					i++;
					projectName = args[i];
				} else if (("-o".equals(args[i])) && (i < args.length - 1)) {
					i++;
					outName = args[i];
				} else if (("-jar".equals(args[i])) && (i < args.length - 1)) {
					i++;
					jarFile = args[i];
				} else if (("-avoid".equals(args[i])) && (i < args.length - 1)) {
					i++;
					toAvoid = args[i];
				} else {
					System.out
							.println("Error: Unrecognized option: " + args[i]);
					System.exit(0);
				}
				i++;
			}

			if (jarFile == null) {
				// If not using jar file, and project name is mandatory
				if ((projectName == null) || outName == null) {
					System.out.println("Error: Missing parameter!!!");
					usage();
					System.exit(0);
				}

				// Creating the absolute path to a given project
				String absoluteName = null;

				if (workDir != null) {
					absoluteName = workDir + File.separator + projectName;
				} else {
					absoluteName = projectName;
				}

				try {
					File theFile = new File(absoluteName);

					if (!theFile.isFile()) // verifica se existe
					{
						System.out.println("File " + theFile.getName()
								+ " not found");
						System.exit(0);
					}

					project = JabutiProject.reloadProj(theFile.toString(),
							false);
				} catch (Exception e) {
					ToolConstants.reportException(e, ToolConstants.STDERR);
					System.exit(0);
				}

				ProberInstrum.instrumentProject(project, outName);

			} else {
				// Checking if all essential parameters are not null
				if (outName == null) {
					System.out.println("Error: Missing parameter!!!");
					usage();
					System.exit(0);
				}

				try {
					File theFile = new File(jarFile);

					if (!theFile.isFile()) // verifica se existe
					{
						System.out.println("File " + theFile.getName()
								+ " not found");
						System.exit(0);
					}
				} catch (Exception e) {
					ToolConstants.reportException(e, ToolConstants.STDERR);
					System.exit(0);
				}

				Program program = new Program(new ZipFile(jarFile), true,
						toAvoid);
				String[] classes = program.getClasses();
				HashSet hs = new HashSet();
				for (i = 0; i < classes.length; i++) {
					hs.add(classes[i]);
				}

				ProberInstrum.instrumentProgram(program, hs, outName);
			}
		} else {
			usage();
		}
	}

	public static boolean instrumentProject(JabutiProject project, String outName) {
		Program program = project.getProgram();
		HashSet toInstrumenter = project.getInstr();

		return instrumentProgram(program, toInstrumenter, outName, project.getTraceFileName());
	}

	/**
	 * Para ser chamado quando nao ha main no programa ou no modo batch em que o
	 * main nao deve ser instrumentado
	 * 
	 * @param program
	 * @param toInstrumenter
	 * @param CFGOption
	 * @return
	 */
	public static boolean instrumentProgram(Program program, HashSet toInstrumenter, String outName) {
		return instrumentProgram(program, toInstrumenter, outName, null);
	}

	/**
	 * 
	 * @param program
	 *            - classes that are part of the program
	 * @param toInstrumenter
	 *            - list of classes to instrument. null == all
	 * @param CFGOption
	 *            - type of CFG used
	 * @param outName
	 *            - name of the output file
	 * @param traceFileName
	 *            - name of the trace file, in case there is a main
	 * @return OK?
	 */
	public static boolean instrumentProgram(Program program, HashSet toInstrumenter, String outName, String traceFileName) {

		DefaultProbeInsert dpi = new DefaultProbeInsert(program, toInstrumenter);
		Map mp = null;

		// esta com pau aqui.... Precisa ignorar as classes com
		// ClassFormatException geradas pelo BCEL.
		// Essas classes devem ser excluidas da instrumenta��o mas incluidas no
		// jar sem serem instrumentadas.

		try {
			mp = dpi.instrument();
			// substitui os objetos JavaClass por byte[]

			Iterator it0 = mp.keySet().iterator();
			Hashtable ht = new Hashtable();

			while (it0.hasNext()) {
				String clName = (String) it0.next();
				JavaClass jv = (JavaClass) mp.get(clName);
				ht.put(clName, jv.getBytes());
			}

			mp = ht;

			File outFile = new File(outName);
			FileOutputStream fos = new FileOutputStream(outFile);
			JarOutputStream outJar = new JarOutputStream(fos);
			Iterator it = mp.keySet().iterator();
			while (it.hasNext()) {
				String clName = (String) it.next();
				byte[] b = (byte[]) mp.get(clName);
				clName = clName.replace('.', '/');
				JarEntry jarEntry = new JarEntry(clName + ".class");
				outJar.putNextEntry(jarEntry);
				outJar.write(b);
			}
			ClassPath cp = new ClassPath(System.getProperty("java.class.path"));
			for (int z1 = 0; z1 < ProberClasses.length; z1++) {
				String clName = ProberClasses[z1];
				byte[] b = cp.getBytes(clName, ".class");
				clName = clName.replace('.', '/');
				JarEntry jarEntry = new JarEntry(clName + ".class");
				outJar.putNextEntry(jarEntry);
				outJar.write(b);
			}
			outJar.close();
		} catch (Exception eu) {
			eu.printStackTrace();
			return false;
		}
		return true;
	}
}
