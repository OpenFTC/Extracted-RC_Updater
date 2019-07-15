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
    private static final String MANIFEST_NAME = "AndroidManifest.xml";

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException
    {
        System.out.println("ExtractedRC_Updater v1.0");

        Main instance = new Main();

        JCommander jCommander =
                JCommander.newBuilder()
                        .addObject(instance)
                        .programName("java -jar ExtractedRC_Updater.jar")
                        .build();

        try
        {
            jCommander.parse(args);

            if (instance.help)
            {
                System.out.println("ExtractedRC Updater v1.0");
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
        System.out.println();
        System.out.println("This script is intended to be used by the primary maintainer of ExtractedRC ONLY.");
        System.out.println("Press Control-C to exit if you are just an OpenRC user.");
        pause();
        System.out.println("Please ensure that you have closed the project in Android Studio");
        pause();

        ensureWeAreInExtractedRcRepo();
        checkThatStockSdkHasAars();
        prepareTempDir();
        //extractAarsAndSourceJars();
        //copyModuleSources();
        //copyModuleResources();
        //copyModuleAssets();
        //copyModuleLibs();
        //copyModuleManifests();
        //copyModuleNativeLibs();

        for(String s : moduleNames)
        {
            processModule(s);
        }

        checklist();
    }

    private void processModule(String s)
    {
        System.out.println("===============================================================");
        System.out.println("= Processing module: " + "'" + s + "'");
        System.out.println("===============================================================");

        extractAarToTempDir(s);
        extractSourcesJarToTempDir(s);
        deleteOldSourceForModule(s);
        copySourceForModule(s);
        deleteOldResourcesForModule(s);
        copyNewResourcesForModule(s);
        deleteOldAssetsForModule(s);
        copyNewAssetsForModule(s);
        deleteOldLibsForModule(s);
        copyNewLibsForModule(s);
        deleteOldManifestForModule(s);
        copyNewManifestForModule(s);
        deleteOldNativeLibsForModule(s);
        copyNewNativeLibsForModule(s);
    }

//    private void copyModuleSources()
//    {
//        for (String s : moduleNames)
//        {
//            deleteOldSourceForModule(s);
//        }
//
//        for(String s : moduleNames)
//        {
//            copySourceForModule(s);
//        }
//    }
//
//    private void copyModuleResources()
//    {
//        for(String s : moduleNames)
//        {
//            deleteOldResourcesForModule(s);
//        }
//
//        for(String s : moduleNames)
//        {
//            copyNewResourcesForModule(s);
//        }
//    }
//
//    private void copyModuleAssets()
//    {
//        for(String s : moduleNames)
//        {
//            deleteOldAssetsForModule(s);
//        }
//
//        for(String s : moduleNames)
//        {
//            copyNewAssetsForModule(s);
//        }
//    }
//
//    private void copyModuleLibs()
//    {
//        for(String s : moduleNames)
//        {
//            deleteOldLibsForModule(s);
//        }
//
//        for(String s : moduleNames)
//        {
//            copyNewLibsForModule(s);
//        }
//    }
//
//    private void copyModuleManifests()
//    {
//        for(String s : moduleNames)
//        {
//            deleteOldManifestForModule(s);
//        }
//
//        for(String s : moduleNames)
//        {
//            copyNewManifestForModule(s);
//        }
//    }
//
//    private void copyModuleNativeLibs()
//    {
//        for(String s : moduleNames)
//        {
//            deleteOldNativeLibsForModule(s);
//        }
//
//        for(String s : moduleNames)
//        {
//            copyNewNativeLibsForModule(s);
//        }
//    }

    private void copyNewManifestForModule(String moduleName)
    {
        stepMsg("Copying new manifest file for module '" + moduleName + "'");

        String newManifestFilePath = mergeDir + File.separator + TEMP_FOLDER_NAME + File.separator + moduleName + "-aar" + File.separator + MANIFEST_NAME;
        String destPath = mergeDir + File.separator + moduleName + File.separator + "src" + File.separator + "main" + File.separator + MANIFEST_NAME;

        try
        {
            Files.copy(Paths.get(newManifestFilePath), Paths.get(destPath));
            ok();
        }
        catch (Exception e)
        {
            fail();
        }
    }

    private void deleteOldManifestForModule(String moduleName)
    {
        stepMsg("Deleting manifest for module '" + moduleName + "'");

        File manifestFile = new File(mergeDir + File.separator + moduleName + File.separator + "src" + File.separator + "main" + File.separator + MANIFEST_NAME);

        try
        {
            manifestFile.delete();
            ok();
        }
        catch (Exception e)
        {
            fail();
        }
    }

    private void copyNewNativeLibsForModule(String moduleName)
    {
        File newJniLibsDir = new File(mergeDir + File.separator + TEMP_FOLDER_NAME + File.separator + moduleName + "-aar" + File.separator + "jni");

        if(!newJniLibsDir.exists())
        {
            return;
        }

        stepMsg("Copying new native libs for module '" + moduleName + "'");

        try
        {
            String outDir = mergeDir + File.separator + moduleName + File.separator + "src" + File.separator + "main" + File.separator + "jniLibs";
            recursiveCopyDir(newJniLibsDir.getAbsolutePath(), outDir);
            ok();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail();
        }
    }

    private void deleteOldNativeLibsForModule(String moduleName)
    {
        File jniLibsFolder = new File(mergeDir + File.separator + moduleName + File.separator + "src" + File.separator + "main" + File.separator + "jniLibs");

        if(!jniLibsFolder.exists())
        {
            return;
        }

        stepMsg("Deleting native libs for module '" + moduleName + "'");

        try
        {
            deleteFolder(jniLibsFolder);
            ok();
        }
        catch (Exception e)
        {
            fail();
        }
    }

    private void copyNewLibsForModule(String moduleName)
    {
        File newLibsDir = new File(mergeDir + File.separator + TEMP_FOLDER_NAME + File.separator + moduleName + "-aar" + File.separator + "libs");

        if(!newLibsDir.exists())
        {
            return;
        }

        stepMsg("Copying new libs for module '" + moduleName + "'");

        try
        {
            String outDir = mergeDir + File.separator + moduleName + File.separator + "libs";
            recursiveCopyDir(newLibsDir.getAbsolutePath(), outDir);
            ok();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail();
        }
    }

    private void deleteOldLibsForModule(String moduleName)
    {
        File libsFolder = new File(mergeDir + File.separator + moduleName + File.separator + "libs");

        if(!libsFolder.exists())
        {
            return;
        }

        stepMsg("Deleting libs for module '" + moduleName + "'");

        try
        {
            deleteAllThingsInFolder(libsFolder);
            ok();
        }
        catch (Exception e)
        {
            fail();
        }
    }

    private void copyNewAssetsForModule(String moduleName)
    {
        File newAsssetsDir = new File(mergeDir + File.separator + TEMP_FOLDER_NAME + File.separator + moduleName + "-aar" + File.separator + "assets");

        if(!newAsssetsDir.exists())
        {
            return;
        }

        stepMsg("Copying new assets for module '" + moduleName + "'");

        try
        {
            String outDir = mergeDir + File.separator + moduleName + File.separator + "src" + File.separator + "main" + File.separator + "assets" + File.separator;
            recursiveCopyDir(newAsssetsDir.getAbsolutePath(), outDir);
            ok();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail();
        }
    }

    private void deleteOldAssetsForModule(String moduleName)
    {
        File assetsFolder = new File(mergeDir + File.separator + moduleName + File.separator + "src" + File.separator + "main" + File.separator + "assets");

        if(!assetsFolder.exists())
        {
            return;
        }

        stepMsg("Deleting assets for module '" + moduleName + "'");

        try
        {
            deleteAllThingsInFolder(assetsFolder);
            ok();
        }
        catch (Exception e)
        {
            fail();
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
            if(tempDir.exists())
            {
                deleteFolder(tempDir);
            }
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

    private void deleteAllThingsInFolder(File folder)
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

    private void deleteFolder(File folder)
    {
        deleteAllThingsInFolder(folder);

        if(!folder.delete())
        {
            throw new RuntimeException();
        }
    }

    private void recursiveCopyDir(String in, String out) throws IOException
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

    private void checklist()
    {
        System.out.println();
        System.out.println("=======================================================");
        System.out.println("= Checklist of stuff to do after this script finishes =");
        System.out.println("=======================================================");
        System.out.println();

        System.out.println("1. Remove the version code and name for each library from its manifest");
        System.out.println("2. Update the version code and name for each library in its build.gradle file");
        System.out.println("3. Use AS to reformat all text files to LF line endings (select root project folder, File -> Line Endings -> LF)");
    }

    private void pause()
    {
        System.out.println("Press Enter continue...");
        try
        {
            System.in.read();
        }
        catch(Exception e)
        {

        }
    }
}
