package mmt.slack.helper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slack.api.bolt.context.builtin.ActionContext;
import com.slack.api.bolt.context.builtin.EventContext;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostEphemeralRequest;
import com.slack.api.methods.request.files.FilesUploadRequest;
import com.slack.api.methods.response.chat.ChatPostEphemeralResponse;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.methods.response.views.ViewsUpdateResponse;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.event.MessageEvent;
import com.slack.api.model.view.View;
import mmt.slack.constants.Component;
import mmt.slack.pojo.AutomationTriggerDTO;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class Common {

    @Autowired
    Views userView;

    public HttpResponse requestClientContext(String endPoint, String body) {
        HttpResponse response = null;
        try {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            int timeout = 3600; // seconds
            HttpParams httpParams = httpClient.getParams();
            HttpConnectionParams.setConnectionTimeout(
                    httpParams, timeout * 1000); // http.connection.timeout
            HttpConnectionParams.setSoTimeout(
                    httpParams, timeout * 1000); // http.socket.timeout

            HttpPost httpPost = new HttpPost(endPoint);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            StringEntity userEntity = new StringEntity(body);
            httpPost.setEntity(userEntity);
            response = httpClient.execute(httpPost);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    public void postMessage(EventContext ctx, MessageEvent event, String messageUser) {
        ChatPostMessageResponse message = null;
        try {
            message = ctx.client().chatPostMessage(r -> r
                    .channel(event.getChannel())
                    .threadTs(event.getTs())
                    .text("<@" + event.getUser() + "> " + messageUser));

        } catch (IOException e) {
            e.printStackTrace();
        } catch (SlackApiException e) {
            e.printStackTrace();
        }
        if (!message.isOk()) {
            ctx.logger.error("chat.postMessage failed: {}", message.getError());
        }
    }

    public void postMessageForDirect(EventContext ctx, MessageEvent event, String command) {
        ChatPostMessageResponse message = null;
        try {
            List<LayoutBlock> blocks = command.equalsIgnoreCase("test") ? userView.buildRequestForAutomation(event.getUser())
                    : command.equalsIgnoreCase("edge") ? userView.buildRequestForEdge(event.getUser()) :
                    command.equalsIgnoreCase("docker") ? userView.buildRequestForDocker(event.getUser()) : null;
            message = ctx.client().chatPostMessage(r -> r
                    .channel(event.getChannel())
                    .text("InteractiveRequest")
                    .blocks(blocks));

        } catch (IOException e) {
            e.printStackTrace();
        } catch (SlackApiException e) {
            e.printStackTrace();
        }
        if (!message.isOk()) {
            ctx.logger.error("chat.postMessage failed: {}", message.getError());
        }
    }

    public void publishView(View view, MethodsClient client, String userId) {
        try {
            client.viewsPublish(r -> r
                    // The token you used to initialize your app
                    .token(System.getenv("SLACK_BOT_TOKEN"))
                    .view(view)
                    .userId(userId)

            );
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SlackApiException e) {
            e.printStackTrace();
        }
    }

    public String validProject(String component) {
        Component[] projectList = Component.values();
        for (Component project : projectList)
            if (project.name().equalsIgnoreCase(component)) {
                return project.getValue();
            }
        return null;
    }

    public void sendMessage(ChatPostEphemeralRequest request, ActionContext ctx, View currentView) {
        try {
            // Get a response as a Java object
            ChatPostEphemeralResponse response = ctx.client().chatPostEphemeral(request);

            View viewForTheCategory = userView.buildResponseView(request.getBlocks(), request.getUser(), currentView);
            ViewsUpdateResponse viewsUpdateResp = ctx.client().viewsUpdate(r -> r
                    .viewId(currentView.getId())
                    .view(viewForTheCategory)
            );
            System.out.println(viewsUpdateResp);
            publishView(viewForTheCategory, ctx.client(), ctx.getRequestUserId());
            if (null != response.getError())
                System.out.println(response.getError());
        } catch (IOException e) {
            // do something with exception
        } catch (SlackApiException e) {
            // do something with exception
        }
    }

    public void postMessageForHome(ActionContext ctx, String messageUser, AutomationTriggerDTO userRequest
            , LayoutBlock block) {
        ChatPostEphemeralRequest request = null;
        try {
            List<LayoutBlock> message = new ArrayList();
            message.add(SectionBlock
                    .builder()
                    .text(MarkdownTextObject
                            .builder()
                            .text("Hi <@" + ctx.getRequestUserId() + "> " + messageUser)
                            .build())
                    .build());
            message.add(block
            );
            if (userRequest.getSource().equalsIgnoreCase("Direct")) {
                ctx.respond(message);
            } else {
                request = ChatPostEphemeralRequest.builder()
                        .token(ctx.getBotToken())
                        .channel("automation")
                        .user(ctx.getRequestUserId())
                        .blocks(message)
                        .text("Command Response")
                        .build()
                ;
                sendMessage(request, ctx, userRequest.getView());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public String writetoJson(Object body) {
        try {
            ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            String request = mapper.writeValueAsString(body);
            return request;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getResponseAsString(HttpResponse response)
    {
        String content =null;
        try {
            HttpEntity entity = response.getEntity();

            // Read the contents of an entity and return it as a String.
             content = EntityUtils.toString(entity);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return content;
    }


    public Object jsonToObject(String response, Class<?> classType) {
        ObjectMapper mapper = new ObjectMapper();
        Object obj = new Object();
        try {
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    false);
            obj = mapper.readValue(response, classType);
            return obj;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}

