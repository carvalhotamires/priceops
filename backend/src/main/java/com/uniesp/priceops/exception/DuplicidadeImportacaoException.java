package com.uniesp.priceops.exception;

import com.uniesp.priceops.model.ImportacaoPdf;

public class DuplicidadeImportacaoException extends RuntimeException {

    private final ImportacaoPdf importacaoExistente;

    public DuplicidadeImportacaoException(String mensagem, ImportacaoPdf importacaoExistente) {
        super(mensagem);
        this.importacaoExistente = importacaoExistente;
    }

    public ImportacaoPdf getImportacaoExistente() {
        return importacaoExistente;
    }
}
