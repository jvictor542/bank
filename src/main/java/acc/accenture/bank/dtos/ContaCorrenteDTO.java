package acc.accenture.bank.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContaCorrenteDTO {

    // ID da conta corrente
    private Long id;
    private String numero; // Número da conta corrente
    private BigDecimal saldo; // Saldo da conta corrente
    private Long agenciaId; // ID da agência
    private Long clienteId;// ID do cliente
}
