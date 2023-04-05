package weixin.mp.facade.dto;

import java.time.LocalDateTime;
import java.util.List;

public interface Publication extends Material {

    LocalDateTime updatedAt();

    List<? extends Paper> items();
}
