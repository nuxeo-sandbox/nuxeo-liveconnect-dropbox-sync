package org.nuxeo.liveconnect.dropbox.sync;

import org.nuxeo.ecm.platform.oauth2.tokens.NuxeoOAuth2Token;
import org.nuxeo.ecm.platform.oauth2.tokens.OAuth2TokenStore;

public class AuthenticationHelper {

    public static NuxeoOAuth2Token createToken(String username, String providerName) {
        OAuth2TokenStore store = new OAuth2TokenStore(providerName);
        NuxeoOAuth2Token token = new NuxeoOAuth2Token(
                System.getProperty("nuxeo-liveconnect-dropbox-sync-accessToken"),
                null,
                System.currentTimeMillis()+300000);
        token.setNuxeoLogin(username);
        token.setServiceLogin("devnull@nuxeo.com");
        token.setClientId(username);
        store.store(username,token);
        return token;
    }

}
