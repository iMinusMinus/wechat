package weixin.mp.infrastructure.endpoint.vo;

import weixin.mp.facade.dto.Publication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record RevisedDraft(String id, LocalDateTime updatedAt, List<Press> items) implements Publication {
    public static RevisedDraft from(Publication p) {
        return new RevisedDraft(p.id(), p.updatedAt(), p.items().stream().map(Press::from).collect(Collectors.toList()));
    }
}
