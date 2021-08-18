package mmt.slack.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DockerResponse {
    public String Id;
    public List<String> Names;
    public String Image;
    public String ImageID;
    public String State;
    public String Status;
}
