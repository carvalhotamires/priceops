package com.uniesp.priceops.controller;

import com.uniesp.priceops.model.HistoricoPreco;
import com.uniesp.priceops.service.HistoricoPrecoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/historico-precos")
@CrossOrigin(origins = "*") // Permite integração futura com o frontend React
public class HistoricoPrecoController {

    @Autowired
    private HistoricoPrecoService historicoPrecoService;

    public record RegistroPrecoRequest(Long produtoId, Long fornecedorId, BigDecimal precoRegistrado, String origem) {}

    @GetMapping
    public ResponseEntity<List<HistoricoPreco>> listar(
            @RequestParam(required = false) Long produtoId,
            @RequestParam(required = false) Long fornecedorId) {
        return ResponseEntity.ok(historicoPrecoService.listar(produtoId, fornecedorId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(historicoPrecoService.buscarPorId(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // Apenas POST e GET (sem PUT ou DELETE para nunca sobrescrever o histórico)
    @PostMapping
    public ResponseEntity<?> registrar(@RequestBody RegistroPrecoRequest request) {
        try {
            HistoricoPreco salvo = historicoPrecoService.registrarPreco(
                    request.produtoId(),
                    request.fornecedorId(),
                    request.precoRegistrado(),
                    request.origem(),
                    null
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
