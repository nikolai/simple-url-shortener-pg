package com.example.shortener.services;

import com.example.shortener.model.RandomKeyGen;
import com.example.shortener.model.Redirection;
import com.example.shortener.model.RedirectionNotFoundException;
import com.example.shortener.repo.RedirectionRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UrlShortenerService {
    @Autowired
    private RedirectionRepo repo;
    @Autowired
    private RandomKeyGen gen;

    @Value("${shortKeySize}")
    private Integer shortKeySize = 3;

    @Value("${short.url.context}")
    private String shortUrlContext;

    public String shorten(String longUrl) {
        String shortKey = gen.generateKey(shortKeySize);
        Redirection redirection = new Redirection(longUrl, shortKey);
        repo.save(redirection);
        return shortUrlContext + "/" + shortKey;
    }

    public Redirection resolve(String shortKey) throws RedirectionNotFoundException {
        Optional<Redirection> redirection = repo.findById(shortKey);
        if (redirection.isPresent()) {
            return redirection.get();
        }
        throw new RedirectionNotFoundException(shortKey);
    }
}
