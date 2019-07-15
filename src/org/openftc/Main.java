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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;

public class Main
{
    @Parameter(names = "-h", help = true, description = "Print help")
    private boolean help;

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
    private int lengthOfLastStepMsg = 0;
    private static final String TEMP_FOLDER_NAME = "tempMergeFolder";
    private static final String MANIFEST_NAME = "AndroidManifest.xml";

    private String TEMP_FOLDER_PATH;

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

        /*
         * Extract the archives
         */
        extractAarToTempDir(s);
        extractSourcesJarToTempDir(s);

        /*
         * Source code
         */
        deleteOldSourceForModule(s);
        copySourceForModule(s);

        /*
         * Resources
         */
        deleteOldResourcesForModule(s);
        copyNewResourcesForModule(s);

        /*
         * Assets
         */
        deleteOldAssetsForModule(s);
        copyNewAssetsForModule(s);

        /*
         * Libs
         */
        deleteOldLibsForModule(s);
        copyNewLibsForModule(s);

        /*
         * Native libs
         */
        deleteOldNativeLibsForModule(s);
        copyNewNativeLibsForModule(s);

        /*
         * Manifest
         */
        deleteOldManifestForModule(s);
        copyNewManifestForModule(s);
    }

    private void copyNewManifestForModule(String moduleName)
    {
        stepMsg("Copying new manifest file for module '" + moduleName + "'");

        String newManifestFilePath = TEMP_FOLDER_PATH + File.separator + moduleName + "-aar" + File.separator + MANIFEST_NAME;
        String destPath = getSrcMainPathForModule(moduleName) + File.separator + MANIFEST_NAME;

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

        File manifestFile = new File(getSrcMainPathForModule(moduleName) + File.separator + MANIFEST_NAME);

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
        File newJniLibsDir = new File(TEMP_FOLDER_PATH + File.separator + moduleName + "-aar" + File.separator + "jni");

        if(!newJniLibsDir.exists())
        {
            return;
        }

        stepMsg("Copying new native libs for module '" + moduleName + "'");

        try
        {
            String outDir = getSrcMainPathForModule(moduleName) + File.separator + "jniLibs";
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
        File jniLibsFolder = new File(getSrcMainPathForModule(moduleName) + File.separator + "jniLibs");

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
        File newLibsDir = new File(TEMP_FOLDER_PATH + File.separator + moduleName + "-aar" + File.separator + "libs");

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
        File newAsssetsDir = new File(TEMP_FOLDER_PATH + File.separator + moduleName + "-aar" + File.separator + "assets");

        if(!newAsssetsDir.exists())
        {
            return;
        }

        stepMsg("Copying new assets for module '" + moduleName + "'");

        try
        {
            String outDir = getSrcMainPathForModule(moduleName) + File.separator + "assets" + File.separator;
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
        File assetsFolder = new File(getSrcMainPathForModule(moduleName) + File.separator + "assets");

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
            File newResourcesDir = new File(TEMP_FOLDER_PATH + File.separator + moduleName + "-aar" + File.separator + "res");

            for(File f : newResourcesDir.listFiles())
            {
                String outDir = getSrcMainPathForModule(moduleName) + File.separator + "res" + File.separator + f.getName();
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
            deleteAllThingsInFolder(new File(getSrcMainPathForModule(moduleName) + File.separator + "res"));
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
            deleteAllThingsInFolder(new File(getSrcMainPathForModule(moduleName) + File.separator + "java"));
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
            File newJavaSourceDir = new File(TEMP_FOLDER_PATH + File.separator + moduleName + "-sources");

            for(File f : newJavaSourceDir.listFiles())
            {
                String outDir = getSrcMainPathForModule(moduleName) + File.separator + "java" + File.separator + f.getName();
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
        TEMP_FOLDER_PATH = mergeDir + File.separator + TEMP_FOLDER_NAME;

        File tempDir = new File(TEMP_FOLDER_PATH);

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

    private void extractAarToTempDir(String aarName)
    {
        stepMsg("Extracting '" + aarName + "' AAR to temporary merge directory");

        try
        {
            new ZipFile(makeFileForModuleAar(aarName)).extractAll(TEMP_FOLDER_PATH + File.separator + aarName + "-aar");
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
            new ZipFile(makeFileForModuleSourcesJar(jarName)).extractAll(TEMP_FOLDER_PATH + File.separator + jarName + "-sources");
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
    
    private String getSrcMainPathForModule(String moduleName)
    {
        return mergeDir + File.separator + moduleName + File.separator + "src" + File.separator + "main";
    }

    private void fail()
    {
        String failMsg = "[FAIL]";

        StringBuilder builder = new StringBuilder();

        for(int i = lengthOfLastStepMsg; i < 80-failMsg.length(); i++)
        {
            builder.append(" ");
        }

        System.out.print(builder.toString());

        System.out.println(failMsg);

        System.exit(1);
    }

    private void ok()
    {
        String okMsg = "[OK]";

        StringBuilder builder = new StringBuilder();

        for(int i = lengthOfLastStepMsg; i < 80-okMsg.length(); i++)
        {
            builder.append(" ");
        }

        System.out.print(builder.toString());

        System.out.println(okMsg);
    }

    private void stepMsg(String msg)
    {
        msg = "> " + msg + "... ";
        System.out.print(msg);
        waitingForFailOrOk = true;
        lengthOfLastStepMsg = msg.length();
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
