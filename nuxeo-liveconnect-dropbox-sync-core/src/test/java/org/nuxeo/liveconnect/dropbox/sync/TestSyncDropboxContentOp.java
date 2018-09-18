package org.nuxeo.liveconnect.dropbox.sync;

import java.util.List;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.oauth2.tokens.NuxeoOAuth2Token;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(AutomationFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy({
    "org.nuxeo.ecm.platform.oauth",
    "nuxeo-liveconnect-dropbox-sync-core"
})

public class TestSyncDropboxContentOp {

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService automationService;

    @Inject
    protected WorkManager workManager;

    @Test
    public void shouldCallWithParameters() throws OperationException {
        NuxeoOAuth2Token token = AuthenticationHelper.createToken(session.getPrincipal().getName(),AuthenticationHelper.TEST_PROVIDER);
        Assume.assumeTrue(token.getAccessToken()!=null);

        DocumentModel rootFolder = session.createDocumentModel(session.getRootDocument().getPathAsString(),
                "rootFolder","Folder");

        rootFolder = session.createDocument(rootFolder);

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(rootFolder);

        DocumentModel doc = (DocumentModel) automationService.run(ctx, SyncDropboxContentOp.ID);

        List<Work> works = workManager.listWork("liveconnectSync",null);
        Assert.assertTrue(works.size()>0);
    }
}
