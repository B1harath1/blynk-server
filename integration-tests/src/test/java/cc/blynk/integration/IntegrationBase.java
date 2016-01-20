package cc.blynk.integration;

import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.integration.model.tcp.TestAppClient;
import cc.blynk.integration.model.tcp.TestHardClient;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.GetTokenMessage;
import cc.blynk.utils.JsonParser;
import cc.blynk.utils.ServerProperties;
import org.mockito.ArgumentCaptor;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;

import static cc.blynk.server.core.protocol.enums.Response.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/4/2015.
 */
public abstract class IntegrationBase extends BaseTest {

    public static String readTestUserProfile(String fileName) {
        if (fileName == null) {
            fileName = "user_profile_json.txt";
        }
        InputStream is = IntegrationBase.class.getResourceAsStream("/json_test/" + fileName);
        Profile profile = JsonParser.parseProfile(is);
        return profile.toString();
    }

    public static String readTestUserProfile() {
        return readTestUserProfile(null);
    }

    private static GetTokenMessage getGetTokenMessage(List<Object> arguments) {
        for (Object obj : arguments) {
            if (obj instanceof GetTokenMessage) {
                return (GetTokenMessage) obj;
            }
        }
        throw new RuntimeException("Get token message wasn't retrieved.");
    }

    public static String getProfileFolder() {
        URL resource = IntegrationBase.class.getResource("/profiles");
        URI uri = null;
        try {
            uri = resource.toURI();
        } catch (Exception e) {
        }
        String resourcesPath = Paths.get(uri).toAbsolutePath().toString();
        System.out.println("Resource path : " + resourcesPath);
        return resourcesPath;
    }

    public static ClientPair initAppAndHardPair(String host, int appPort, int hardPort, String user, String jsonProfile,
                                  ServerProperties properties) throws Exception {

        TestAppClient appClient = new TestAppClient(host, appPort, properties);
        TestHardClient hardClient = new TestHardClient(host, hardPort);

        return initAppAndHardPair(appClient, hardClient, user, jsonProfile);
    }

    public static ClientPair initAppAndHardPair(TestAppClient appClient, TestHardClient hardClient, String user,
                                  String jsonProfile) throws Exception {

        appClient.start();
        hardClient.start();

        String userProfileString = readTestUserProfile(jsonProfile);
        int dashId = JsonParser.parseProfile(userProfileString, 1).dashBoards[0].id;

        appClient.send("register " + user)
                 .send("login " + user + " Android 1RC7")
                 .send("saveProfile " + userProfileString)
                 .send("activate " + dashId)
                 .send("getToken " + dashId);

        ArgumentCaptor<Object> objectArgumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(appClient.responseMock, timeout(2000).times(5)).channelRead(any(), objectArgumentCaptor.capture());

        List<Object> arguments = objectArgumentCaptor.getAllValues();
        String token = getGetTokenMessage(arguments).body;

        hardClient.send("login " + token);
        verify(hardClient.responseMock, timeout(2000)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        appClient.reset();
        hardClient.reset();

        return new ClientPair(appClient, hardClient, token);
    }

    public static ClientPair initAppAndHardPair(int tcpAppPort, int tcpHartPort, ServerProperties properties) throws Exception {
        return initAppAndHardPair("localhost", tcpAppPort, tcpHartPort, "dima@mail.ua 1", null, properties);
    }

    public ClientPair initAppAndHardPair() throws Exception {
        return initAppAndHardPair("localhost", tcpAppPort, tcpHardPort, "dima@mail.ua 1", null, properties);
    }

    public ClientPair initAppAndHardPair(String jsonProfile) throws Exception {
        return initAppAndHardPair("localhost", tcpAppPort, tcpHardPort, "dima@mail.ua 1", jsonProfile, properties);
    }

}
