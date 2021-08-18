package mmt.slack.service;

import com.slack.api.bolt.App;
import mmt.slack.pojo.Project;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.Data;

@Service
@Data
public class GlobalVariables {

    @Value("${slack.signing.secret}")
    private String signingSecret;
    @Value("${slack.bot.token}")
    private String slackBot;
    public App app;
    @Value("${automation.trigger.endpoint}")
    private String automationTriggerEndpoint;
    private Project project;
    @Value("${edge.task.trigger.endpoint}")
    private String edgeTaskEndpoint;
    @Value("${app.level.token}")
    private String appLevelTokent;
    @Value("${project.error.message}")
    private String projectErrorMessage;
    @Value("${branch.error.message}")
    private String branchErrorMessage;
    @Value(("${usrnam-psswrd.error.message}"))
    private String usrnameErrorMessage;
    @Value(("${automation.running.error.message}"))
    private String automationRunningMessage;
    @Value(("${automation.success.message}"))
    private String automationSuccessMessage;
    @Value(("${edge.success.message}"))
    private String edgeSuccessMessage;
    @Value(("${slack.message.welcome.message}"))
    private String botWelcomeMessage;
    @Value(("${serverIp.message}"))
    private String serverIpErrorMessage;
    @Value("${docker.task.trigger}")
    private String dockerDetailsEndPoint;

    @Value("${hawkeye.bar.link}")
    private String hawkeyeDataLink;

}
