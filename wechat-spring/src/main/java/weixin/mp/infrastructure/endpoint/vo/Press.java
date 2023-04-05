package weixin.mp.infrastructure.endpoint.vo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import weixin.mp.facade.dto.Paper;

public record Press(@NotEmpty String title,
                    @NotEmpty(groups = {Paper.class}) String author,
                    String digest,
                    @NotEmpty @Size(max = 20000) String content,
                    @NotEmpty(groups = {Paper.class}) String contentSourceUrl,
                    @NotEmpty String thumbMediaId,
                    boolean displayCover,
                    Integer commentState,
                    // 保存、发布后才会存在
                    String url,
                    // 发布后才存在
                    Boolean deleted) implements Paper {
    @Override
    public boolean commentEnabled() {
        return commentState != null && commentState != 0;
    }

    @Override
    public boolean onlyFansComment() {
        return commentState != null && commentState < 0;
    }

    public static Press from(Paper paper) {
        return new Press(paper.title(), paper.author(), paper.digest(), paper.content(), paper.contentSourceUrl(),
                paper.thumbMediaId(), paper.displayCover(),
                paper.onlyFansComment() ? -1 : paper.commentEnabled() ? 1 : 0,
                paper.url(), paper.deleted());
    }
}
