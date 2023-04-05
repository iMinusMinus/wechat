package weixin.mp.facade.dto;

public interface Paper extends ManualScript {
    default String coverUrl() {
        return null;
    }
    String url();
    default Boolean deleted() {
        return null;
    }
}
