/*
 * Copyright 2009-2016 European Molecular Biology Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package uk.ac.ebi.microarray.arrayexpress.shared.auth;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class AuthenticationHelper {

    public String sendAuthenticationRequest(String username, String passwordHash, String endPoint) throws IOException {
        String responseString = null;
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(endPoint);
        httpPost.setHeader("Access-Control-Allow-Credentials","true");
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("login", username));
        nvps.add(new BasicNameValuePair("hash", passwordHash));
        httpPost.setEntity(new UrlEncodedFormEntity(nvps));
        CloseableHttpResponse response = httpclient.execute(httpPost);
        try {
            responseString = EntityUtils.toString(response.getEntity());
        } finally {
            response.close();
        }
        return responseString;
    }
    private String toHexStr(byte[] dgst) {
        if (dgst == null)
            return "";
        StringBuilder sb = new StringBuilder();
        for (byte b : dgst) {
            int hxd = (b >> 4) & 0x0F;
            sb.append((char) (hxd >= 10 ? ('A' + (hxd - 10)) : ('0' + hxd)));
            hxd = b & 0x0F;
            sb.append((char) (hxd >= 10 ? ('A' + (hxd - 10)) : ('0' + hxd)));
        }
        return sb.toString();
    }

    public String generateHash(String password) {
        MessageDigest sha1 = null;
        try {
            sha1 = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return toHexStr(sha1.digest(password.getBytes()));
    }

}
