package com.healthdekho.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the React app's routes when the React build is packaged inside the JAR (classpath:/static).
 * A browser refresh on /dashboard etc. returns index.html and React Router takes over.
 */
@Controller
public class SpaForwardController {

    @GetMapping({"/", "/login", "/dashboard", "/products", "/recommendations", "/register/{productCode}"})
    public String index() {
        return "forward:/index.html";
    }
}
