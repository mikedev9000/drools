package org.kie.api.builder.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.kie.api.builder.helper.KieModuleDeploymentHelperImpl.internalLoadResources;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.kie.api.builder.helper.KieModuleDeploymentHelperImpl.KJarResource;

public class KieModuleDeploymentHelperLoadResourcesTest {

    @Test
    public void testInternalLoadResources() throws Exception {
        List<KJarResource> resources = null;
        // local
        String path = "/builder/simple_query_test.drl";
        resources = internalLoadResources(path, false);
        assertEquals( path, 1, resources.size());
        String content = resources.get(0).content;
        assertTrue( content != null && content.length() > 10 );

        path = "/builder/test/";
        resources = internalLoadResources(path, true);
        assertEquals( path, 2, resources.size());
        content = resources.get(0).content;
        assertTrue( content != null && content.length() > 10 );

        path = "/builder/";
        resources = internalLoadResources(path, true);
        assertEquals( path, 1, resources.size());
        content = resources.get(0).content;
        assertTrue( content != null && content.length() > 10 );

        // classpath
        path = "META-INF/WorkDefinitions.conf";
        resources = internalLoadResources(path, false);
        assertEquals( path, 1, resources.size());
        content = resources.get(0).content;
        assertTrue( content != null && content.length() > 10 );

        path = "META-INF/plexus/";
        resources = internalLoadResources(path, true);
        assertEquals( path, 2, resources.size());
        content = resources.get(0).content;
        assertTrue( content != null && content.length() > 10 );

        // file
        content = "test file created by " + this.getClass().getSimpleName();

        final String baseTempPath = System.getProperty("java.io.tmpdir");
        File tempFile = File.createTempFile(UUID.randomUUID().toString(), ".tst");
        tempFile.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(tempFile);
        fos.write(content.getBytes());
        fos.close();
        
        resources = internalLoadResources(tempFile.getAbsolutePath(), false);
        assertEquals( path, 1, resources.size());
        content = resources.get(0).content;
        assertTrue( content != null && content.length() > 10 );

        File tempDir = new File(baseTempPath + "/" + UUID.randomUUID().toString());
        tempDir.mkdir();
        tempDir.deleteOnExit();
        tempFile = new File(tempDir.getAbsolutePath() + "/" + UUID.randomUUID().toString() + ".tst");
        fos = new FileOutputStream(tempFile);
        fos.write(content.getBytes());
        fos.close();
        
        resources = internalLoadResources(tempDir.getAbsolutePath(), true);
        assertEquals( path, 1, resources.size());
        content = resources.get(0).content;
        assertTrue( content != null && content.length() > 10 );
    }
}
