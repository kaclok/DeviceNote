package com.smlj.singledevice_note.core.o;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class PageParam {
    protected int pageNum;
    protected int pageSize;
}
