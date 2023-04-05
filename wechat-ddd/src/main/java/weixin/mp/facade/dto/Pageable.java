package weixin.mp.facade.dto;

import java.util.List;

public interface Pageable<E> {

    int total();

    int offset();

    int pageSize();

    List<E> items();

    default String cursorId() {
        return null;
    }
}
