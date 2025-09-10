package com.chatapp.mapper;

import com.chatapp.entity.ImageFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ImageFileMapper {
    int insert(ImageFile imageFile);
    ImageFile findById(@Param("id") Long id);
    ImageFile findByOriginalName(@Param("name") String name);
}