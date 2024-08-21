package br.com.siecola.productservice.controllers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductsController {

    private static final Logger LOG = LogManager.getLogger(ProductsController.class);

    @GetMapping
    public String getAllProducts() {
        LOG.info("Get all products");
        return "All products";
    }
}
