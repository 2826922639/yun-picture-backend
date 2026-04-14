package com.hhh.yunpicturebackend.ai;

import reactor.core.publisher.Flux;

public interface AiChatWithRAGService {
    Flux<String> answer(String query);
}
