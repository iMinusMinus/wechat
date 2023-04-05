package weixin.mp.facade.dto;

import java.time.LocalDateTime;

public interface MultiMedia extends Material {

    /**
     *
     * @return 文件名称
     */
    String name();

    /**
     *
     * @return 素材的最后更新时间
     */
    LocalDateTime updatedAt();

    String url();
}
