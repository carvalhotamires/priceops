package com.uniesp.priceops.controller;

import com.uniesp.priceops.dto.VinculoItemFornecedorRequestDto;
import com.uniesp.priceops.dto.VinculoItemFornecedorResponseDto;
import com.uniesp.priceops.service.VinculoItemFornecedorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/vinculos-fornecedores")
@CrossOrigin(origins = "*") // Permite integração com o React
public class VinculoItemFornecedorController {

    @Autowired
    private VinculoItemFornecedorService vinculoService;

    @PostMapping
    public ResponseEntity<?> criarOuAtualizar(@RequestBody VinculoItemFornecedorRequestDto request) {
        try {
            VinculoItemFornecedorResponseDto salvo = vinculoService.criarOuAtualizar(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<VinculoItemFornecedorResponseDto>> listar(
            @RequestParam(required = false) Long fornecedorId,
            @RequestParam(required = false) Long produtoId,
            @RequestParam(required = false) String codigoFornecedor,
            @RequestParam(required = false) Boolean apenasAtivos) {
        return ResponseEntity.ok(vinculoService.listar(fornecedorId, produtoId, codigoFornecedor, apenasAtivos));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(vinculoService.buscarPorId(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> alternarStatus(
            @PathVariable Long id,
            @RequestParam boolean ativo) {
        try {
            return ResponseEntity.ok(vinculoService.alternarStatus(id, ativo));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletar(@PathVariable Long id) {
        try {
            vinculoService.deletar(id);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
