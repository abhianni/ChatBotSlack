package mmt.slack.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class BuildDetails {
    public String displayName;
    public String url;
    public String failCount;
    public String skipCount;
    public String totalCount;
    public String status;
}
