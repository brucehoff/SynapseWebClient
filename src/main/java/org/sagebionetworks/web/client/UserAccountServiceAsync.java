package org.sagebionetworks.web.client;

import org.sagebionetworks.web.shared.users.UserData;
import org.sagebionetworks.web.shared.users.UserRegistration;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface UserAccountServiceAsync {

	void sendPasswordResetEmail(String emailAddress, AsyncCallback<Void> callback);
	
	void sendSetApiPasswordEmail(String emailAddress, AsyncCallback<Void> callback);

	void setPassword(String email, String newPassword, AsyncCallback<Void> callback);

	void initiateSession(String username, String password, boolean explicitlyAcceptsTermsOfUse, AsyncCallback<UserData> callback);

	void getUser(String sessionToken, AsyncCallback<UserData> callback);	

	void createUser(UserRegistration userInfo, AsyncCallback<Void> callback);
	
	void updateUser(String firstName, String lastName, String displayName, AsyncCallback<Void> callback);

	void terminateSession(String sessionToken, AsyncCallback<Void> callback);

	void ssoLogin(String sessionToken, AsyncCallback<Boolean> callback);

	void getPrivateAuthServiceUrl(AsyncCallback<String> callback);

	void getPublicAuthServiceUrl(AsyncCallback<String> callback);
	
	void getSynapseWebUrl(AsyncCallback<String> callback);
	
	void getTermsOfUse(AsyncCallback<String> callback);

}
