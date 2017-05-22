/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *
 */

package org.nuxeo.liveconnect.dropbox.sync;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;


public class DropboxSyncWorker extends AbstractWork {

    private static final Log log = LogFactory.getLog(DropboxSyncWorker.class);

    protected String provider;

    public DropboxSyncWorker(String provider, String documentId) {
        this.provider = provider;
        this.docId = documentId;
    }

    @Override
    public void work() {
        setProgress(Progress.PROGRESS_INDETERMINATE);
        setStatus("inProgress");

        if (!TransactionHelper.isTransactionActive()) {
            startTransaction();
        }

        openSystemSession();
        DocumentModel root = session.getDocument(new IdRef(docId));
        DropboxSyncService importer = Framework.getService(DropboxSyncService.class);
        try {
            importer.sync(root,this.provider);
            setStatus("Done");
        } catch (NuxeoException e) {
            log.error(e);
            setStatus("Failed");
        }
        session.save();
    }

    @Override
    public String getTitle() {
        return "DropBoxSync-"+System.currentTimeMillis();
    }

    @Override
    public String getCategory() {
        return "livconnectSync";
    }
}
