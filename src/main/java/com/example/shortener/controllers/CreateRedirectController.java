package com.example.shortener.controllers;

import com.example.shortener.messages.CreateRedirectRequest;
import com.example.shortener.messages.CreateRedirectResponse;
import com.example.shortener.services.UrlShortenerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class CreateRedirectController {

    @Autowired
    UrlShortenerService shortener;

    @RequestMapping(value="/create", method = RequestMethod.POST)
    public CreateRedirectResponse createRedirect(@RequestBody CreateRedirectRequest request) {
        return new CreateRedirectResponse(shortener.shorten(request.getLongUrl()));
    }


}
