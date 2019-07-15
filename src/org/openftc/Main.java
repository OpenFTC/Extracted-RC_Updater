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

    enum Location
    {
        MERGE,
        NEW
    }

    enum Module
    {
        ROBOTCORE  ("RobotCore"),
        FTCCOMMON  ("FtcCommon"),
        HARDWARE   ("Hardware"),
        INSPECTION ("Inspection"),
        ONBOTJAVA  ("OnBotJava"),
        BLOCKS     ("Blocks"),
        ROBOTSERVER("RobotServer");

        String name;

        Module(String name)
        {
            this.name = name;
        }
    }

    enum Archive
    {
        AAR,
        SOURCE_JAR
    }

    enum ModuleFolder
    {
        JAVA_SOURCE (Archive.SOURCE_JAR, "java"   ),
        ASSETS      (Archive.AAR,        "assets" ),
        LIBS        (Archive.AAR,        "libs"   ),
        NATIVE_LIBS (Archive.AAR,        "jniLibs"),
        RESOURCES   (Archive.AAR,        "res"    );

        Archive archive;
        String name;

        ModuleFolder(Archive archive, String name)
        {
            this.archive = archive;
            this.name = name;
        }
    }

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

        for(Module module : Module.values())
        {
            processModule(module);
        }

        checklist();
    }

    private void processModule(Module module)
    {
        System.out.println("===============================================================");
        System.out.println("= Processing module: " + "'" + module.name + "'");
        System.out.println("===============================================================");

        /*
         * Extract the archives
         */
        extractAarToTempDir(module);
        extractSourcesJarToTempDir(module);

        /*
         * Source code
         */
        deleteOldSourceForModule(module);
        copySourceForModule(module);

        /*
         * Resources
         */
        deleteOldResourcesForModule(module);
        copyNewResourcesForModule(module);

        /*
         * Assets
         */
        deleteOldAssetsForModule(module);
        copyNewAssetsForModule(module);

        /*
         * Libs
         */
        deleteOldLibsForModule(module);
        copyNewLibsForModule(module);

        /*
         * Native libs
         */
        deleteOldNativeLibsForModule(module);
        copyNewNativeLibsForModule(module);

        /*
         * Manifest
         */
        deleteOldManifestForModule(module);
        copyNewManifestForModule(module);
    }

    private void copyNewManifestForModule(Module module)
    {
        stepMsg("Copying new manifest file for module '" + module.name + "'");

        String newManifestFilePath = TEMP_FOLDER_PATH + File.separator + module.name + "-aar" + File.separator + MANIFEST_NAME;
        String destPath = getSrcMainPathForModule(module) + File.separator + MANIFEST_NAME;

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

    private void deleteOldManifestForModule(Module module)
    {
        stepMsg("Deleting manifest for module '" + module.name + "'");

        File manifestFile = new File(getSrcMainPathForModule(module) + File.separator + MANIFEST_NAME);

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

    private void copyNewNativeLibsForModule(Module module)
    {
        File newJniLibsDir = new File(TEMP_FOLDER_PATH + File.separator + module.name + "-aar" + File.separator + "jni");

        if(!newJniLibsDir.exists())
        {
            return;
        }

        stepMsg("Copying new native libs for module '" + module.name + "'");

        try
        {
            String outDir = getPathForFolderInModuleSrcMain(module, "jniLibs");
            recursiveCopyDir(newJniLibsDir.getAbsolutePath(), outDir);
            ok();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail();
        }
    }

    private void deleteOldNativeLibsForModule(Module module)
    {
        File jniLibsFolder = new File(getPathForFolderInModuleSrcMain(module, "jniLibs"));

        if(!jniLibsFolder.exists())
        {
            return;
        }

        stepMsg("Deleting native libs for module '" + module.name + "'");

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

    private void copyNewLibsForModule(Module module)
    {
        File newLibsDir = new File(TEMP_FOLDER_PATH + File.separator + module.name + "-aar" + File.separator + "libs");

        if(!newLibsDir.exists())
        {
            return;
        }

        stepMsg("Copying new libs for module '" + module.name + "'");

        try
        {
            String outDir = mergeDir + File.separator + module.name + File.separator + "libs";
            recursiveCopyDir(newLibsDir.getAbsolutePath(), outDir);
            ok();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail();
        }
    }

    private void deleteOldLibsForModule(Module module)
    {
        File libsFolder = new File(mergeDir + File.separator + module.name + File.separator + "libs");

        if(!libsFolder.exists())
        {
            return;
        }

        stepMsg("Deleting libs for module '" + module.name + "'");

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

    private void copyNewAssetsForModule(Module module)
    {
        File newAsssetsDir = new File(TEMP_FOLDER_PATH + File.separator + module.name + "-aar" + File.separator + "assets");

        if(!newAsssetsDir.exists())
        {
            return;
        }

        stepMsg("Copying new assets for module '" + module.name + "'");

        try
        {
            String outDir = getPathForFolderInModuleSrcMain(module, "assets");
            recursiveCopyDir(newAsssetsDir.getAbsolutePath(), outDir);
            ok();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail();
        }
    }

    private void deleteOldAssetsForModule(Module module)
    {
        File assetsFolder = new File(getPathForFolderInModuleSrcMain(module, "assets"));

        if(!assetsFolder.exists())
        {
            return;
        }

        stepMsg("Deleting assets for module '" + module.name + "'");

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

    private void copyNewResourcesForModule(Module module)
    {
        stepMsg("Copying new resources for module '" + module.name + "'");

        try
        {
            File newResourcesDir = new File(TEMP_FOLDER_PATH + File.separator + module.name + "-aar" + File.separator + "res");

            for(File f : newResourcesDir.listFiles())
            {
                String outDir = getPathForFolderInModuleSrcMain(module, "res") + File.separator + f.getName();
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

    private void deleteOldResourcesForModule(Module module)
    {
        stepMsg("Deleting resources for module '" + module.name + "'");

        try
        {
            deleteAllThingsInFolder(new File(getPathForFolderInModuleSrcMain(module, "res")));
            ok();
        }
        catch (Exception e)
        {
            fail();
        }
    }

    private void deleteOldSourceForModule(Module module)
    {
        stepMsg("Deleting Java code for module " + module.name);

        try
        {
            deleteAllThingsInFolder(new File(getPathForFolderInModuleSrcMain(module, "java")));
            ok();
        }
        catch (Exception e)
        {
            fail();
        }
    }

    private void copySourceForModule(Module module)
    {
        stepMsg("Copying new Java code for module " + module.name);

        try
        {
            File newJavaSourceDir = new File(TEMP_FOLDER_PATH + File.separator + module.name + "-sources");

            for(File f : newJavaSourceDir.listFiles())
            {
                String outDir = getPathForFolderInModuleSrcMain(module, "java") + File.separator + f.getName();
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

        for(Module module : Module.values())
        {
            if(!new File(mergeDir + File.separator + module.name).exists())
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

        for(Module module : Module.values())
        {
            if(!(makeFileForModuleAar(module).exists() && makeFileForModuleSourcesJar(module).exists()))
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

    private void extractAarToTempDir(Module module)
    {
        stepMsg("Extracting '" + module.name + "' AAR to temporary merge directory");

        try
        {
            new ZipFile(makeFileForModuleAar(module)).extractAll(TEMP_FOLDER_PATH + File.separator + module.name + "-aar");
            ok();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail();
        }
    }

    private void extractSourcesJarToTempDir(Module module)
    {
        stepMsg("Extracting '" + module.name + "' sources JAR to temporary merge directory");

        try
        {
            new ZipFile(makeFileForModuleSourcesJar(module)).extractAll(TEMP_FOLDER_PATH + File.separator + module.name + "-sources");
            ok();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail();
        }
    }

    private File makeFileForModuleSourcesJar(Module module)
    {
        return new File(stockDir + File.separator + "libs" + File.separator + module.name + "-release-sources.jar");
    }

    private File makeFileForModuleAar(Module module)
    {
        return new File(stockDir + File.separator + "libs" + File.separator + module.name + "-release.aar");
    }
    
    private String getSrcMainPathForModule(Module module)
    {
        return mergeDir + File.separator + module.name + File.separator + "src" + File.separator + "main";
    }

    private String getPathForFolderInModuleSrcMain(Module module, String folderName)
    {
        return getSrcMainPathForModule(module) + File.separator + folderName;
    }

    private String getFolderPath(Location location, Module module, ModuleFolder folder)
    {
        switch (location)
        {
            case MERGE:
            {
                return mergeDir + File.separator + module.name + File.separator + folder.name;
            }

            case NEW:
            {
                if(folder.archive == Archive.SOURCE_JAR)
                {
                    return TEMP_FOLDER_PATH + File.separator + module.name + "-aar" + File.separator + folder.name;
                }
                else if(folder.archive == Archive.AAR)
                {
                    if(folder == ModuleFolder.JAVA_SOURCE)
                    {
                        return TEMP_FOLDER_PATH + File.separator + module.name + "-sources";
                    }

                    return null;
                }
            }

            default:
            {
                return null;
            }
        }
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
