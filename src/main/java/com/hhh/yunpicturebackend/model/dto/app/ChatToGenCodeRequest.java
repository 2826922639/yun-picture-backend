package com.hhh.yunpicturebackend.model.dto.app;

import lombok.Data;
import java.util.List;

@Data
public class ChatToGenCodeRequest {
    private Long appId;
    private String message;
    private List<String> imageUrls;
}