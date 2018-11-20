package org.antiloop222.kscheck;

public class AvailableServer {
    public String reference;
    public String zone;

    public AvailableServer(String reference, String zone) {
        this.reference = reference;
        this.zone = zone;
    }

    @Override
    public String toString() {
        return "AvailableServer{" +
                "reference='" + reference + '\'' +
                ", zone='" + zone + '\'' +
                '}';
    }
}
