package org.nuxeo.liveconnect.dropbox.sync;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;

@Operation(
        id=SyncDropboxContentOp.ID,
        category=Constants.CAT_DOCUMENT,
        label="SyncDropboxContent",
        description="Schedule the synchronization of all dropbox account registered in liveconnect")
public class SyncDropboxContentOp {

    public static final String ID = "Document.SyncDropboxContentOp";

    @Context
    protected CoreSession session;

    @Context
    protected WorkManager workManager;

    @Param(name = "provider", required = false)
    protected String provider="dropbox";

    @OperationMethod
    public DocumentModel run(DocumentModel doc) {
        Work work = new DropboxSyncWorker(provider,doc.getId());
        workManager.schedule(work);
        return doc;
    }
}
