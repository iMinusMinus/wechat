package weixin.mp.domain;

public interface AbstractMessage {

    String from();

    String to();

    long timestamp();
}
