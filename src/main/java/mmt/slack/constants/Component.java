package mmt.slack.constants;

public enum Component {
    HES("Hotels-entity-service"),
    CG("Hotels-ClientGateway");


    public String getValue() {
        return value;
    }

    String value;
    Component(String value) {
        this.value = value;
    }
}
