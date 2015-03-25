package ch.mfrey.maven.plugin.copy.test;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Assert;
import org.junit.Test;

import ch.mfrey.maven.plugin.copy.CopyMojo;
import ch.mfrey.maven.plugin.copy.Replace;
import ch.mfrey.maven.plugin.copy.Resource;

public class CopyMojoTest {

    private static final String currentLoc = CopyMojoTest.class.getResource("/").getFile();

    @Test
    public void testNothingToDo() throws MojoExecutionException, MojoFailureException {
        CopyMojo mojo = new CopyMojo();
        mojo.setShowfiles(true);

        Resource resource = new Resource();
        resource.addInclude("folder1/**/*.txt");
        resource.setDirectory(currentLoc);
        mojo.setResources(new Resource[] { resource });

        mojo.execute();

        Assert.assertTrue(new File(currentLoc + "/folder1/folder2/test.txt").exists());
    }

    @Test
    public void testCopy() throws MojoExecutionException, MojoFailureException {
        CopyMojo mojo = new CopyMojo();
        mojo.setShowfiles(true);

        Resource resource = new Resource();
        resource.addInclude("folder1/**/*.txt");
        resource.setDirectory(currentLoc);
        resource.addPath(new Replace("folder1", "copy1"));
        mojo.setResources(new Resource[] { resource });

        mojo.execute();

        Assert.assertTrue(new File(currentLoc + "/folder1/folder2/test.txt").exists());
        File file = new File(currentLoc + "/copy1/folder2/test.txt");
        Assert.assertTrue(file.exists());
        file.delete();
    }

    @Test
    public void testCopy2() throws MojoExecutionException, MojoFailureException {
        CopyMojo mojo = new CopyMojo();
        mojo.setShowfiles(true);

        Resource resource = new Resource();
        resource.addInclude("folder1/**/*.txt");
        resource.setDirectory(currentLoc);
        resource.addPath(new Replace("folder1/folder2", "copy1/copy2")).addPath(new Replace("test", "test2"));
        mojo.setResources(new Resource[] { resource });

        mojo.execute();

        Assert.assertTrue(new File(currentLoc + "/folder1/folder2/test.txt").exists());
        File file = new File(currentLoc + "/copy1/copy2/test2.txt");
        Assert.assertTrue(file.exists());
        file.delete();
    }

    @Test
    public void testCopyAndReplace() throws MojoExecutionException, MojoFailureException, IOException {
        CopyMojo mojo = new CopyMojo();
        mojo.setShowfiles(true);

        Resource resource = new Resource();
        resource.addInclude("folder1/**/*.txt");
        resource.setDirectory(currentLoc);
        resource.addPath(new Replace("folder1/folder2", "copy1/copy2")).addPath(new Replace("test", "test2"));
        resource.addReplace(new Replace("to be modified", "has been modified"));
        mojo.setResources(new Resource[] { resource });

        mojo.execute();

        Assert.assertTrue(new File(currentLoc + "/folder1/folder2/test.txt").exists());
        File file = new File(currentLoc + "/copy1/copy2/test2.txt");
        Assert.assertTrue(file.exists());
        String readFileToString = FileUtils.readFileToString(file, resource.getCharset());
        // file.delete();

        // check if replaced twice
        int idx = readFileToString.indexOf("has been modified");
        Assert.assertTrue(idx != -1);
        Assert.assertTrue(readFileToString.indexOf("has been modified", idx + 1) != -1);
    }

    @Test
    public void testSafeWorkingDir() throws MojoExecutionException, MojoFailureException {
        CopyMojo mojo = new CopyMojo();
        mojo.setShowfiles(true);

        Resource resource = new Resource();
        resource.addInclude("folder1/**/*.txt");
        resource.setDirectory(currentLoc);
        resource.addPath(new Replace(currentLoc, "/tmp"));
        mojo.setResources(new Resource[] { resource });

        mojo.execute();

        Assert.assertFalse(new File("/tmp/test.txt").exists());
    }

    @Test
    public void testUnsafeWorkingDir() throws MojoExecutionException, MojoFailureException {
        CopyMojo mojo = new CopyMojo();
        mojo.setShowfiles(true);

        Resource resource = new Resource();
        resource.setWorkOnFullPath(true);
        resource.addInclude("folder1/**/*.txt");
        resource.setDirectory(currentLoc);
        resource.addPath(new Replace(currentLoc + "folder1/folder2", "/tmp")).addPath(new Replace("test", "test2"));
        mojo.setResources(new Resource[] { resource });

        mojo.execute();

        File file = new File("/tmp/test2.txt");
        Assert.assertTrue(file.exists());
        file.delete();
    }

    @Test
    public void testMove() throws MojoExecutionException, MojoFailureException {
        copy();

        CopyMojo mojo = new CopyMojo();
        mojo.setShowfiles(true);

        Resource resource = new Resource();
        resource.setMove(true);
        resource.addInclude("copy1/**/*.txt");
        resource.setDirectory(currentLoc);
        resource.addPath(new Replace("folder2", "copy2/copy3/copy4"));
        mojo.setResources(new Resource[] { resource });

        mojo.execute();

        Assert.assertFalse(new File(currentLoc + "/copy1/folder2/test.txt").exists());
        File file = new File(currentLoc + "/copy1/copy2/copy3/copy4/test.txt");
        Assert.assertTrue(file.exists());
        file.delete();
    }

    private void copy() throws MojoExecutionException, MojoFailureException {
        CopyMojo mojo = new CopyMojo();
        mojo.setShowfiles(true);

        Resource resource = new Resource();
        resource.addInclude("folder1/**/*.txt");
        resource.setDirectory(currentLoc);
        resource.addPath(new Replace("folder1", "copy1"));
        mojo.setResources(new Resource[] { resource });

        mojo.execute();

        Assert.assertTrue(new File(currentLoc + "/folder1/folder2/test.txt").exists());
        Assert.assertTrue(new File(currentLoc + "/copy1/folder2/test.txt").exists());
    }
}