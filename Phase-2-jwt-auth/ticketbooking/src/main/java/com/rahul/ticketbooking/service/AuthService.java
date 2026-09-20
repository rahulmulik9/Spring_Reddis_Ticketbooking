package com.rahul.ticketbooking.service;

import com.rahul.ticketbooking.dto.AuthRequest;
import com.rahul.ticketbooking.entity.Person;
import com.rahul.ticketbooking.entity.Role;
import com.rahul.ticketbooking.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final PersonRepository personRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public void register(AuthRequest request) {
        if (personRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }
        Person person = new Person();
        person.setUsername(request.getUsername());
        person.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        person.setRole(Role.USER);
        personRepository.save(person);
    }

    public String login(AuthRequest request) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (AuthenticationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        Person person = personRepository.findByUsername(request.getUsername()).orElseThrow();
        return jwtService.generateToken(person);
    }
}