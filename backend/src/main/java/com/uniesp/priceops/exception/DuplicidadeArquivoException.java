package com.uniesp.priceops.exception;

import com.uniesp.priceops.model.ArquivoPdf;

public class DuplicidadeArquivoException extends RuntimeException {

    private final ArquivoPdf arquivoExistente;

    public DuplicidadeArquivoException(String mensagem, ArquivoPdf arquivoExistente) {
        super(mensagem);
        this.arquivoExistente = arquivoExistente;
    }

    public ArquivoPdf getArquivoExistente() {
        return arquivoExistente;
    }
}
