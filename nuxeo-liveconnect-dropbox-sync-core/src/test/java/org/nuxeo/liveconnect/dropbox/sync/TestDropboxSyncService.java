package org.nuxeo.liveconnect.dropbox.sync;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features(AutomationFeature.class)
@Deploy({
    "org.nuxeo.ecm.platform.oauth",
    "org.nuxeo.ecm.liveconnect.core",
    "org.nuxeo.ecm.liveconnect.dropbox.core",
    "org.nuxeo.ecm.core.cache",
    "org.nuxeo.ecm.platform.filemanager.core",
    "org.nuxeo.ecm.platform.types.core",
    "nuxeo-liveconnect-dropbox-sync-core"})
@LocalDeploy({
    "nuxeo-liveconnect-dropbox-sync-core:OSGI-INF/oauth-provider-contrib.xml",
})
public class TestDropboxSyncService {

    @Inject
    CoreSession session;

    @Inject
    protected DropboxSyncService dropboxsyncservice;

    @Test
    public void testService() {
        assertNotNull(dropboxsyncservice);

        AuthenticationHelper.createToken(session.getPrincipal().getName(),"dropbox");

        DocumentModel rootFolder = session.createDocumentModel(session.getRootDocument().getPathAsString(),
                "rootFolder","Folder");
        rootFolder = session.createDocument(rootFolder);

        dropboxsyncservice.sync(rootFolder,"dropbox");

        DocumentModelList docs = session.query("Select * From File");
        assertTrue(docs.size()>0);
    }
}
