package org.nuxeo.liveconnect.dropbox.sync;

import org.nuxeo.ecm.core.api.DocumentModel;

public interface DropboxSyncService {

    void sync(DocumentModel root,String providerName);

}
