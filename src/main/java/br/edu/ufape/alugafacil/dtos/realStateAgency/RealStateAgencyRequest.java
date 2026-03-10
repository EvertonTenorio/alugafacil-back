package br.edu.ufape.alugafacil.dtos.realStateAgency;

import br.edu.ufape.alugafacil.dtos.address.AddressRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RealStateAgencyRequest (
    @NotBlank(message = "O nome fantasia da imobiliária é obrigatório") 
    String name,
    
    @NotBlank(message = "A Razão Social é obrigatória") 
    String corporateName,
    
    @NotBlank(message = "O CNPJ é obrigatório") 
    String cnpj,
    
    @NotBlank(message = "O e-mail de contato da empresa é obrigatório")
    @Email(message = "E-mail da empresa inválido")
    String email,
    
    @NotBlank(message = "O telefone da empresa é obrigatório")
    String phoneNumber,

    String website,
    String photoUrl,
    
    @NotBlank(message = "O nome do administrador responsável é obrigatório") 
    String adminName,
    
    @NotBlank(message = "O e-mail de login do administrador é obrigatório") 
    @Email(message = "E-mail de login inválido")
    String adminEmail,
    
    @NotBlank(message = "A senha é obrigatória")
    @Size(min = 4, message = "A senha deve ter no mínimo 6 caracteres")
    String adminPassword,
    
    @NotNull(message = "O endereço é obrigatório")
    @Valid 
    AddressRequest address
) {}

