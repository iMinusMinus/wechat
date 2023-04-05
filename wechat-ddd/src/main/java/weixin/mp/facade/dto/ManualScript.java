package weixin.mp.facade.dto;

public interface ManualScript {
    String title();
    String author();
    String digest();
    String content();
    String contentSourceUrl();
    String thumbMediaId();
    boolean displayCover();
    boolean commentEnabled();
    boolean onlyFansComment();
}
