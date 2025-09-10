package com.chatapp.service;

import com.chatapp.entity.ImageFile;

public interface ImageFileService {
    ImageFile save(String contentType, byte[] data, long size, String originalName);
    ImageFile get(Long id);
    ImageFile findByOriginalName(String name);
}