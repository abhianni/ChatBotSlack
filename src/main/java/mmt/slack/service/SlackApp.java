package mmt.slack.service;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.context.builtin.EventContext;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.slack.api.methods.response.views.ViewsUpdateResponse;
import com.slack.api.model.event.AppHomeOpenedEvent;
import com.slack.api.model.event.MessageEvent;
import com.slack.api.model.view.View;
import lombok.var;
import mmt.slack.helper.AutomationHelper;
import mmt.slack.helper.Common;
import mmt.slack.helper.Views;
import mmt.slack.pojo.AutomationTriggerDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.*;
import static com.slack.api.model.block.element.BlockElements.staticSelect;
import static com.slack.api.model.view.Views.view;
import static com.slack.api.model.view.Views.viewTitle;

@Configuration
public class SlackApp {


    @Autowired
    GlobalVariables globalVariables;

    @Autowired
    AutomationHelper helper;

    @Autowired
    Common common;

    @Autowired
    Views userView;

    @Bean
    public AppConfig loadAppConfig() {
        AppConfig config = new AppConfig();
        ClassLoader classLoader = SlackApp.class.getClassLoader();
        config.setSigningSecret(globalVariables.getSigningSecret());
        config.setSingleTeamBotToken(globalVariables.getSlackBot());
        try {
            Map<String, String> getModifiableEnvironment = getModifiableEnvironment();
            getModifiableEnvironment.put("SLACK_APP_TOKEN", globalVariables.getAppLevelTokent());
            getModifiableEnvironment.put("SLACK_BOT_TOKEN", globalVariables.getSlackBot());
            getModifiableEnvironment.put("SLACK_SIGNING_SECRET", globalVariables.getSigningSecret());
//            Process process = Runtime.getRuntime().exec(
//                    "cmd /c Token.bat", null, new File("./auth/"));
//
//            StringBuilder output = new StringBuilder();
//
//            BufferedReader reader = new BufferedReader(
//                    new InputStreamReader(process.getInputStream()));
//            String line;
//            while ((line = reader.readLine()) != null) {
//                output.append(line + "\n");
//            }
//
//            int exitVal = process.waitFor();
//            if (exitVal == 0) {
//                System.out.println("Success!");
//                System.out.println(output);
//                System.exit(0);
//            } else {
//                System.out.println("shell execution failed");
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return config;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> getModifiableEnvironment() throws Exception {
        Class<?> pe = Class.forName("java.lang.ProcessEnvironment");
        Method getenv = pe.getDeclaredMethod("getenv", String.class);
        getenv.setAccessible(true);
        Field props = pe.getDeclaredField("theCaseInsensitiveEnvironment");
        props.setAccessible(true);
        return (Map<String, String>) props.get(null);
    }

    @Bean
    public App initSlackApp(AppConfig config) {
        App app = new App(config);
        globalVariables.setApp(app);
        Map<String, String> projectMap = new HashMap<>();
        Map<String, AutomationTriggerDTO> userCommandMap = new HashMap<>();
        String appToken = System.getenv("SLACK_APP_TOKEN");
        try {
            SocketModeApp socketModeApp = new SocketModeApp(appToken, app);

            app.client().viewsPublish(viewsPublishRequestBuilder -> viewsPublishRequestBuilder);

            globalVariables.getApp().event(MessageEvent.class, (payload, ctx) -> {
                MessageEvent event = payload.getEvent();
                EventContext context = helper.processUserInput(event, ctx,userCommandMap);
                return context.ack();
            });
            globalVariables.getApp().event(AppHomeOpenedEvent.class, (req, ctx) -> {
                var logger = ctx.logger;
                var userId = req.getEvent().getUser();
                try {
                    // Call the conversations.create method using the built-in WebClient
                    var modalView = view(v -> v
                            .type("home")
                            .externalId(userId + "_Home")
                            .callbackId("Command-Request")
                            .title(viewTitle(title -> title.type("plain_text").text("QA Bot").emoji(true)))
                            .blocks(asBlocks(
                                    section(s -> s.text(markdownText(mt ->
                                            mt.text("*Welcome , <@" + userId + "> :house:*")))),
                                    section(s -> s.text(markdownText(mt ->
                                            mt.text("Please provide valid command for you helping bot")))),
                                    divider(),
                                    divider(),
                                    section(section -> section
                                            .blockId("command-block")
                                            .text(markdownText("*Pick a Command from the dropdown list*"))
                                            .accessory(staticSelect(staticSelect -> staticSelect
                                                    .actionId("command-selection-action")
                                                    .placeholder(plainText("Supported Command"))
                                                    .options(asOptions(
                                                            option(plainText("Automation Trigger"), "test"),
                                                            option(plainText("Edge Task Trigger"), "edge")
                                                    ))
                                            ))
                                    )
                            ))

                    );
                    common.publishView(modalView, ctx.client(), userId);
                    // Print result
                    logger.info("result: {}");
                    System.out.println(userId);
                } catch (Exception e) {
                    logger.error("error: {}", e.getMessage(), e);
                }
                return ctx.ack();
            });
            globalVariables.getApp().blockAction("command-selection-action", (payload, ctx) -> {
                AutomationTriggerDTO dto = (null == userCommandMap.get(payload.getContext().getRequestUserId())) ?
                        new AutomationTriggerDTO() : userCommandMap.get(payload.getContext().getRequestUserId());
                dto.setCommand(payload.getPayload().getActions().get(0).
                        getSelectedOption().getValue());
                View currentView = payload.getPayload().getView();
                userCommandMap.put(payload.getContext().getRequestUserId(), dto);
                String categoryId = payload.getPayload().getActions().get(0).getSelectedOption().getValue();
                View viewForTheCategory = (categoryId.equalsIgnoreCase("test")) ? userView.buildViewForAutomation(payload.getContext().getRequestUserId()) :
                        categoryId.equalsIgnoreCase("edge") ? userView.buildViewForEdge(payload.getContext().getRequestUserId()) : null;
                ViewsUpdateResponse viewsUpdateResp = ctx.client().viewsUpdate(r -> r
                        .viewId(currentView.getId())
                        .view(viewForTheCategory)
                );
                System.out.println(viewsUpdateResp);
                common.publishView(viewForTheCategory, ctx.client(), payload.getContext().getRequestUserId());
                return ctx.ack();
            });
            globalVariables.getApp().blockAction("project-selection-action", (payload, ctx) -> {
                AutomationTriggerDTO dto = (null == userCommandMap.get(payload.getContext().getRequestUserId())) ?
                        new AutomationTriggerDTO() : userCommandMap.get(payload.getContext().getRequestUserId());
                dto.setComponent(payload.getPayload().getActions().get(0).
                        getSelectedOption().getValue());
                userCommandMap.put(payload.getContext().getRequestUserId(), dto);
                System.out.println(payload.getPayload().getActions()
                        .get(0).getSelectedOption().getValue());
                return ctx.ack();
            });
            globalVariables.getApp().blockAction("branch-action", (payload, ctx) -> {
                AutomationTriggerDTO dto = (null == userCommandMap.get(payload.getContext().getRequestUserId())) ?
                        new AutomationTriggerDTO() : userCommandMap.get(payload.getContext().getRequestUserId());
                dto.setBranch(payload.getPayload().getActions().get(0).
                        getValue());
                userCommandMap.put(payload.getContext().getRequestUserId(), dto);
                System.out.println(payload.getPayload().getActions()
                        .get(0).getValue());
                return ctx.ack();
            });
            globalVariables.getApp().blockAction("userId-action", (payload, ctx) -> {
                AutomationTriggerDTO dto = (null == userCommandMap.get(payload.getContext().getRequestUserId())) ?
                        new AutomationTriggerDTO() : userCommandMap.get(payload.getContext().getRequestUserId());
                dto.setUsername(payload.getPayload().getActions().get(0).
                        getValue());
                userCommandMap.put(payload.getContext().getRequestUserId(), dto);
                System.out.println(payload.getPayload().getActions()
                        .get(0).getValue());
                return ctx.ack();
            });

            globalVariables.getApp().blockAction("psswrd-action", (payload, ctx) -> {
                AutomationTriggerDTO dto = (null == userCommandMap.get(payload.getContext().getRequestUserId())) ?
                        new AutomationTriggerDTO() : userCommandMap.get(payload.getContext().getRequestUserId());
                dto.setPsswrd(payload.getPayload().getActions().get(0).
                        getValue());
                userCommandMap.put(payload.getContext().getRequestUserId(), dto);
                return ctx.ack();
            });
            globalVariables.getApp().blockAction("submit", (payload, ctx) -> {
                String command = userCommandMap.get(payload.getContext().getRequestUserId()).getCommand();
                AutomationTriggerDTO dto = userCommandMap.get(payload.getContext().getRequestUserId());
                dto.setView(payload.getPayload().getView());
                helper.triggerCommandView(ctx, projectMap, userCommandMap.get(payload.getContext().getRequestUserId()),
                        userCommandMap,payload.getContext().getRequestUserId());

                return ctx.ack();
            });
            globalVariables.getApp().blockAction("request-selection-action", (payload, ctx) -> {
                AutomationTriggerDTO dto = (null == userCommandMap.get(payload.getContext().getRequestUserId())) ?
                        new AutomationTriggerDTO() : userCommandMap.get(payload.getContext().getRequestUserId());
                dto.setComponent(payload.getPayload().getActions().get(0).
                        getSelectedOption().getValue());
                userCommandMap.put(payload.getContext().getRequestUserId(), dto);
                System.out.println(payload.getPayload().getActions()
                        .get(0).getSelectedOption().getValue());
                return ctx.ack();
            });

            globalVariables.getApp().blockAction("branch-request-action", (payload, ctx) -> {
                AutomationTriggerDTO dto = (null == userCommandMap.get(payload.getContext().getRequestUserId())) ?
                        new AutomationTriggerDTO() : userCommandMap.get(payload.getContext().getRequestUserId());
                dto.setBranch(payload.getPayload().getActions().get(0).
                        getValue());
                userCommandMap.put(payload.getContext().getRequestUserId(), dto);
                return ctx.ack();
            });
            globalVariables.getApp().blockAction("usrname-request-action", (payload, ctx) -> {
                AutomationTriggerDTO dto = (null == userCommandMap.get(payload.getContext().getRequestUserId())) ?
                        new AutomationTriggerDTO() : userCommandMap.get(payload.getContext().getRequestUserId());
                dto.setUsername(payload.getPayload().getActions().get(0).
                        getValue());
                userCommandMap.put(payload.getContext().getRequestUserId(), dto);
                return ctx.ack();
            });
            globalVariables.getApp().blockAction("psswrd-request-action", (payload, ctx) -> {
                AutomationTriggerDTO dto = (null == userCommandMap.get(payload.getContext().getRequestUserId())) ?
                        new AutomationTriggerDTO() : userCommandMap.get(payload.getContext().getRequestUserId());
                dto.setPsswrd(payload.getPayload().getActions().get(0).
                        getValue());
                userCommandMap.put(payload.getContext().getRequestUserId(), dto);
                return ctx.ack();
            });
            globalVariables.getApp().blockAction("submitRequest", (payload, ctx) -> {
                String command = userCommandMap.get(payload.getContext().getRequestUserId()).getCommand();
                AutomationTriggerDTO dto = userCommandMap.get(payload.getContext().getRequestUserId());
                dto.setView(payload.getPayload().getView());
               // helper.triggerCommandView(ctx, projectMap, userCommandMap.get(payload.getContext().getRequestUserId()), userCommandMap);
                helper.triggerCommandDirectMessage(ctx,projectMap,userCommandMap.get(payload.getContext().getRequestUserId()),
                        userCommandMap,payload.getContext().getRequestUserId());
                return ctx.ack();
            });

            globalVariables.getApp().blockAction("server-request-action", (payload, ctx) -> {
                AutomationTriggerDTO dto = (null == userCommandMap.get(payload.getContext().getRequestUserId())) ?
                        new AutomationTriggerDTO() : userCommandMap.get(payload.getContext().getRequestUserId());
                dto.setServerIp(payload.getPayload().getActions().get(0).
                        getValue());
                userCommandMap.put(payload.getContext().getRequestUserId(), dto);
                return ctx.ack();
            });

            globalVariables.getApp().viewSubmission("Automation", (payload, ctx) -> {
                if (null != payload.getContext().getRequestUserId())
                    System.out.println(payload);
                else
                    System.out.println(payload);

                System.out.println(payload);
                return ctx.ack();
            });

            socketModeApp.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return app;
    }


}

