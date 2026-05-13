package nl.partycentrum.lux.service;

import nl.partycentrum.lux.domain.Customer;
import nl.partycentrum.lux.dto.customer.CustomerRequest;
import nl.partycentrum.lux.dto.customer.CustomerResponse;
import nl.partycentrum.lux.exception.ApiException;
import nl.partycentrum.lux.repository.CustomerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> findAll(String search) {
        var customers = search == null || search.isBlank()
                ? customerRepository.findAll()
                : customerRepository.findByNaamContainingIgnoreCaseOrEmailContainingIgnoreCase(search, search);
        return customers.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse findById(Long id) {
        return toResponse(getCustomer(id));
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        return toResponse(createEntity(request));
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request) {
        var customer = getCustomer(id);
        apply(customer, request);
        return toResponse(customer);
    }

    @Transactional
    public void delete(Long id) {
        customerRepository.delete(getCustomer(id));
    }

    public Customer getCustomer(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Klant niet gevonden."));
    }

    public Customer createEntity(CustomerRequest request) {
        var customer = new Customer();
        apply(customer, request);
        return customerRepository.save(customer);
    }

    public CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getNaam(),
                customer.getNaam(),
                customer.getEmail(),
                customer.getTelefoon(),
                customer.getTelefoon(),
                customer.getAdres(),
                customer.getAdres(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }

    private void apply(Customer customer, CustomerRequest request) {
        var naam = request.resolvedNaam();
        var telefoon = request.resolvedTelefoon();
        if (naam == null || naam.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Klantnaam is verplicht.");
        }
        if (telefoon == null || telefoon.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Telefoonnummer is verplicht.");
        }
        customer.setNaam(naam.trim());
        customer.setEmail(request.email());
        customer.setTelefoon(telefoon.trim());
        customer.setAdres(request.resolvedAdres());
    }
}
