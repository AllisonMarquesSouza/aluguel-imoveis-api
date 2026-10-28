package com.br.meuimovel.storage;

import org.springframework.core.io.Resource;

public record StoredImage(Resource resource, String contentType) {}
