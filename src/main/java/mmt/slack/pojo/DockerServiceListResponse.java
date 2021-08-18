package mmt.slack.pojo;

import lombok.Data;

import java.util.List;

@Data
public class DockerServiceListResponse {
    public List<DockerResponse> dockerResponse;

}
