package weixin.mp.infrastructure.endpoint.vo;

import weixin.mp.facade.dto.Pageable;

import java.util.List;

public record Pagination<E>(int total, int offset, int pageSize, List<E> items) implements Pageable {}
