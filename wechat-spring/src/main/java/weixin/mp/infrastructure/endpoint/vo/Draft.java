package weixin.mp.infrastructure.endpoint.vo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import weixin.mp.facade.dto.ManualScript;

public record Draft(@NotEmpty String title,
                    @NotEmpty(groups = {ManualScript.class}) String author,
                    String digest,
                    @NotEmpty @Size(max = 20000) String content,
                    @NotEmpty(groups = {ManualScript.class}) String contentSourceUrl,
                    @NotEmpty String thumbMediaId,
                    Integer commentState,
                    // 素材接口特有
                    boolean displayCover) implements ManualScript {
    @Override
    public boolean commentEnabled() {
        return commentState != null && commentState != 0;
    }

    @Override
    public boolean onlyFansComment() {
        return commentState != null && commentState < 0;
    }
}
