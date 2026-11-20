package ro.greentween.copaculmeu.data.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "Paged response wrapper")
public class PagedResponse<T> extends PageImpl<T> {

    public PagedResponse() {
        super(new ArrayList<>(), PageRequest.of(0, 20), 0);
    }

    public PagedResponse(List<T> content, Pageable pageable, long total) {
        super(content != null ? content : new ArrayList<>(), pageable != null ? pageable : PageRequest.of(0, 20), total);
    }

    public PagedResponse(List<T> content) {
        super(content != null ? content : new ArrayList<>());
    }

    @Schema(description = "Current page index (alias for number)", example = "0")
    public int getPage() {
        return getNumber();
    }
}
