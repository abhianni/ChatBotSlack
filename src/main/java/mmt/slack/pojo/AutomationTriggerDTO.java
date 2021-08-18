package mmt.slack.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.slack.api.model.view.View;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class AutomationTriggerDTO {

    private  String component;
    private String branch;
    private String command;
    private String action;
    private String username;
    private String psswrd;
    private View view;
    public String failCount;
    public String skipCount;
    public String totalCount;
    public String source;
    public String serverIp;

}