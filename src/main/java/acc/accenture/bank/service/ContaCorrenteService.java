package acc.accenture.bank.service;

import acc.accenture.bank.dtos.AgenciaDTO;
import acc.accenture.bank.dtos.ContaCorrenteDTO;
import acc.accenture.bank.dtos.ExtratoDTO;
import acc.accenture.bank.exception.CampoObrigatorioException;
import acc.accenture.bank.exception.EntidadeNaoEncontradaException;
import acc.accenture.bank.exception.OperacaoDesconhecidaException;
import acc.accenture.bank.exception.SaldoInsuficienteException;
import acc.accenture.bank.mapper.ContaCorrenteMapper;
import acc.accenture.bank.mapper.ExtratoMapper;
import acc.accenture.bank.model.ContaCorrente;
import acc.accenture.bank.model.Extrato;
import acc.accenture.bank.repository.ContaCorrenteRepository;
import acc.accenture.bank.repository.ExtratoRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ContaCorrenteService {

    @Autowired
    private ContaCorrenteRepository contaCorrenteRepository;

    @Autowired
    private ContaCorrenteMapper contaCorrenteMapper;

    @Autowired
    private ExtratoRepository extratoRepository;

    @Autowired
    private ExtratoMapper extratoMapper;

    public List<ContaCorrenteDTO> findAll() {
        return contaCorrenteRepository.findAll().stream()
                .map(contaCorrenteMapper::toDTO)
                .collect(Collectors.toList());
    }

    public ContaCorrenteDTO findById(Long id) {
        return contaCorrenteRepository.findById(id)
                .map(contaCorrenteMapper::toDTO)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Conta Corrente"));
    }

    public ContaCorrenteDTO save(ContaCorrenteDTO contaCorrenteDTO) {
        validarCamposObrigatorios(contaCorrenteDTO);
        ContaCorrente contaCorrente = contaCorrenteMapper.toEntity(contaCorrenteDTO);
        return contaCorrenteMapper.toDTO(contaCorrenteRepository.save(contaCorrente));
    }

    public void deleteById(Long id) {
        if (!contaCorrenteRepository.existsById(id)) {
            throw new EntidadeNaoEncontradaException("Conta Corrente");
        }
        contaCorrenteRepository.deleteById(id);
    }

    public ContaCorrenteDTO update(Long id, ContaCorrenteDTO contaCorrenteDTO) {
        if (!contaCorrenteRepository.existsById(id)) {
            throw new EntidadeNaoEncontradaException("Conta Corrente");
        }
        validarCamposObrigatorios(contaCorrenteDTO);
        ContaCorrente contaCorrente = contaCorrenteMapper.toEntity(contaCorrenteDTO);
        contaCorrente.setId(id);
        return contaCorrenteMapper.toDTO(contaCorrenteRepository.save(contaCorrente));
    }

    @Transactional
    public void depositar(Long id, BigDecimal valor) {
        ContaCorrente contaCorrente = contaCorrenteRepository.findById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Conta Corrente"));
        contaCorrente.setSaldo(contaCorrente.getSaldo().add(valor));
        contaCorrenteRepository.save(contaCorrente);
    }

    @Transactional
    public void sacar(Long id, BigDecimal valor) {
        ContaCorrente contaCorrente = contaCorrenteRepository.findById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Conta Corrente"));
        if (contaCorrente.getSaldo().compareTo(valor) < 0) {
            throw new SaldoInsuficienteException();
        }
        contaCorrente.setSaldo(contaCorrente.getSaldo().subtract(valor));
        contaCorrenteRepository.save(contaCorrente);
    }

    @Transactional
    public void transferir(Long idOrigem, Long idDestino, BigDecimal valor) {
        ContaCorrente contaOrigem = contaCorrenteRepository.findById(idOrigem)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Conta Corrente de origem"));

        ContaCorrente contaDestino = contaCorrenteRepository.findById(idDestino)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Conta Corrente de destino"));

        if (contaOrigem.getSaldo().compareTo(valor) < 0) {
            throw new SaldoInsuficienteException();
        }

        contaOrigem.setSaldo(contaOrigem.getSaldo().subtract(valor));
        contaDestino.setSaldo(contaDestino.getSaldo().add(valor));

        contaCorrenteRepository.save(contaOrigem);
        contaCorrenteRepository.save(contaDestino);
    }

    public BigDecimal recalcularSaldo(Long id) {
        ContaCorrente contaCorrente = contaCorrenteRepository.findById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Conta Corrente"));

        List<Extrato> extratos = extratoRepository.findByContaCorrenteId(id);
        BigDecimal saldoRecalculado = BigDecimal.ZERO;

        // Iterar sobre as transações e ajustar o saldo
        for (Extrato extrato : extratos) {
            switch (extrato.getOperacao()) {
                case DEPOSITO:
                    saldoRecalculado = saldoRecalculado.add(extrato.getValor());
                    break;
                case SAQUE:
                case TRANSFERENCIA:
                    saldoRecalculado = saldoRecalculado.subtract(extrato.getValor());
                    break;
                default:
                    //throw new RuntimeException("Operação desconhecida: " + extrato.getOperacao());
                    throw new OperacaoDesconhecidaException("Operação desconhecida: " + extrato.getOperacao());
            }
        }
        contaCorrente.setSaldo(saldoRecalculado);
        contaCorrenteRepository.save(contaCorrente);

        return saldoRecalculado;
    }

    public List<ExtratoDTO> exibirExtrato(Long contaCorrenteId) {
        ContaCorrente contaCorrente = contaCorrenteRepository.findById(contaCorrenteId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Conta Corrente"));

        // Recuperar todas as entradas de extrato para a conta corrente especificada
        List<Extrato> extratos = extratoRepository.findByContaCorrenteId(contaCorrenteId);

        // Mapear as entradas do extrato para DTOs
        return extratos.stream()
                .map(extratoMapper::toDTO)
                .collect(Collectors.toList());
    }

    private void validarCamposObrigatorios(ContaCorrenteDTO contaCorrenteDTO) {
        if (contaCorrenteDTO.getNumero() == null || contaCorrenteDTO.getNumero().trim().isEmpty()) {
            throw new CampoObrigatorioException("Numero da Conta Corrente");
        }
    }

}
