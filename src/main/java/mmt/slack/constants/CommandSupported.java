package mmt.slack.constants;

public enum CommandSupported {
    AUTOMTION("test <projectname> <branch>"),
    EDGE("edge <projectname> <usrname> <psswrd>");

    public String getValue() {
        return value;
    }

    String value;
    CommandSupported(String value) {
        this.value = value;
    }
}
