/*
 * Copyright (c) 2018 OpenFTC Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.openftc;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import net.lingala.zip4j.core.ZipFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;

public class Main
{
    @Parameter(names = "-h", help = true, description = "Print help")
    private boolean help;

    /*@Parameter(names = {"-i", "--input"}, description = "AAR/JAR input file", required = true)
    private String inputFilepath;

    @Parameter(names = {"-s", "--sources"}, description = "Sources JAR file (optional)", required = false)
    private String sourcesFilepath;

    @Parameter(names = {"-o", "--output"}, description = "ZIP Output file", required = true)
    private String outputFilepath;

    @Parameter(names = {"-g", "--group"}, description = "Group name", required = true)
    private String groupName;

    @Parameter(names = {"-a", "--artifact"}, description = "Artifact name", required = true)
    private String artifactName;

    @Parameter(names = {"-v", "--version"}, description = "Artifact version", required = true)
    private String artifactVersion;*/

    @Parameter(names = {"-m", "--merge-dir"}, description = "SDK Directory", required = false)
    private String mergeDir;

    @Parameter(names = {"-s", "--stock-dir"}, description = "SDK Directory", required = true)
    private String stockDir;

    String[] moduleNames = {
            "Blocks",
            "FtcCommon",
            "Hardware",
            "Inspection",
            "OnBotJava",
            "RobotCore",
            "RobotServer"};

    private boolean waitingForFailOrOk = false;
    private static final String TEMP_FOLDER_NAME = "tempMergeFolder";

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException
    {
        System.out.println("ExtractedRC_Updater v1.0");

        Main instance = new Main();

        JCommander jCommander =
                JCommander.newBuilder()
                        .addObject(instance)
                        .programName("java -jar AAR_Repackager.jar")
                        .build();

        try
        {
            jCommander.parse(args);

            if (instance.help)
            {
                System.out.println("AAR/JAR Repackager v1.0");
                jCommander.usage();
            }
            else
            {
                instance.run();
            }
        }
        catch (ParameterException e)
        {
            System.err.println(e.getMessage() + "\nRun with -h for usage details");
        }
    }


    private void run() throws IOException, NoSuchAlgorithmException
    {
        ensureWeAreInExtractedRcRepo();
        checkThatStockSdkHasAars();
        prepareTempDir();
        extractAarsAndSourceJars();
        copyModuleSources();
        copyModuleResources();
        //reformatModulesToLf();
    }

    private void copyModuleSources()
    {
        for (String s : moduleNames)
        {
            deleteOldSourceForModule(s);
        }

        for(String s : moduleNames)
        {
            copySourceForModule(s);
        }
    }

    private void copyModuleResources()
    {
        for(String s : moduleNames)
        {
            deleteOldResourcesForModule(s);
        }

        for(String s : moduleNames)
        {
            copyNewResourcesForModule(s);
        }
    }

    private void reformatModulesToLf()
    {
        for(String s : moduleNames)
        {
            reformatModuleToLf(s);
        }
    }

    private void copyNewResourcesForModule(String moduleName)
    {
        stepMsg("Copying new resources for module '" + moduleName + "'");

        try
        {
            File newResourcesDir = new File(mergeDir + File.separator + TEMP_FOLDER_NAME + File.separator + moduleName + "-aar" + File.separator + "res");

            for(File f : newResourcesDir.listFiles())
            {
                String outDir = mergeDir + File.separator + moduleName + File.separator + "src" + File.separator + "main" + File.separator + "res" + File.separator + f.getName();
                recursiveCopyDir(f.getAbsolutePath(), outDir);
            }
            ok();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail();
        }
    }

    private void deleteOldResourcesForModule(String moduleName)
    {
        stepMsg("Deleting resources for module '" + moduleName + "'");

        try
        {
            deleteAllThingsInFolder(new File(mergeDir + File.separator + moduleName + File.separator + "src" + File.separator + "main" + File.separator + "res"));
            ok();
        }
        catch (Exception e)
        {
            fail();
        }
    }

    private void deleteOldSourceForModule(String moduleName)
    {
        stepMsg("Deleting Java code for module " + moduleName);

        try
        {
            deleteAllThingsInFolder(new File(mergeDir + File.separator + moduleName + File.separator + "src" + File.separator + "main" + File.separator + "java"));
            ok();
        }
        catch (Exception e)
        {
            fail();
        }
    }

    private void copySourceForModule(String moduleName)
    {
        stepMsg("Copying new Java code for module " + moduleName);

        try
        {
            File newJavaSourceDir = new File(mergeDir + File.separator + TEMP_FOLDER_NAME + File.separator + moduleName + "-sources");

            for(File f : newJavaSourceDir.listFiles())
            {
                String outDir = mergeDir + File.separator + moduleName + File.separator + "src" + File.separator + "main" + File.separator + "java" + File.separator + f.getName();
                recursiveCopyDir(f.getAbsolutePath(), outDir);
            }
            ok();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail();
        }
    }

    private void reformatModuleToLf(String moduleName)
    {
        stepMsg("Recursively reformatting module '" + moduleName + "' to LF line endings");

        try
        {
            recursiveReformatToLf(mergeDir +File.separator + moduleName + File.separator + "src");
            ok();
        }
        catch (Exception e)
        {
            fail();
        }
    }

    private void ensureWeAreInExtractedRcRepo()
    {
        stepMsg("Running preliminary check on merge directory");

        for(String s : moduleNames)
        {
            if(!new File(mergeDir + File.separator + s).exists())
            {
                fail();
            }
        }

        if(!new File(mergeDir + File.separator + "FtcRobotController").exists())
        {
            fail();
        }

        ok();
    }

    private void checkThatStockSdkHasAars()
    {
        stepMsg("Running preliminary check on stock SDK");

        for(String s : moduleNames)
        {
            if(!(makeFileForModuleAar(s).exists() && makeFileForModuleSourcesJar(s).exists()))
            {
                fail();
            }
        }

        ok();
    }

    private void prepareTempDir()
    {
        File tempDir = new File(mergeDir + File.separator + TEMP_FOLDER_NAME);

        stepMsg("Deleting temporary folder in merge directory if it exists");

        try
        {
            deleteFolder(tempDir);
            ok();
        }
        catch (Exception e)
        {
            fail();
        }

        stepMsg("Creating temporary folder in merge directory");

        if(tempDir.mkdir())
        {
            ok();
        }
        else
        {
            fail();
        }
    }

    private void extractAarsAndSourceJars()
    {
        for(String s : moduleNames)
        {
            extractAarToTempDir(s);
            extractSourcesJarToTempDir(s);
        }
    }

    private void extractAarToTempDir(String aarName)
    {
        stepMsg("Extracting '" + aarName + "' AAR to temporary merge directory");

        try
        {
            new ZipFile(makeFileForModuleAar(aarName)).extractAll(mergeDir + File.separator + TEMP_FOLDER_NAME + File.separator + aarName + "-aar");
            ok();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail();
        }
    }

    private void extractSourcesJarToTempDir(String jarName)
    {
        stepMsg("Extracting '" + jarName + "' sources JAR to temporary merge directory");

        try
        {
            new ZipFile(makeFileForModuleSourcesJar(jarName)).extractAll(mergeDir + File.separator + TEMP_FOLDER_NAME + File.separator + jarName + "-sources");
            ok();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail();
        }
    }

    private File makeFileForModuleSourcesJar(String moduleName)
    {
        return new File(stockDir + File.separator + "libs" + File.separator + moduleName + "-release-sources.jar");
    }

    private File makeFileForModuleAar(String moduleName)
    {
        return new File(stockDir + File.separator + "libs" + File.separator + moduleName + "-release.aar");
    }

    private void fail()
    {
        if(waitingForFailOrOk)
        {
            System.out.println("[FAIL]");
            System.exit(1);
            waitingForFailOrOk = false;
        }
        else
        {
            throw new RuntimeException();
        }
    }

    private void ok()
    {
        if(waitingForFailOrOk)
        {
            System.out.println("[OK]");
            waitingForFailOrOk = false;
        }
        else
        {
            throw new RuntimeException();
        }
    }

    private void stepMsg(String msg)
    {
        System.out.print("> " + msg + "... ");
        waitingForFailOrOk = true;
    }

    private static void deleteAllThingsInFolder(File folder)
    {
        File[] files = folder.listFiles();

        if (files != null)
        {
            for (File f : files)
            {
                if (f.isDirectory())
                {
                    deleteFolder(f);
                }
                else
                {
                    if(!f.delete())
                    {
                        throw new RuntimeException();
                    }
                }
            }
        }
    }

    private static void deleteFolder(File folder)
    {
        deleteAllThingsInFolder(folder);

        if(!folder.delete())
        {
            throw new RuntimeException();
        }
    }

    private static void recursiveCopyDir(String in, String out) throws IOException
    {
        for(File f : new File(in).listFiles())
        {
            if(f.isDirectory())
            {
                recursiveCopyDir(f.getAbsolutePath(), out + File.separator + f.getName());
            }
            else
            {
                new File(out + File.separator + f.getName()).getParentFile().mkdirs();
                Files.copy(Paths.get(f.getAbsolutePath()), Paths.get(out + File.separator + f.getName()), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static void recursiveReformatToLf(String dir) throws IOException
    {
        for(File f : new File(dir).listFiles())
        {
            if(f.isDirectory())
            {
                recursiveReformatToLf(f.getAbsolutePath());
            }
            else
            {
                Charset charset = StandardCharsets.UTF_8;
                String fileContent = new String(Files.readAllBytes(Paths.get(f.getAbsolutePath())), charset);
                fileContent = fileContent.replaceAll("\r\n", "\n");
                Files.write(Paths.get(f.getAbsolutePath()), fileContent.getBytes(charset));
            }
        }
    }
}
