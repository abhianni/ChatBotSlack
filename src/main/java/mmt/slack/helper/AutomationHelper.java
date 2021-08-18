package mmt.slack.helper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slack.api.bolt.context.builtin.ActionContext;
import com.slack.api.bolt.context.builtin.EventContext;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.event.MessageEvent;
import mmt.slack.constants.CommandSupported;
import mmt.slack.constants.Component;
import mmt.slack.constants.ProjectAutomationStatus;
import mmt.slack.pojo.AutomationTriggerDTO;
import mmt.slack.pojo.BuildDetails;
import mmt.slack.pojo.DockerResponse;
import mmt.slack.service.GlobalVariables;
import org.apache.http.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class AutomationHelper {


    @Autowired
    Common common;


    @Autowired
    GlobalVariables globalVariables;

    @Autowired
    Views userView;

    public String getAutomationUrl(String url) {
        StringBuffer result = new StringBuffer();
        result.append(url);
        Component[] projectList = Component.values();
        for (Component project : projectList) {
            switch (project) {
                case HES:
                    result.append("artifact/web-api-automation/Results/Report/OrchestratorTestReport.html");
                    break;
                case CG:
                    result.append("allure/");
                    break;
            }
            break;
        }
        return result.toString();
    }

    public void triggerAutomation(AutomationTriggerDTO automationRequest, Map<String, String> projectMap, ActionContext ctx) {

        projectMap.put(automationRequest.getComponent(), ProjectAutomationStatus.IN_PROGRESS.name());
        LayoutBlock responseBlock = userView.buildResponseDataAutomation(automationRequest);
        String projectRequested = automationRequest.getComponent();
        automationRequest.setComponent(common.validProject(automationRequest.getComponent()));
        String request = common.writetoJson(automationRequest);
        HttpResponse apiResponse = common.requestClientContext(globalVariables.getAutomationTriggerEndpoint(), request);
        String response = common.getResponseAsString(apiResponse);
        try {
            BuildDetails buildDetails = (BuildDetails) common.jsonToObject(response, BuildDetails.class);
            String automationUrl = getAutomationUrl(buildDetails.getUrl());
            projectMap.put(projectRequested, ProjectAutomationStatus.FREE.name());
            if (buildDetails.getStatus().equalsIgnoreCase("PASS")) {
                automationRequest.setTotalCount(buildDetails.getTotalCount());
                automationRequest.setFailCount(buildDetails.getFailCount());
                automationRequest.setSkipCount(buildDetails.getSkipCount());
            }
            common.postMessageForHome(ctx, ":wave: Please find automation results in url- " + automationUrl,
                    automationRequest, responseBlock);
            HttpResponse imageResponse = common.requestClientContext(globalVariables.getHawkeyeDataLink(), request);
            String imageLink = common.getResponseAsString(imageResponse);
            LayoutBlock imageBlock = userView.imageBlock(imageLink);
            common.postMessageForHome(ctx,"Please find last 5 execution results",automationRequest,imageBlock);
        } catch (Exception e) {
            e.printStackTrace();
            projectMap.put(projectRequested, ProjectAutomationStatus.FREE.name());
        }
    }

    public void triggerEdgeTask(AutomationTriggerDTO automationRequest, ActionContext ctxAction) {
        automationRequest.setComponent(common.validProject(automationRequest.getComponent()));
        LayoutBlock Block = userView.buildResponseDataEdge(automationRequest);
        automationRequest.setAction("restart");
        String request = common.writetoJson(automationRequest);
        HttpResponse apiResponse = common.requestClientContext(globalVariables.getEdgeTaskEndpoint(), request);
        String response = common.getResponseAsString(apiResponse);
        common.postMessageForHome(ctxAction, response, automationRequest, Block);

    }

    public void getDockerDetails(AutomationTriggerDTO automationRequest, ActionContext ctxAction) {
        LayoutBlock errorBlock = userView.buildResponseDataEdge(automationRequest);
        try {
            String request = common.writetoJson(automationRequest);
            HttpResponse apiResponse = common.requestClientContext(globalVariables.getDockerDetailsEndPoint(), request);
            String response = common.getResponseAsString(apiResponse);
            ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            List<DockerResponse> dockerServiceResponse = null;
            dockerServiceResponse = mapper.reader()
                    .forType(new TypeReference<List<DockerResponse>>() {
                    })
                    .readValue(response);
            LayoutBlock block = userView.buildResponseDocker(automationRequest, dockerServiceResponse);
            common.postMessageForHome(ctxAction, "Images running on server " + automationRequest.getServerIp(), automationRequest, block);
        } catch (Exception e) {
            e.printStackTrace();
            common.postMessageForHome(ctxAction,"Please check serverIp entered",automationRequest,errorBlock);
        }
    }


    public EventContext processUserInput(MessageEvent event, EventContext ctx, Map<String, AutomationTriggerDTO> userCommandMap) {
        if (!(event.getText().equalsIgnoreCase("test") || event.getText().equalsIgnoreCase("edge")
                || event.getText().equalsIgnoreCase("docker"))) {
            common.postMessage(ctx, event, globalVariables.getBotWelcomeMessage());
            if (null != userCommandMap.get(ctx.getRequestUserId()))
                userCommandMap.remove(ctx.getRequestUserId());
            return ctx;
        }
        AutomationTriggerDTO dto = (null == userCommandMap.get(event.getUser())) ?
                new AutomationTriggerDTO() : userCommandMap.get(event.getUser());
        userCommandMap.put(event.getUser(), dto);
        dto.setCommand(event.getText());
        userCommandMap.put(ctx.getRequestUserId(), dto);
        common.postMessageForDirect(ctx, event, event.getText());
        return ctx;
    }

    public ActionContext triggerCommandDirectMessage(ActionContext ctx, Map<String, String> projectMap,
                                                     AutomationTriggerDTO request,
                                                     Map<String, AutomationTriggerDTO> userCommandMap, String userId) {
        try {
            LayoutBlock errorBlock = userView.buildResponseDataEdge(request);
            if (request.getCommand().equalsIgnoreCase("docker")
                    && null == request.getServerIp()) {
                common.postMessageForHome(ctx, globalVariables.getServerIpErrorMessage(), request, errorBlock);
                return ctx;
            } else if (request.getCommand().equalsIgnoreCase("test") || request.getCommand().
                    equalsIgnoreCase("edge")) {
                if ((null == request.getComponent() || null == request.getCommand())) {
                    ctx.respond("Hi <@" + userId + "> " + globalVariables.getProjectErrorMessage());
                    return ctx;
                }
                if (null != request.getComponent() && request.getCommand().equalsIgnoreCase("test")
                        && null == request.getBranch()) {
                    ctx.respond("Hi <@" + userId + "> " + globalVariables.getBranchErrorMessage());
                    return ctx;
                }
                if (null != request.getComponent() && request.getCommand().equalsIgnoreCase("edge")
                        && (null == request.getUsername() || null == request.getPsswrd())) {
                    common.postMessageForHome(ctx, globalVariables.getUsrnameErrorMessage(), request, errorBlock);
                    return ctx;
                }
            }
            if (null != common.validProject(request.getComponent())
                    || request.getCommand().equalsIgnoreCase("docker")) {
                if (null == projectMap.get(request.getComponent()))
                    projectMap.put(request.getComponent(), ProjectAutomationStatus.VALID.name());
                request.setSource("Direct");
                switch (request.getCommand().toLowerCase()) {
                    case ("test"):
                        if (projectMap.get(request.getComponent()).equals(ProjectAutomationStatus.IN_PROGRESS.name()))
                            ctx.respond("Hi <@" + userId + "> " + globalVariables.getAutomationRunningMessage() + "- " + request.getComponent());
                        else {
                            ctx.respond("Hi <@" + userId + "> " + globalVariables.getAutomationSuccessMessage());
                            triggerAutomation(request, projectMap, ctx);
                            userCommandMap.remove(userId);
                        }
                        return ctx;
                    case "edge":
                        request.setAction("restart");
                        ctx.respond("Hi <@" + userId + "> " + globalVariables.getEdgeSuccessMessage());
                        triggerEdgeTask(request, ctx);
                        userCommandMap.remove(userId);
                        return ctx;
                    case "docker":
                        getDockerDetails(request, ctx);
                        userCommandMap.remove(userId);
                        return ctx;
                    default:
                        ctx.respond("Hi <@" + userId + "> " + " Invaid command, Bot command format -" + errorMessage());
                }

            } else
                ctx.respond("Hi <@" + userId + "> " + " Entered project " + request.getComponent() + " is not a valid project");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ctx;
    }

    public ActionContext triggerCommandView(ActionContext ctx, Map<String, String> projectMap, AutomationTriggerDTO request
            , Map<String, AutomationTriggerDTO> userCommandMap, String userId) {
        request.setSource("View");
        LayoutBlock responseBlock = userView.buildResponseDataAutomation(request);
        LayoutBlock errorBlock = userView.buildResponseDataEdge(request);
        if (null == request.getComponent() || null == request.getCommand()) {
            common.postMessageForHome(ctx, globalVariables.getProjectErrorMessage(), request, errorBlock);
            return ctx;
        }
        if (null != request.getComponent() && request.getCommand().equalsIgnoreCase("test") && null == request.getBranch()) {
            common.postMessageForHome(ctx, globalVariables.getBranchErrorMessage(), request, errorBlock);
            return ctx;
        }
        if (null != request.getComponent() && request.getCommand().equalsIgnoreCase("edge")
                && (null == request.getUsername() || null == request.getPsswrd())) {
            common.postMessageForHome(ctx, globalVariables.getUsrnameErrorMessage(), request, errorBlock);
            return ctx;
        }
        if (null != common.validProject(request.getComponent())) {
            if (null == projectMap.get(request.getComponent()))
                projectMap.put(request.getComponent(), ProjectAutomationStatus.VALID.name());
            switch (request.getCommand()) {
                case ("test"):
                    if (projectMap.get(request.getComponent()).equals(ProjectAutomationStatus.IN_PROGRESS.name()))
                        common.postMessageForHome(ctx, globalVariables.getAutomationRunningMessage() +
                                "- " + request.getComponent(), request, errorBlock);
                    else {

                        common.postMessageForHome(ctx, globalVariables.getAutomationSuccessMessage(), request, errorBlock);
                        triggerAutomation(request, projectMap, ctx);
                    }
                    userCommandMap.remove(userId);
                    return ctx;
                case "edge":
                    request.setAction("restart");
                    common.postMessageForHome(ctx, globalVariables.getEdgeSuccessMessage(), request, errorBlock);
                    triggerEdgeTask(request, ctx);
                    userCommandMap.remove(userId);
                    return ctx;
                default:
                    common.postMessageForHome(ctx, "Command not Accepted -" + errorMessage(), request, errorBlock);
            }

        } else
            common.postMessageForHome(ctx, "Entered project " + request.getComponent() + " is not a valid project",
                    request, errorBlock);

        return ctx;
    }

    public String errorMessage() {
        StringBuffer message = new StringBuffer();
        CommandSupported[] commandSupporteds = CommandSupported.values();
        for (CommandSupported command : commandSupporteds) {
            message.append(System.lineSeparator());
            message.append(command.getValue());
        }
        return message.toString();
    }


}
