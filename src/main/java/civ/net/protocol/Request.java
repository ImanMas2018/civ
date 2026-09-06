package civ.net.protocol;

public abstract class Request extends Message {

    protected Request(String type) {
        super(type);
    }
}
