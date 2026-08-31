package com.elitebnb_backend.dto;

import com.elitebnb_backend.entity.PropertyImageType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddPropertyImageRequest {

    private String imageUrl;

    private boolean coverImage;

    private PropertyImageType imageType;
}