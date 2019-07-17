/*
 * Copyright (c) 2019 OpenFTC Team
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

import net.lingala.zip4j.core.ZipFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;

public class Updater
{
    enum Location
    {
        EXISTING,
        NEW
    }

    enum Module
    {
        ROBOTCORE          ("RobotCore",          true ),
        FTCCOMMON          ("FtcCommon",          true ),
        FTCROBOTCONTROLLER ("FtcRobotController", false),
        HARDWARE           ("Hardware",           true ),
        INSPECTION         ("Inspection",         true ),
        ONBOTJAVA          ("OnBotJava",          true ),
        BLOCKS             ("Blocks",             true ),
        ROBOTSERVER        ("RobotServer",        true );

        String name;
        boolean isPackagedInArchive;

        Module(String name, boolean isPackagedInArchive)
        {
            this.name = name;
            this.isPackagedInArchive = isPackagedInArchive;
        }
    }

    enum Archive
    {
        AAR,
        SOURCE_JAR
    }

    enum ModuleItem
    {
        ROOT_DIR(null, null, null),
        JAVA_SOURCE (Archive.SOURCE_JAR, "java",                "java"  ),
        ASSETS      (Archive.AAR,        "assets",              "assets"),
        LIBS        (Archive.AAR,        "libs",                "libs"  ),
        NATIVE_LIBS (Archive.AAR,        "jniLibs",             "jni"   ),
        RESOURCES   (Archive.AAR,        "res",                 "res"   ),
        MANIFEST    (Archive.AAR,        "AndroidManifest.xml", "AndroidManifest.xml");

        Archive archiveType;
        String stdName;
        String archiveName;

        ModuleItem(Archive archiveType, String stdName, String archiveName)
        {
            this.archiveType = archiveType;
            this.stdName = stdName;
            this.archiveName = archiveName;
        }
    }

    private static final String TEMP_FOLDER_NAME = "tempMergeFolder";
    private ConsoleStatusManager csm = new ConsoleStatusManager();
    private String TEMP_FOLDER_PATH;
    private String existingMergeDir;
    private String newStockDir;
    private long startTime;

    public Updater(String existingMergeDir, String newStockDir)
    {
        this.existingMergeDir = existingMergeDir;
        this.newStockDir = newStockDir;
    }

    void run()
    {
        startTime = System.currentTimeMillis();
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
        System.out.print((char)27 + "[47m");
        System.out.print((char)27 + "[34m");
        System.out.println();
        System.out.println("===============================================================");
        System.out.println("= Processing module: " + "'" + module.name + "'");
        System.out.println("===============================================================");
        System.out.print((char)27 + "[0m");

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
        csm.stepMsg("Copying new manifest file for module '" + module.name + "'");

        String newManifestFilePath = getItemPath(Location.NEW, module, ModuleItem.MANIFEST);
        String destPath = getItemPath(Location.EXISTING, module, ModuleItem.MANIFEST);

        try
        {
            Files.copy(Paths.get(newManifestFilePath), Paths.get(destPath));
            csm.ok();
        }
        catch (Exception e)
        {
            csm.fail(e);
        }
    }

    private void deleteOldManifestForModule(Module module)
    {
        csm.stepMsg("Deleting manifest for module '" + module.name + "'");

        File manifestFile = getFileForItem(Location.EXISTING, module, ModuleItem.MANIFEST);

        try
        {
            manifestFile.delete();
            csm.ok();
        }
        catch (Exception e)
        {
            csm.fail(e);
        }
    }

    private void copyNewNativeLibsForModule(Module module)
    {
        csm.stepMsg("Copying new native libs for module '" + module.name + "'");

        File newJniLibsDir = getFileForItem(Location.NEW, module, ModuleItem.NATIVE_LIBS);

        if(!newJniLibsDir.exists())
        {
            csm.na();
            return;
        }

        try
        {
            String outDir = getItemPath(Location.EXISTING, module, ModuleItem.NATIVE_LIBS);
            FileUtil.recursiveCopyDir(newJniLibsDir.getAbsolutePath(), outDir);
            csm.ok();
        }
        catch (Exception e)
        {
            csm.fail(e);
        }
    }

    private void deleteOldNativeLibsForModule(Module module)
    {
        csm.stepMsg("Deleting native libs for module '" + module.name + "'");

        File jniLibsFolder = getFileForItem(Location.EXISTING, module, ModuleItem.NATIVE_LIBS);

        if(!jniLibsFolder.exists())
        {
            csm.na();
            return;
        }

        try
        {
            FileUtil.deleteFolder(jniLibsFolder);
            csm.ok();
        }
        catch (Exception e)
        {
            csm.fail(e);
        }
    }

    private void copyNewLibsForModule(Module module)
    {
        csm.stepMsg("Copying new libs for module '" + module.name + "'");

        File newLibsDir = getFileForItem(Location.NEW, module, ModuleItem.LIBS);

        if(!newLibsDir.exists())
        {
            csm.na();
            return;
        }

        try
        {
            String outDir = getItemPath(Location.EXISTING, module, ModuleItem.LIBS);
            FileUtil.recursiveCopyDir(newLibsDir.getAbsolutePath(), outDir);
            csm.ok();
        }
        catch (Exception e)
        {
            csm.fail(e);
        }
    }

    private void deleteOldLibsForModule(Module module)
    {
        csm.stepMsg("Deleting libs for module '" + module.name + "'");

        File libsFolder = getFileForItem(Location.EXISTING, module, ModuleItem.LIBS);

        if(!libsFolder.exists())
        {
            csm.na();
            return;
        }

        try
        {
            FileUtil.deleteAllThingsInFolder(libsFolder);
            csm.ok();
        }
        catch (Exception e)
        {
            csm.fail(e);
        }
    }

    private void copyNewAssetsForModule(Module module)
    {
        csm.stepMsg("Copying new assets for module '" + module.name + "'");

        File newAsssetsDir = getFileForItem(Location.NEW, module, ModuleItem.ASSETS);

        if(!newAsssetsDir.exists())
        {
            csm.na();
            return;
        }

        try
        {
            String outDir = getItemPath(Location.EXISTING, module, ModuleItem.ASSETS);
            FileUtil.recursiveCopyDir(newAsssetsDir.getAbsolutePath(), outDir);
            csm.ok();
        }
        catch (Exception e)
        {
            csm.fail(e);
        }
    }

    private void deleteOldAssetsForModule(Module module)
    {
        csm.stepMsg("Deleting assets for module '" + module.name + "'");

        File assetsFolder = getFileForItem(Location.EXISTING, module, ModuleItem.ASSETS);

        if(!assetsFolder.exists())
        {
            csm.na();
            return;
        }

        try
        {
            FileUtil.deleteAllThingsInFolder(assetsFolder);
            csm.ok();
        }
        catch (Exception e)
        {
            csm.fail(e);
        }
    }

    private void copyNewResourcesForModule(Module module)
    {
        csm.stepMsg("Copying new resources for module '" + module.name + "'");

        try
        {
            File newResourcesDir = getFileForItem(Location.NEW, module, ModuleItem.RESOURCES);

            for(File f : newResourcesDir.listFiles())
            {
                String outDir = getItemPath(Location.EXISTING, module, ModuleItem.RESOURCES) + File.separator + f.getName();
                FileUtil.recursiveCopyDir(f.getAbsolutePath(), outDir);
            }
            csm.ok();
        }
        catch (Exception e)
        {
            csm.fail(e);
        }
    }

    private void deleteOldResourcesForModule(Module module)
    {
        csm.stepMsg("Deleting resources for module '" + module.name + "'");

        try
        {
            FileUtil.deleteAllThingsInFolder(getFileForItem(Location.EXISTING, module, ModuleItem.RESOURCES));
            csm.ok();
        }
        catch (Exception e)
        {
            csm.fail(e);
        }
    }

    private void deleteOldSourceForModule(Module module)
    {
        csm.stepMsg("Deleting Java code for module " + module.name);

        try
        {
            FileUtil.deleteAllThingsInFolder(getFileForItem(Location.EXISTING, module, ModuleItem.JAVA_SOURCE));
            csm.ok();
        }
        catch (Exception e)
        {
            csm.fail(e);
        }
    }

    private void copySourceForModule(Module module)
    {
        csm.stepMsg("Copying new Java code for module " + module.name);

        try
        {
            File newJavaSourceDir = getFileForItem(Location.NEW, module, ModuleItem.JAVA_SOURCE);

            for(File f : newJavaSourceDir.listFiles())
            {
                String outDir = getItemPath(Location.EXISTING, module, ModuleItem.JAVA_SOURCE) + File.separator + f.getName();
                FileUtil.recursiveCopyDir(f.getAbsolutePath(), outDir);
            }
            csm.ok();
        }
        catch (Exception e)
        {
            csm.fail(e);
        }
    }

    private void ensureWeAreInExtractedRcRepo()
    {
        csm.stepMsg("Running preliminary check on merge directory");

        for(Module module : Module.values())
        {
            if(!getFileForItem(Location.EXISTING, module, ModuleItem.ROOT_DIR).exists())
            {
                csm.fail();
            }
        }

        if(!new File(existingMergeDir + File.separator + "FtcRobotController").exists())
        {
            csm.fail();
        }

        csm.ok();
    }

    private void checkThatStockSdkHasAars()
    {
        csm.stepMsg("Running preliminary check on stock SDK");

        for(Module module : Module.values())
        {
            if(!module.isPackagedInArchive)
            {
                continue;
            }

            if(!(makeFileForModuleAar(module).exists() && makeFileForModuleSourcesJar(module).exists()))
            {
                csm.fail();
            }
        }

        csm.ok();
    }

    private void prepareTempDir()
    {
        TEMP_FOLDER_PATH = existingMergeDir + File.separator + TEMP_FOLDER_NAME;

        File tempDir = new File(TEMP_FOLDER_PATH);

        csm.stepMsg("Deleting temporary folder in merge directory if it exists");

        try
        {
            if(tempDir.exists())
            {
                FileUtil.deleteFolder(tempDir);
            }
            csm.ok();
        }
        catch (Exception e)
        {
            csm.fail(e);
        }

        csm.stepMsg("Creating temporary folder in merge directory");

        if(tempDir.mkdir())
        {
            csm.ok();
        }
        else
        {
            csm.fail();
        }
    }

    private void extractAarToTempDir(Module module)
    {
        csm.stepMsg("Extracting '" + module.name + "' AAR to temporary directory");

        if(!module.isPackagedInArchive)
        {
            csm.na();
            return;
        }

        try
        {
            new ZipFile(makeFileForModuleAar(module)).extractAll(TEMP_FOLDER_PATH + File.separator + module.name + "-aar");
            csm.ok();
        }
        catch (Exception e)
        {
            csm.fail(e);
        }
    }

    private void extractSourcesJarToTempDir(Module module)
    {
        csm.stepMsg("Extracting '" + module.name + "' sources JAR to temporary directory");

        if(!module.isPackagedInArchive)
        {
            csm.na();
            return;
        }

        try
        {
            String path = TEMP_FOLDER_PATH + File.separator + module.name + "-sources";
            new ZipFile(makeFileForModuleSourcesJar(module)).extractAll(path);
            FileUtil.deleteFolder(new File(path + File.separator + "META-INF"));
            csm.ok();
        }
        catch (Exception e)
        {
            csm.fail(e);
        }
    }

    private File makeFileForModuleSourcesJar(Module module)
    {
        return new File(newStockDir + File.separator + "libs" + File.separator + module.name + "-release-sources.jar");
    }

    private File makeFileForModuleAar(Module module)
    {
        return new File(newStockDir + File.separator + "libs" + File.separator + module.name + "-release.aar");
    }

    private String getSrcMainPathForModule(Module module)
    {
        return existingMergeDir + File.separator + module.name + File.separator + "src" + File.separator + "main";
    }

    private String getPathForFolderInModuleSrcMain(Module module, String folderName)
    {
        return getSrcMainPathForModule(module) + File.separator + folderName;
    }

    private File getFileForItem(Location location, Module module, ModuleItem item)
    {
        return new File(getItemPath(location, module, item));
    }

    private String getItemPath(Location location, Module module, ModuleItem item)
    {
        switch (location)
        {

            /*
             * Archive File Tree:
             *
             *    AAR:
             *     - jni (native libs)
             *     - libs
             *     - res (resources)
             *     - assets
             *     - AndroidManifest.xml
             *
             *     JAR:
             *      - com.some.java.packageA
             *      - org.some.java.packageEtc
             *
             * Standard File Tree:
             *
             *     ModuleName
             *      - libs
             *      - src/main
             *         - assets
             *         - jniLibs (native libs)
             *         - res (resources)
             *         - AndroidManifest.xml
             *         - java (Java source)
             *            - com.some.java.packageA
             *            - org.some.java.packageEtc
             */

            /*
             * Standard
             */
            case EXISTING:
            {
                if(item == ModuleItem.ROOT_DIR)
                {
                    return existingMergeDir + File.separator + module.name;
                }
                else if(item == ModuleItem.LIBS)
                {
                    return existingMergeDir + File.separator + module.name + File.separator + item.stdName;
                }
                else if(item == ModuleItem.MANIFEST)
                {
                    return getSrcMainPathForModule(module) + File.separator + item.stdName;
                }
                else
                {
                    return getPathForFolderInModuleSrcMain(module, item.stdName);
                }
            }

            /*
             * Archive
             */
            case NEW:
            {
                if(module == Module.FTCROBOTCONTROLLER)
                {
                    if(item == ModuleItem.ROOT_DIR)
                    {
                        return newStockDir + File.separator + module.name;
                    }
                    else if(item == ModuleItem.LIBS)
                    {
                        return newStockDir + File.separator + module.name + File.separator + item.stdName;
                    }
                    else if(item == ModuleItem.MANIFEST)
                    {
                        return newStockDir + File.separator + module.name + File.separator + "src" + File.separator + "main" + File.separator + item.stdName;
                        //return getSrcMainPathForModule(module) + File.separator + item.stdName;
                    }
                    else
                    {
                        return newStockDir + File.separator + module.name + File.separator + "src" + File.separator + "main" + File.separator + item.stdName;
                        //return getPathForFolderInModuleSrcMain(module, item.stdName);
                    }
                }

                else if(item == ModuleItem.ROOT_DIR)
                {
                    throw new RuntimeException();
                }
                if(item.archiveType == Archive.SOURCE_JAR)
                {
                    if(item == ModuleItem.JAVA_SOURCE)
                    {
                        return TEMP_FOLDER_PATH + File.separator + module.name + "-sources"; //just in root dir
                    }

                    throw new RuntimeException();
                }
                else if(item.archiveType == Archive.AAR)
                {
                    return TEMP_FOLDER_PATH + File.separator + module.name + "-aar" + File.separator + item.archiveName;
                }
            }

            default:
            {
                throw new RuntimeException();
            }
        }
    }

    private void checklist()
    {
        long endTime = System.currentTimeMillis();

        double delta = endTime - startTime;

        DecimalFormat format = new DecimalFormat("#.##");

        System.out.print((char)27 + "[47m");
        System.out.print((char)27 + "[34m");

        System.out.println();
        System.out.println("Script completed successfully in " + format.format(delta/1000d) + " seconds");

        System.out.println();
        System.out.println("=======================================================");
        System.out.println("= Checklist of stuff to do after this script finishes =");
        System.out.println("=======================================================");
        System.out.println();

        System.out.println("1. Remove the version code and name for each library from its manifest");
        System.out.println("2. Update the version code and name for each library in its build.gradle file");
        System.out.println("3. Use AS to reformat all text files to LF line endings (select root project folder, File -> Line Endings -> LF)");
        System.out.println();
        System.out.print((char)27 + "[0m");
    }
}
