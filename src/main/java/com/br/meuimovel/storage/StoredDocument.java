package com.br.meuimovel.storage;

import org.springframework.core.io.Resource;

public record StoredDocument(Resource resource, String contentType) {}
