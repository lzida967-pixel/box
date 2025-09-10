package com.chatapp.service.impl;

import com.chatapp.entity.ImageFile;
import com.chatapp.mapper.ImageFileMapper;
import com.chatapp.service.ImageFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ImageFileServiceImpl implements ImageFileService {

    @Autowired
    private ImageFileMapper imageFileMapper;

    @Override
    public ImageFile save(String contentType, byte[] data, long size, String originalName) {
        ImageFile img = new ImageFile();
        img.setContentType(contentType);
        img.setData(data);
        img.setSize(size);
        img.setOriginalName(originalName);
        imageFileMapper.insert(img);
        return img;
    }

    @Override
    public ImageFile get(Long id) {
        return imageFileMapper.findById(id);
    }

    @Override
    public ImageFile findByOriginalName(String name) {
        return imageFileMapper.findByOriginalName(name);
    }
}