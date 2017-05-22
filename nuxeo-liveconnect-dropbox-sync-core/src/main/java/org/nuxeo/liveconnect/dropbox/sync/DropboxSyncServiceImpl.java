package org.nuxeo.liveconnect.dropbox.sync;

import java.io.IOException;
import java.util.Collection;
import java.util.Locale;

import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.liveconnect.core.LiveConnectFileInfo;
import org.nuxeo.ecm.liveconnect.dropbox.DropboxBlobProvider;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.oauth2.tokens.NuxeoOAuth2Token;
import org.nuxeo.ecm.platform.oauth2.tokens.OAuth2TokenStore;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.dropbox.core.*;
import com.google.api.client.auth.oauth2.StoredCredential;

public class DropboxSyncServiceImpl extends DefaultComponent implements DropboxSyncService {

    private static final String APPLICATION_NAME = "Nuxeo/0";

    /**
     * Component activated notification.
     * Called when the component is activated. All component dependencies are resolved at that moment.
     * Use this method to initialize the component.
     *
     * @param context the component context.
     */
    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
    }

    /**
     * Component deactivated notification.
     * Called before a component is unregistered.
     * Use this method to do cleanup if any and free any resources held by the component.
     *
     * @param context the component context.
     */
    @Override
    public void deactivate(ComponentContext context) {
        super.deactivate(context);
    }

    /**
     * Application started notification.
     * Called after the application started.
     * You can do here any initialization that requires a working application
     * (all resolved bundles and components are active at that moment)
     *
     * @param context the component context. Use it to get the current bundle context
     * @throws Exception
     */
    @Override
    public void applicationStarted(ComponentContext context) {
        // do nothing by default. You can remove this method if not used.
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        // Add some logic here to handle contributions
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        // Logic to do when unregistering any contribution
    }

    @Override
    public void sync(DocumentModel root, String providerName) {
        OAuth2TokenStore store = new OAuth2TokenStore(providerName);
        try {
            Collection<StoredCredential> credentials = store.values();
            for (StoredCredential credential: credentials) {
                NuxeoOAuth2Token token = store.getToken(credential.getAccessToken());
                syncUserFolder(token,root);
            }
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }


    protected void syncUserFolder(NuxeoOAuth2Token token, DocumentModel root) throws IOException {
        DbxRequestConfig config = new DbxRequestConfig(APPLICATION_NAME, Locale.getDefault().toString());
        DbxClient dbxClient = new DbxClient(config, token.getAccessToken());

        String userId = token.getServiceLogin();

        DocumentModel userRoot = getOrCreateUserRoot(root,token.getNuxeoLogin(),userId);
        String deltaKey =(String) userRoot.getPropertyValue("lc:deltakey");

        DbxDelta<DbxEntry> delta;
        try {
            delta = dbxClient.getDelta(deltaKey);
        } catch (DbxException e) {
            throw new NuxeoException(e);
        }

        do {
            for (DbxDelta.Entry entry : delta.entries) {
                if (entry.metadata == null) {
                    System.out.println("Deleted: " + entry.lcPath);
                    if (entry.metadata instanceof DbxEntry.Folder) {
                        continue;
                    }
                    deleteFileDocument(userRoot,userId,entry.lcPath);
                } else {
                    if (entry.metadata instanceof DbxEntry.Folder) {
                        continue;
                    }
                    DbxEntry.File dbxFile = (DbxEntry.File) entry.metadata;
                    getOrCreateFileDocument(userRoot,userId,dbxFile);
                    System.out.println("Added or modified: " + entry.lcPath);
                }
            }
        } while (delta.hasMore);

        userRoot.setPropertyValue("lc:deltakey",delta.cursor);
        userRoot.getCoreSession().saveDocument(userRoot);
        userRoot.getCoreSession().save();
        if (TransactionHelper.isTransactionActive()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }

    protected DocumentModel getOrCreateUserRoot(DocumentModel dropboxRoot, String nuxeoUsername, String serviceid) {
        CoreSession session = dropboxRoot.getCoreSession();
        String query = String.format("Select * From Workspace Where lc:owner = '%s' AND ecm:isCheckedInVersion = 0 AND ecm:isProxy = 0",serviceid);
        DocumentModelList list = session.query(query);
        if (list.size()>0) return list.get(0);

        UserManager userManager = Framework.getService(UserManager.class);
        NuxeoPrincipal principal = userManager.getPrincipal(nuxeoUsername);
        String fullname = ""+principal.getFirstName()+" "+principal.getLastName();

        DocumentModel userFolder =
                session.createDocumentModel(
                        dropboxRoot.getPathAsString(),nuxeoUsername,"Workspace");
        userFolder.setPropertyValue("dc:title",fullname);
        userFolder.addFacet("Liveconnect");
        userFolder.setPropertyValue("lc:owner",serviceid);
        return session.createDocument(userFolder);
    }

    protected DocumentModel getOrCreateFileDocument(DocumentModel folder, String ownerId, DbxEntry.File dbxFile) throws IOException {
        CoreSession session = folder.getCoreSession();
        String query = String.format(
                "Select * From Document Where lc:owner = '%s' AND lc:itemid='%s' AND ecm:isCheckedInVersion = 0 AND ecm:isProxy = 0",ownerId,dbxFile.path);
        DocumentModelList list = session.query(query);
        if (list.size()>0) return list.get(0);

        DropboxBlobProvider blobProvider = (DropboxBlobProvider) Framework.getService(BlobManager.class)
                .getBlobProvider("dropbox");
        Blob blob = blobProvider.toBlob(new LiveConnectFileInfo(ownerId,dbxFile.path));

        FileManager fileManager = Framework.getService(FileManager.class);
        DocumentModel file =
                fileManager.createDocumentFromBlob(folder.getCoreSession(),blob,folder.getPathAsString(),true,blob.getFilename());
        file.addFacet("Liveconnect");
        file.setPropertyValue("lc:owner",ownerId);
        file.setPropertyValue("lc:itemid",dbxFile.path);
        session.saveDocument(file);
        return file;
    }

    protected void deleteFileDocument(DocumentModel folder, String ownerId, String itemId) throws
            IOException {
        CoreSession session = folder.getCoreSession();
        String query = String.format(
                "Select * From Document Where lc:owner = '%s' AND lc:itemid='%s' AND ecm:isCheckedInVersion = 0 AND ecm:isProxy = 0",ownerId,itemId);
        DocumentModelList list = session.query(query);
        for (DocumentModel doc:list) {
            session.followTransition(doc,"to_delete");
        }
    }

}
